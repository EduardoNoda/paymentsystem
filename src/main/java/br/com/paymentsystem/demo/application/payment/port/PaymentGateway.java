package br.com.paymentsystem.demo.application.payment.port;

import br.com.paymentsystem.demo.domain.payment.Payment;

public interface PaymentGateway {

    GatewayResult process(Payment payment);

}