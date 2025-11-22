package edu.ucsal.fiadopayapi.controller;

import edu.ucsal.fiadopayapi.domain.Merchant;
import edu.ucsal.fiadopayapi.dto.MerchantCreateDTO;
import edu.ucsal.fiadopayapi.repository.MerchantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/fiadopay/admin/merchants")
@RequiredArgsConstructor
public class MerchantAdminController {
    private final MerchantRepository merchants;

    @PostMapping
    public Merchant create(@Valid @RequestBody MerchantCreateDTO dto) {

        validarNomeNaoDuplicado(dto.name());

        Merchant merchant = Merchant.builder()
                .name(dto.name())
                .webhookUrl(dto.webhookUrl())
                .clientId(gerarClientId())
                .clientSecret(gerarClientSecret())
                .status(Merchant.Status.ACTIVE)
                .build();

        return merchants.save(merchant);
    }


    private void validarNomeNaoDuplicado(String name) {
        if (merchants.existsByName(name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Merchant name already exists"
            );
        }
    }

    private String gerarClientId() {
        return UUID.randomUUID().toString();
    }

    private String gerarClientSecret() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}