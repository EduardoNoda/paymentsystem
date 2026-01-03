package br.com.paymentsystem.demo.application.payment.command;

import br.com.paymentsystem.demo.application.payment.port.PaymentDataAnalyzer;
import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Set;

public class DefaultPaymentDataAnalyzer implements PaymentDataAnalyzer {

    private static Set<String> SUPPORTED_CURRENCIES = Set.of("BRL", "USD", "EUR");

    public PaymentStatus dataAnalyzer(Payment payment) {

        if (payment.getAmount() == null ||
                payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentStatus.RECUSED;
        }

        if (payment.getCurrency() == null ||
                payment.getCurrency().length() != 3 ||
                !SUPPORTED_CURRENCIES.contains(payment.getCurrency())) {
            return PaymentStatus.RECUSED;
        }

        if (payment.getClientSnapshot() == null) {
            return PaymentStatus.RECUSED;
        }

        if (payment.getStatus() != PaymentStatus.PROCESSING) {
            return PaymentStatus.RECUSED;
        }

        // validação de JSON
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode snapshot = mapper.readTree(payment.getClientSnapshot());

            if (!snapshot.hasNonNull("clientId") ||
                    !snapshot.hasNonNull("paymentMethod")) {
                return PaymentStatus.RECUSED;
            }

        } catch (Exception e) {
            throw new IllegalStateException("Erro técnico ao validar snapshot", e);
        }

        return PaymentStatus.APPROVED;
    }

}