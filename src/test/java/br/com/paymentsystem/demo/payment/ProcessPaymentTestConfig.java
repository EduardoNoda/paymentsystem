package br.com.paymentsystem.demo.payment;

import br.com.paymentsystem.demo.application.payment.port.ActionOriginContext;
import br.com.paymentsystem.demo.application.payment.port.PaymentDataAnalyzer;
import br.com.paymentsystem.demo.application.payment.port.PaymentGateway;
import br.com.paymentsystem.demo.application.payment.usecase.ProcessPaymentUseCase;
import br.com.paymentsystem.demo.domain.payment.PaymentLeasePolicy;
import br.com.paymentsystem.demo.domain.payment.PaymentRepository;
import br.com.paymentsystem.demo.infrastructure.gateway.FakePaymentGateway;
import br.com.paymentsystem.demo.infrastructure.persistence.PostgresActionOriginContext;
import jakarta.persistence.EntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class ProcessPaymentTestConfig {

    EntityManager entityManager;

    @Bean
    public FakePaymentGateway fakePaymentGateway() {
        return new FakePaymentGateway();
    }

    @Bean
    public PaymentGateway paymentGateway(FakePaymentGateway fake) {
        return fake;
    }

    @Bean
    public PostgresActionOriginContext fakeActionOriginContext() {
        return new PostgresActionOriginContext(entityManager);
    }

    @Bean
    public ActionOriginContext actionOriginContext(
            PostgresActionOriginContext fake
    ) {
        return fake;
    }
    @Bean
    public ProcessPaymentUseCase processPaymentUseCase(
            ActionOriginContext actionOriginContext,
            PaymentRepository paymentRepository,
            PaymentGateway paymentGateway,
            PaymentDataAnalyzer paymentDataAnalyzer,
            PaymentLeasePolicy paymentLeasePolicy
    ) {
        return new ProcessPaymentUseCase(
                actionOriginContext,
                paymentRepository,
                paymentGateway,
                paymentDataAnalyzer,
                paymentLeasePolicy
        );
    }
}
