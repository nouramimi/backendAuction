
package com.WebProject.payment_service.service;
import org.springframework.http.HttpStatusCode;
import com.WebProject.payment_service.dto.*;
import com.WebProject.payment_service.exception.PaymentException;
import com.WebProject.payment_service.exception.PaymentNotFoundException;
import com.WebProject.payment_service.model.Payment;
import com.WebProject.payment_service.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository paymentRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    public Mono<PaymentResponse> initiatePayment(PaymentRequest request) {
        return Mono.fromCallable(() -> {
            Stripe.apiKey = stripeSecretKey;
            Map<String, Object> params = new HashMap<>();
            params.put("amount", request.getAmount().multiply(new BigDecimal("100")).intValue());
            params.put("currency", "usd");
            params.put("payment_method_types", List.of("card"));

            try {
                return PaymentIntent.create(params);
            } catch (StripeException e) {
                throw new PaymentException("Error creating payment: " + e.getMessage());
            }
        }).flatMap(paymentIntent -> {
            Payment payment = Payment.builder()
                    .amount(request.getAmount())
                    .userId(request.getUserId())
                    .auctionId(request.getAuctionId())
                    .status(PaymentStatus.PENDING)
                    .stripePaymentIntentId(paymentIntent.getId())
                    .createdAt(LocalDateTime.now())
                    .build();

            return paymentRepository.save(payment)
                    .flatMap(savedPayment -> {
                        NotificationRequest notificationRequest = NotificationRequest.builder()
                                .userId(request.getUserId())
                                .type(NotificationType.PAYMENT_INITIATED)
                                .message(String.format("Payment of $%.2f initiated for auction %s",
                                        request.getAmount(), request.getAuctionId()))
                                .relatedEntityId(savedPayment.getId())
                                .relatedEntityType("PAYMENT")
                                .build();

                        return sendNotification(notificationRequest)
                                .thenReturn(PaymentResponse.builder()
                                        .id(savedPayment.getId())
                                        .clientSecret(paymentIntent.getClientSecret())
                                        .status(savedPayment.getStatus())
                                        .amount(savedPayment.getAmount())
                                        .build());
                    });
        });
    }

    public Mono<PaymentResponse> confirmPayment(String paymentId) {
        return paymentRepository.findById(paymentId)
                .switchIfEmpty(Mono.error(new PaymentNotFoundException("Payment not found with id: " + paymentId)))
                .flatMap(payment -> {
                    try {
                        Stripe.apiKey = stripeSecretKey;
                        PaymentIntent paymentIntent = PaymentIntent.retrieve(payment.getStripePaymentIntentId());

                        // Log the actual Stripe status for debugging
                        log.info("Stripe payment status: {}", paymentIntent.getStatus());

                        if ("succeeded".equals(paymentIntent.getStatus()) ||
                                "processing".equals(paymentIntent.getStatus())) {
                            payment.setStatus(PaymentStatus.COMPLETED);
                            payment.setUpdatedAt(LocalDateTime.now());

                            return paymentRepository.save(payment)
                                    .flatMap(savedPayment -> {
                                        NotificationRequest buyerNotification = NotificationRequest.builder()
                                                .userId(payment.getUserId())
                                                .type(NotificationType.PAYMENT_COMPLETED)
                                                .message(String.format("Payment of $%.2f completed for auction %s",
                                                        payment.getAmount(), payment.getAuctionId()))
                                                .relatedEntityId(payment.getId())
                                                .relatedEntityType("PAYMENT")
                                                .build();

                                        return sendNotification(buyerNotification)
                                                .then(getAuctionDetails(payment.getAuctionId()))
                                                .flatMap(auction -> {
                                                    if (auction == null || auction.get("sellerId") == null) {
                                                        log.error("Invalid auction details received for auctionId: {}", payment.getAuctionId());
                                                        return Mono.just(PaymentResponse.builder()
                                                                .id(payment.getId())
                                                                .status(payment.getStatus())
                                                                .amount(payment.getAmount())
                                                                .build());
                                                    }

                                                    String sellerId = (String) auction.get("sellerId");
                                                    NotificationRequest sellerNotification = NotificationRequest.builder()
                                                            .userId(sellerId)
                                                            .type(NotificationType.PAYMENT_RECEIVED)
                                                            .message(String.format("Payment of $%.2f received for your auction %s",
                                                                    payment.getAmount(), payment.getAuctionId()))
                                                            .relatedEntityId(payment.getId())
                                                            .relatedEntityType("PAYMENT")
                                                            .build();

                                                    return sendNotification(sellerNotification)
                                                            .thenReturn(PaymentResponse.builder()
                                                                    .id(payment.getId())
                                                                    .status(payment.getStatus())
                                                                    .amount(payment.getAmount())
                                                                    .build());
                                                });
                                    });
                        } else {
                            log.error("Payment not successful. Stripe status: {}", paymentIntent.getStatus());
                            return Mono.error(new PaymentException("Payment not successful. Status: " + paymentIntent.getStatus()));
                        }
                    } catch (StripeException e) {
                        log.error("Stripe error during payment confirmation: {}", e.getMessage());
                        return Mono.error(new PaymentException("Error confirming payment with Stripe: " + e.getMessage()));
                    }
                })
                .doOnError(e -> log.error("Error in payment confirmation flow: {}", e.getMessage()));
    }

    private Mono<Void> sendNotification(NotificationRequest request) {
        return webClientBuilder.build()
                .post()
                .uri("http://notification-service/api/notifications")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("Client error when sending notification: {}", body);
                                    return Mono.empty();
                                })
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("Server error when sending notification: {}", body);
                                    return Mono.empty();
                                })
                )
                .bodyToMono(Void.class)
                .onErrorResume(WebClientRequestException.class, e -> {
                    log.error("Network error when sending notification: {}", e.getMessage());
                    return Mono.empty();
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Unexpected error when sending notification: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Map> getAuctionDetails(String auctionId) {
        return webClientBuilder.build()
                .get()
                .uri("http://auction-service/api/auctions/" + auctionId)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> {
                    log.error("Error fetching auction details: {}", e.getMessage());
                    return Mono.empty();
                })
                .doOnNext(response -> {
                    if (response == null || !response.containsKey("sellerId")) {
                        log.warn("Received invalid auction details format for auctionId: {}", auctionId);
                    }
                });
    }
}