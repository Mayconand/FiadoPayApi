package edu.ucsal.fiadopayapi.repository;

import edu.ucsal.fiadopayapi.domain.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookDeliveryRepository extends  JpaRepository<WebhookDelivery, Long> {
}