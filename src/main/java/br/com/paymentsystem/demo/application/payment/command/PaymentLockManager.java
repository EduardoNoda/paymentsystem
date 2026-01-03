package br.com.paymentsystem.demo.application.payment.command;

public interface PaymentLockManager {
    boolean tryAcquire(String idempotencyKey);
}