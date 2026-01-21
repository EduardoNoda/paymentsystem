package br.com.paymentsystem.demo.infrastructure.dto;

import java.math.BigDecimal;

public interface GatewayData {
    String getIdempotencyKey();
    BigDecimal getAmount();
    String getCurrency();
    String getClientSnapshot();
}
