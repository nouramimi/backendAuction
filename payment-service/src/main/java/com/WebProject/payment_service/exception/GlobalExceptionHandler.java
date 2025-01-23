package com.WebProject.payment_service.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ErrorResponse {
        private int status;
        private String message;
    }

    @ExceptionHandler(PaymentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponse> handlePaymentException(PaymentException ex) {
        return Mono.just(new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage()
        ));
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ErrorResponse> handlePaymentNotFoundException(PaymentNotFoundException ex) {
        return Mono.just(new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage()
        ));
    }

    @ExceptionHandler(WebClientResponseException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Mono<ErrorResponse> handleWebClientResponseException(WebClientResponseException ex) {
        return Mono.just(new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service communication error: " + ex.getMessage()
        ));
    }
}