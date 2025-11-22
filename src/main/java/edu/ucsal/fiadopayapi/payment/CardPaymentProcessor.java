package edu.ucsal.fiadopayapi.payment;

import org.springframework.stereotype.Component;

import edu.ucsal.fiadopayapi.annotations.PaymentMethod;
import edu.ucsal.fiadopayapi.domain.Payment;

@PaymentMethod(type = "CARD")
@Component
public class CardPaymentProcessor implements PaymentProcessor {

    @Override
    public boolean process(Payment payment) {
// Simulate basic card rule: decline if amount ends with .13 (funny business), otherwise approve
        return payment.getAmount().remainder(new java.math.BigDecimal("1")).compareTo(java.math.BigDecimal.ZERO) != 0
                || !payment.getAmount().toPlainString().endsWith(".13");
    }
}