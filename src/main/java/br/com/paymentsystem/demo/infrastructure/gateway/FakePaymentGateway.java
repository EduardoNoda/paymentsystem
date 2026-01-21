package br.com.paymentsystem.demo.infrastructure.gateway;

import br.com.paymentsystem.demo.application.payment.port.GatewayResult;
import br.com.paymentsystem.demo.application.payment.port.PaymentGateway;
import br.com.paymentsystem.demo.infrastructure.dto.PaymentGatewayRequest;
import org.springframework.stereotype.Component;

@Component
public class FakePaymentGateway implements PaymentGateway {

    private final GatewayResult nextResult = GatewayResult.APPROVED;


    public GatewayResult process(PaymentGatewayRequest request) {
        return nextResult;
    }
}