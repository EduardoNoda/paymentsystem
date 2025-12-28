package br.com.paymentsystem.demo.application.payment.command;

import java.math.BigDecimal;

public record CreatePaymentCommand(String idempotencyKey,
                                   BigDecimal amount,
                                   String currency,
                                   String clientSnapshot
                                   ) {}