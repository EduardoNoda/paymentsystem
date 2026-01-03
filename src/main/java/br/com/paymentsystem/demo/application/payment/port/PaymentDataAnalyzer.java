package br.com.paymentsystem.demo.application.payment.port;

import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentStatus;

public interface PaymentDataAnalyzer {

    PaymentStatus dataAnalyzer(Payment payment);

}