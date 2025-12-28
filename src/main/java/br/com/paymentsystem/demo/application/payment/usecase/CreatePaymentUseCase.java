package br.com.paymentsystem.demo.application.payment.usecase;

import br.com.paymentsystem.demo.application.payment.command.CreatePaymentCommand;
import br.com.paymentsystem.demo.application.payment.port.ActionOrigin;
import br.com.paymentsystem.demo.application.payment.port.ActionOriginContext;
import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentRepository;
import br.com.paymentsystem.demo.domain.payment.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final ActionOriginContext actionOriginContext;

    public CreatePaymentUseCase (
            PaymentRepository paymentRepository,
            ActionOriginContext actionOriginContext
    ){
        this.paymentRepository = paymentRepository;
        this.actionOriginContext = actionOriginContext;
    }

    @Transactional
    public Payment execute(CreatePaymentCommand command) {
        actionOriginContext.set(ActionOrigin.API);

        Payment payment = new Payment();
        payment.setIdempotencyKey(command.idempotencyKey());
        payment.setAmount(command.amount());
        payment.setCurrency(command.currency());
        payment.setClientSnapshot(command.clientSnapshot());
        payment.setStatus(PaymentStatus.RECEIVED);

        return paymentRepository.save(payment);
    }
}