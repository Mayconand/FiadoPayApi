package edu.ucsal.fiadopayapi.controller;

import edu.ucsal.fiadopayapi.dto.PaymentRequest;
import edu.ucsal.fiadopayapi.dto.PaymentResponse;
import edu.ucsal.fiadopayapi.dto.RefundRequest;
import edu.ucsal.fiadopayapi.service.PaymentService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.Map;
@RestController
@RequestMapping("/fiadopay/gateway")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;

    @PostMapping("/payments")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PaymentResponse> createPayment(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authToken,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentRequest request
    ) {
        PaymentResponse response = service.criadorDePagamentos(authToken, idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/payments/{id}")
    public PaymentResponse getPayment(@PathVariable String id) {
        return service.getPayment(id);
    }

    @PostMapping("/refunds")
    @SecurityRequirement(name = "bearerAuth")
    public Map<String, Object> refundPayment(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authToken,
            @Valid @RequestBody RefundRequest request
    ) {
        return service.refund(authToken, request.paymentId());
    }
}

