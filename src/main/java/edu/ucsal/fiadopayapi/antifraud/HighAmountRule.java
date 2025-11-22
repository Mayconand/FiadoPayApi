package edu.ucsal.fiadopayapi.antifraud;


import org.springframework.stereotype.Component;

import edu.ucsal.fiadopayapi.annotations.AntiFraud;
import edu.ucsal.fiadopayapi.domain.Payment;


@AntiFraud(name = "HighAmount", threshold = 5000.0)
@Component
public class HighAmountRule implements AntiFraudRule {
    @Override
    public String inspect(Payment p) {
        if (p.getAmount().doubleValue() > 5000.0) {
            return "AMOUNT_TOO_HIGH";
        }
        return null;
    }
}