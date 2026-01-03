package br.com.paymentsystem.demo.payment;

import br.com.paymentsystem.demo.application.payment.port.GatewayResult;
import br.com.paymentsystem.demo.application.payment.port.PaymentGateway;
import br.com.paymentsystem.demo.domain.payment.Payment;
import org.springframework.context.annotation.Bean;

public class FakePaymentGateway implements PaymentGateway {
    private GatewayResult result = GatewayResult.APPROVED;

    @Bean
    public void willReturn (GatewayResult result) {
        this.result = result;
    }

    @Override
    public GatewayResult process(Payment payment){
        return result;
    }
}