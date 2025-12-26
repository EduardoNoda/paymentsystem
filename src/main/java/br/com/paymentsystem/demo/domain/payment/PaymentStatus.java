package br.com.paymentsystem.demo.domain.payment;

public enum PaymentStatus {
    RECEIVED,
    PROCESSING,
    TO_ANALYZE,
    APPROVED,
    RECUSED,
    FAIL,
    CANCEL_ADMIN
}