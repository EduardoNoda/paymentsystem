package br.com.paymentsystem.demo.system;

import br.com.paymentsystem.demo.application.payment.command.CreatePaymentCommand;
import br.com.paymentsystem.demo.application.payment.port.ActionOrigin;
import br.com.paymentsystem.demo.application.payment.port.GatewayResult;
import br.com.paymentsystem.demo.application.payment.usecase.CreatePaymentUseCase;
import br.com.paymentsystem.demo.application.payment.usecase.ProcessPaymentUseCase;
import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentRepository;
import br.com.paymentsystem.demo.domain.payment.PaymentStatus;
import br.com.paymentsystem.demo.infrastructure.transaction.ActionOriginTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(PaymentGatewayTestConfig.class)
class PaymentFlowSystemTest extends BaseSystemTest{

    @Autowired
    private CreatePaymentUseCase createPaymentUseCase;

    @Autowired
    private ProcessPaymentUseCase processPaymentUseCase;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ActionOriginTransaction executor;

    @Test
    void should_create_and_process_payment_successfully() {

        // arrange
        String idempotencyKey = "idem-system-001";

        CreatePaymentCommand command = new CreatePaymentCommand(
                idempotencyKey,
                new BigDecimal(1000),
                "BRL",
                """
                        {
                            "clientId": "123",
                            "paymentMethod" : "CREDIT-CARD"
                        }
                        """
        );

        when(paymentGateway.process(any()))
                .thenReturn(GatewayResult.APPROVED);

        executor.run(ActionOrigin.API, () -> {
            createPaymentUseCase.execute(command);
            processPaymentUseCase.execute(idempotencyKey);
        });

        // assert
        Payment payment = paymentRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElseThrow();

        assertThat(payment.getStatus())
                .isIn(
                        PaymentStatus.APPROVED,
                        PaymentStatus.RECUSED
                );
    }
}
