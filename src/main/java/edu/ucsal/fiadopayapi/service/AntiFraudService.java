package edu.ucsal.fiadopayapi.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import edu.ucsal.fiadopayapi.annotations.AntiFraud;
import edu.ucsal.fiadopayapi.antifraud.AntiFraudRule;
import edu.ucsal.fiadopayapi.domain.Payment;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AntiFraudService {

    private final ApplicationContext ctx;

    public Optional<String> inspect(Payment p) {
        var rules = ctx.getBeansOfType(AntiFraudRule.class).values();

        for (AntiFraudRule rule : rules) {
            var annotation = rule.getClass().getAnnotation(AntiFraud.class);
            if (annotation == null) continue;

            String reason = rule.inspect(p);
            if (reason != null && !reason.isBlank()) {
                return Optional.of(annotation.name() + ":" + reason);
            }
        }

        return Optional.empty();
    }
}