package br.com.paymentsystem.demo.payment;

import br.com.paymentsystem.demo.application.payment.command.PaymentLockManager;
import br.com.paymentsystem.demo.application.payment.port.ActionOriginContext;
import br.com.paymentsystem.demo.application.payment.port.PaymentDataAnalyzer;
import br.com.paymentsystem.demo.application.payment.port.PaymentGateway;
import br.com.paymentsystem.demo.application.payment.usecase.ProcessPaymentUseCase;
import br.com.paymentsystem.demo.domain.payment.PaymentRepository;
import jakarta.persistence.EntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class ProcessPaymentTestConfig {

    @Bean
    public FakePaymentGateway fakePaymentGateway() {
        return new FakePaymentGateway();
    }

    @Bean
    public PaymentGateway paymentGateway(FakePaymentGateway fake) {
        return fake;
    }

    @Bean
    public FakeActionOriginContext fakeActionOriginContext() {
        return new FakeActionOriginContext();
    }

    @Bean
    public ActionOriginContext actionOriginContext(
            FakeActionOriginContext fake
    ) {
        return fake;
    }
    @Bean
    public ProcessPaymentUseCase processPaymentUseCase(
            PaymentRepository paymentRepository,
            PaymentGateway paymentGateway,
            PaymentLockManager paymentLockManager,
            PaymentDataAnalyzer paymentDataAnalyzer
    ) {
        return new ProcessPaymentUseCase(
                paymentRepository,
                paymentGateway,
                paymentLockManager,
                paymentDataAnalyzer
        );
    }
}
