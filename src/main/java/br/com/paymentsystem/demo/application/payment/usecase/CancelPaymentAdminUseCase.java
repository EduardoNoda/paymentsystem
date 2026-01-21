package br.com.paymentsystem.demo.application.payment.usecase;

import br.com.paymentsystem.demo.application.payment.port.ActionOrigin;
import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentRepository;
import br.com.paymentsystem.demo.domain.payment.PaymentStatus;

public class CancelPaymentAdminUseCase {

    PaymentRepository paymentRepository;

    public CancelPaymentAdminUseCase (PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public void execute (String idempotencyKey, ActionOrigin origin) {
        if(origin != ActionOrigin.ADMIN) {
            throw new IllegalStateException("Somente ADMIN consegue cancelar pagamento");
        }

        Payment payment = paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new IllegalArgumentException("Pagamento não encontrado"));

        if(payment.getStatus() != PaymentStatus.TO_ANALYZE
        && payment.getStatus() != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Pagamento não pode ser cancelado a partir do status " + payment.getStatus());
        }

        paymentRepository.cancelPayment(idempotencyKey);
    }

}