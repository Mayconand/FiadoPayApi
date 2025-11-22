package edu.ucsal.fiadopayapi.dto;

import jakarta.validation.constraints.NotBlank;

public record RefundRequest(
        @NotBlank String paymentId
) {}