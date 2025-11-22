package edu.ucsal.fiadopayapi.payment;

import edu.ucsal.fiadopayapi.domain.Payment;

public interface PaymentProcessor {
    boolean process(Payment payment);
}