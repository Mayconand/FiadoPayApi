package edu.ucsal.fiadopayapi.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(
        @NotBlank String client_id,
        @NotBlank String client_secret
) {}