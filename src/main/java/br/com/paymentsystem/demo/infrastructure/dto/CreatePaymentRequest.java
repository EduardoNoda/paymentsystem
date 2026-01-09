package br.com.paymentsystem.demo.infrastructure.dto;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        String idempotencyKey,
        BigDecimal amount,
        String currency,
        String clientSnapshot
) {
}
