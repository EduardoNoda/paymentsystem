package br.com.paymentsystem.demo.infrastructure.dto;

import java.math.BigDecimal;

public record PaymentGatewayRequest(
        String paymentId,
        BigDecimal amount,
        String currency
) {
}