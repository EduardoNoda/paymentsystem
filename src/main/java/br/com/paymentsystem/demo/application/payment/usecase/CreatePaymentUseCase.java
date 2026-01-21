package br.com.paymentsystem.demo.application.payment.usecase;

import br.com.paymentsystem.demo.application.payment.command.CreatePaymentCommand;
import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentRepository;
import org.springframework.stereotype.Service;

@Service
public class CreatePaymentUseCase {

    private final PaymentRepository paymentRepository;

    public CreatePaymentUseCase (
            PaymentRepository paymentRepository
    ){
        this.paymentRepository = paymentRepository;
    }

    public Payment execute(CreatePaymentCommand command) {

        Payment payment = Payment.create(
                command.idempotencyKey(),
                command.amount(),
                command.currency(),
                command.clientSnapshot()
        );

        return paymentRepository.save(payment);
    }
}