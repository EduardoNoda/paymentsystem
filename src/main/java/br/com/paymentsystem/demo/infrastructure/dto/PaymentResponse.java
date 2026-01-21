package br.com.paymentsystem.demo.infrastructure.dto;

import br.com.paymentsystem.demo.domain.payment.Payment;

import java.math.BigDecimal;

public record PaymentResponse(
        String idempotencyKey,
        String status,
        BigDecimal amount,
        String currency
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getIdempotencyKey(),
                payment.getStatus().name(),
                payment.getAmount(),
                payment.getCurrency()
        );
    }
}

