package edu.ucsal.fiadopayapi.repository;

import edu.ucsal.fiadopayapi.domain.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByClientId(String clientId);

    boolean existsByName(String name);
}