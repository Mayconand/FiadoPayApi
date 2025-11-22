package edu.ucsal.fiadopayapi.repository;
import edu.ucsal.fiadopayapi.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByIdempotencyKeyAndMerchantId(String ik, Long mid);
}