package edu.ucsal.fiadopayapi.controller;

import edu.ucsal.fiadopayapi.domain.Merchant;
import edu.ucsal.fiadopayapi.dto.TokenRequest;
import edu.ucsal.fiadopayapi.dto.TokenResponse;
import edu.ucsal.fiadopayapi.repository.MerchantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/fiadopay/auth")
@RequiredArgsConstructor
public class AuthController {
    private final MerchantRepository merchants;

    @PostMapping("/token")
    public TokenResponse token(@RequestBody @Valid TokenRequest req) {

        var merchant = merchants.findByClientId(req.client_id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        boolean credenciaisInvalidas =
                !merchant.getClientSecret().equals(req.client_secret()) ||
                        merchant.getStatus() != Merchant.Status.ACTIVE;

        if (credenciaisInvalidas) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return createTokenFor(merchant);
    }

    private TokenResponse createTokenFor(Merchant merchant) {
        return new TokenResponse("FAKE-" + merchant.getId(), "Bearer", 3600);
    }
}