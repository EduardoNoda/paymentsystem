package br.com.paymentsystem.demo.application.payment.usecase;

import br.com.paymentsystem.demo.application.payment.port.ActionOrigin;
import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentRepository;
import br.com.paymentsystem.demo.domain.payment.PaymentStatus;
import br.com.paymentsystem.demo.infrastructure.persistence.PostgresActionOriginContext;
import org.springframework.stereotype.Service;

@Service
public class AnalyzePaymentUseCase {

    private final PostgresActionOriginContext postgresActionOriginContext;
    private final PaymentRepository paymentRepository;

    public AnalyzePaymentUseCase(
            PaymentRepository paymentRepository,
            PostgresActionOriginContext postgresActionOriginContext
    ) {
        this.paymentRepository = paymentRepository;
        this.postgresActionOriginContext = postgresActionOriginContext;
    }

    public void execute (
            String idempotencyKey,
            PaymentStatus status
    ) {
       ActionOrigin origin = postgresActionOriginContext.get();

        if(origin == ActionOrigin.API) {
            throw new IllegalStateException("API não está permitido receber pagamentos em analise");
        }

        Payment payment = paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new IllegalArgumentException("Pagamento não encontrado"));

        if(payment.getStatus() != PaymentStatus.TO_ANALYZE) {
            throw new IllegalStateException("Pagamentos só podem ser analisados se estiverem no estado TO_ANALYZE");
        }

        if(!isFinalStatus(status)) {
            throw new IllegalStateException("Estado inválido para análise " + status);
        }

        paymentRepository.returnToProcessing(idempotencyKey);
    }

    public boolean isFinalStatus(PaymentStatus status) {
        return status == PaymentStatus.APPROVED
                || status == PaymentStatus.RECUSED
                || status == PaymentStatus.FAIL;
    }
}