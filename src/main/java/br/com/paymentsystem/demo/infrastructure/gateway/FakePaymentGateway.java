package br.com.paymentsystem.demo.infrastructure.gateway;

import br.com.paymentsystem.demo.application.payment.port.GatewayResult;
import br.com.paymentsystem.demo.application.payment.port.PaymentGateway;
import br.com.paymentsystem.demo.infrastructure.dto.PaymentGatewayRequest;

public class FakePaymentGateway implements PaymentGateway {

    private GatewayResult nextResult = GatewayResult.APPROVED;

    public void simulateNextResult (GatewayResult result) {
        this.nextResult = result;
    }


    public GatewayResult process(PaymentGatewayRequest request) {
        return nextResult;
    }
}