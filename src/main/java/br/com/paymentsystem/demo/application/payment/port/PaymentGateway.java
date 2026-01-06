package br.com.paymentsystem.demo.application.payment.port;

import br.com.paymentsystem.demo.infrastructure.dto.PaymentGatewayRequest;

public interface PaymentGateway {

    GatewayResult process(PaymentGatewayRequest payment);

}