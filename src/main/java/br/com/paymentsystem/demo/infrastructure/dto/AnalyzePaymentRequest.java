package br.com.paymentsystem.demo.infrastructure.dto;

import br.com.paymentsystem.demo.domain.payment.PaymentStatus;

public record AnalyzePaymentRequest(
        PaymentStatus status
) {
}
