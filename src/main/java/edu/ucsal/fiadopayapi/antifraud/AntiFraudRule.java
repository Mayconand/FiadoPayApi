package edu.ucsal.fiadopayapi.antifraud;


import edu.ucsal.fiadopayapi.domain.Payment;

public interface AntiFraudRule {
    /** returns a non-empty reason when suspicious, otherwise null/empty */
    String inspect(Payment p);
}