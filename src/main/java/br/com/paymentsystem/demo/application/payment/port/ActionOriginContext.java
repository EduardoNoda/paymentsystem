package br.com.paymentsystem.demo.application.payment.port;

@FunctionalInterface
public interface ActionOriginContext {
    void set(ActionOrigin origin);
}