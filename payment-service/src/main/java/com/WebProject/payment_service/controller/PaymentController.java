package com.WebProject.payment_service.controller;

import com.WebProject.payment_service.dto.PaymentRequest;
import com.WebProject.payment_service.dto.PaymentResponse;
import com.WebProject.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public Mono<PaymentResponse> initiatePayment(@RequestBody PaymentRequest request) {
        return paymentService.initiatePayment(request);
    }

    @PostMapping("/{paymentId}/confirm")
    public Mono<PaymentResponse> confirmPayment(@PathVariable String paymentId) {
        return paymentService.confirmPayment(paymentId);
    }
}