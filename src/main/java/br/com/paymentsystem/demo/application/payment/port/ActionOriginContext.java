package br.com.paymentsystem.demo.application.payment.port;

public interface ActionOriginContext {
    void set(ActionOrigin origin);
    ActionOrigin get();
}