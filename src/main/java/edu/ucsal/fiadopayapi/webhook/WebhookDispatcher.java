package edu.ucsal.fiadopayapi.webhook;

import java.time.Instant;
import java.util.concurrent.ExecutorService;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import edu.ucsal.fiadopayapi.domain.WebhookDelivery;
import edu.ucsal.fiadopayapi.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class WebhookDispatcher {
    private final ExecutorService fiadopayExecutor;
    private final RestTemplate rest = new RestTemplate();
    private final WebhookDeliveryRepository deliveries;


    public void dispatchAsync(String url, String payload, String eventId, String eventType) {

        WebhookDelivery d = WebhookDelivery.builder()
                .eventId(eventId)
                .eventType(eventType)
                .paymentId(eventId)
                .targetUrl(url)
                .payload(payload)
                .attempts(0)
                .delivered(false)
                .lastAttemptAt(Instant.now())
                .build();

        deliveries.save(d);

        fiadopayExecutor.submit(() -> {
            boolean sucesso = false;

            try {
                var headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                var ent = new HttpEntity<>(payload, headers);

                var resp = rest.postForEntity(url, ent, String.class);

                sucesso = resp.getStatusCode().is2xxSuccessful();

            } catch (Exception ignore) {
                // falha no envio — mesma lógica do original
            } finally {
                d.setAttempts(d.getAttempts() + 1);
                d.setLastAttemptAt(Instant.now());
                d.setDelivered(sucesso);
                deliveries.save(d);
            }
        });
    }

}
