package br.com.paymentsystem.demo.infrastructure.batch;

import br.com.paymentsystem.demo.application.payment.port.ActionOrigin;
import br.com.paymentsystem.demo.application.payment.usecase.ProcessPaymentUseCase;
import br.com.paymentsystem.demo.domain.payment.PaymentRepository;
import br.com.paymentsystem.demo.exception.PaymentAlreadyBeingProcessException;
import br.com.paymentsystem.demo.infrastructure.transaction.ActionOriginTransaction;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentRetryJob {

    private final PaymentRepository paymentRepository;
    private final ProcessPaymentUseCase processPaymentUseCase;
    private final ActionOriginTransaction actionOriginTransaction;

    public PaymentRetryJob(
            PaymentRepository paymentRepository,
            ProcessPaymentUseCase processPaymentUseCase,
            ActionOriginTransaction actionOriginTransaction
    ) {
        this.paymentRepository = paymentRepository;
        this.processPaymentUseCase = processPaymentUseCase;
        this.actionOriginTransaction = actionOriginTransaction;
    }

    @Scheduled(fixedDelay = 30_000)
    public void execute() {

        List<String> paymentsToRetry =
                paymentRepository.findPaymentsToRetry();

        for (String key : paymentsToRetry) {
            try {
                actionOriginTransaction.run(ActionOrigin.JOB, () ->
                        processPaymentUseCase.execute(key)
                );
            } catch (PaymentAlreadyBeingProcessException e) {
                // alguém pegou antes → OK
            } catch (Exception e) {
                // falhou → permanece TO_ANALYZE → retry futuro
            }
        }
    }
}


