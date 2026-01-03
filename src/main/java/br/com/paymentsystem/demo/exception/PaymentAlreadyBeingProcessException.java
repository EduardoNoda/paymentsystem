package  br.com.paymentsystem.demo.exception;

public class PaymentAlreadyBeingProcessException extends RuntimeException{

    public PaymentAlreadyBeingProcessException(String paymentId) {
        super("Payment " + paymentId + " já está sendo processado.");
    }

}