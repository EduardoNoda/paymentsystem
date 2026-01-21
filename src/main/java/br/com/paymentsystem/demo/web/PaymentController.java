package br.com.paymentsystem.demo.web;

import br.com.paymentsystem.demo.application.payment.command.CreatePaymentCommand;
import br.com.paymentsystem.demo.application.payment.port.ActionOrigin;
import br.com.paymentsystem.demo.application.payment.usecase.CreatePaymentUseCase;
import br.com.paymentsystem.demo.application.payment.usecase.ProcessPaymentUseCase;
import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentRepository;
import br.com.paymentsystem.demo.infrastructure.dto.CreatePaymentRequest;
import br.com.paymentsystem.demo.infrastructure.dto.PaymentResponse;
import br.com.paymentsystem.demo.infrastructure.transaction.ActionOriginTransaction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/payment")
public class PaymentController {
    private final PaymentRepository paymentRepository;
    private final ProcessPaymentUseCase processPaymentUseCase;
    private final CreatePaymentUseCase createPaymentUseCase;
    private final ActionOriginTransaction actionOriginTransaction;

    public PaymentController (
            PaymentRepository paymentRepository,
            ProcessPaymentUseCase processPaymentUseCase,
            CreatePaymentUseCase createPaymentUseCase,
            ActionOriginTransaction actionOriginTransaction
    ) {
        this.paymentRepository = paymentRepository;
        this.processPaymentUseCase = processPaymentUseCase;
        this.createPaymentUseCase = createPaymentUseCase;
        this.actionOriginTransaction = actionOriginTransaction;
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody CreatePaymentRequest request) {
        CreatePaymentCommand command = new CreatePaymentCommand(
                request.idempotencyKey(),
                request.amount(),
                request.currency(),
                request.clientSnapshot()
        );
        actionOriginTransaction.run(ActionOrigin.API, () -> createPaymentUseCase.execute(command));

        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<Void> process(@PathVariable String id) {
        processPaymentUseCase.execute(id);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> get(@PathVariable String id) {
        Payment payment = paymentRepository.findByIdempotencyKey(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }
}