package edu.ucsal.fiadopayapi.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.ucsal.fiadopayapi.annotations.PaymentMethod;
import edu.ucsal.fiadopayapi.dto.PaymentRequest;
import edu.ucsal.fiadopayapi.dto.PaymentResponse;
import edu.ucsal.fiadopayapi.domain.Merchant;
import edu.ucsal.fiadopayapi.domain.Payment;
import edu.ucsal.fiadopayapi.payment.PaymentProcessor;
import edu.ucsal.fiadopayapi.repository.MerchantRepository;
import edu.ucsal.fiadopayapi.repository.PaymentRepository;
import edu.ucsal.fiadopayapi.webhook.WebhookDispatcher;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    // Repositório dos merchants (clientes que aceitam o pagamento)
    private final MerchantRepository vendas;

    // Repositório dos pagamentos
    private final PaymentRepository pagamentos;

    // ApplicationContext para localizar beans com @PaymentMethod
    private final ApplicationContext ctx;

    // Executor para processamento assíncrono dos pagamentos
    private final ExecutorService fiadopayExecutor;

    // Serviço antifraude
    private final AntiFraudService antifraud;

    // Disparador de webhook para enviar notificações externas
    private final WebhookDispatcher webhookDispatcher;


    // Busca um pagamento pelo ID
    public PaymentResponse getPayment(String id) {
        Payment p = pagamentos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return toResponse(p);
    }


    // Processa um reembolso
    public Map<String, Object> refund(String auth, String paymentId) {
        Long merchantId = validadorAndGettersId(auth);

        // Busca o pagamento
        var p = pagamentos.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Garante que o pagamento pertence ao merchant autenticado
        if (!p.getMerchantId().equals(merchantId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        // Atualiza status como REFUND
        p.setStatus(Payment.Status.REFUNDED);
        p.setUpdatedAt(Instant.now());
        pagamentos.save(p);

        // Dispara webhook se existir
        Merchant m = vendas.findById(p.getMerchantId()).orElse(null);
        if (m != null && m.getWebhookUrl() != null) {
            webhookDispatcher.dispatchAsync(
                    m.getWebhookUrl(),
                    "{\"event\":\"payment.refunded\",\"id\":\"" + p.getId() + "\"}",
                    p.getId(),
                    "payment.refunded"
            );
        }

        return Map.of("status", "ok", "id", p.getId());
    }


    // Criação de pagamento (com idempotência)
    public PaymentResponse criadorDePagamentos(String authHeader, String idemKey, PaymentRequest req) {

        Long merchantId = validadorAndGettersId(authHeader);

        // 1. Idempotência
        Optional<Payment> existing = buscarPagamentoPorIdempotencia(idemKey, merchantId);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        // 2. Criação do pagamento
        Payment novoPagamento = criarNovoPagamento(merchantId, idemKey, req);
        pagamentos.save(novoPagamento);

        // 3. Processamento assíncrono
        fiadopayExecutor.submit(() -> processadorDePagamentos(novoPagamento));

        return toResponse(novoPagamento);
    }

    private Payment criarNovoPagamento(Long merchantId, String idemKey, PaymentRequest req) {

        int installments = req.installments() == null ? 1 : req.installments();
        Instant now = Instant.now();

        return Payment.builder()
                .id("pay_" + UUID.randomUUID())
                .merchantId(merchantId)
                .method(req.method().toUpperCase())
                .amount(req.amount())
                .currency(req.currency())
                .installments(installments)
                .monthlyInterest(0.0)
                .totalWithInterest(req.amount())
                .status(Payment.Status.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .idempotencyKey(idemKey)
                .metadataOrderId(req.metadataOrderId())
                .build();
    }


    // ---------------------------------------------------------------------------------------
    // PROCESSAMENTO ASSÍNCRONO DO PAGAMENTO
    // ---------------------------------------------------------------------------------------
    private void processadorDePagamentos(Payment p) {
        try {
            // Passo 1 — Anti-fraude
            var af = antifraud.inspect(p);
            if (af.isPresent()) {
                p.setStatus(Payment.Status.DECLINED);
                p.setUpdatedAt(Instant.now());
                pagamentos.save(p);
                notificadrDeVendas(p);
                return;
            }

            // Passo 2 — Seleção do PaymentProcessor via anotação @PaymentMethod
            Map<String, PaymentProcessor> procs = ctx.getBeansOfType(PaymentProcessor.class);
            PaymentProcessor chosen = null;

            for (PaymentProcessor pp : procs.values()) {
                PaymentMethod a = pp.getClass().getAnnotation(PaymentMethod.class);
                if (a != null && a.type().equalsIgnoreCase(p.getMethod())) {
                    chosen = pp;
                    break;
                }
            }

            // Se não houver processor, o pagamento é aprovado por padrão
            boolean approved = chosen == null || chosen.process(p);

            // Passo 3 — Cálculo de juros quando há parcelamento
            double monthlyInterest = 0.0;
            if (p.getInstallments() > 1) {
                monthlyInterest = 1.0; // juros fixo de 1% ao mês
                BigDecimal total = p.getAmount();
                total = total.add(total.multiply(
                        BigDecimal.valueOf((monthlyInterest / 100.0) * p.getInstallments())
                ));
                p.setTotalWithInterest(total);
            }

            p.setMonthlyInterest(monthlyInterest);
            p.setStatus(approved ? Payment.Status.APPROVED : Payment.Status.DECLINED);
            p.setUpdatedAt(Instant.now());
            pagamentos.save(p);

        } catch (Exception ex) {
            // Em caso de erro interno, expira o pagamento
            p.setStatus(Payment.Status.EXPIRED);
            p.setUpdatedAt(Instant.now());
            pagamentos.save(p);
        }
    }


    // ---------------------------------------------------------------------------------------
    // DISPARO DE WEBHOOK APÓS MUDANÇA DE STATUS
    // ---------------------------------------------------------------------------------------
    private void notificadrDeVendas(Payment p) {
        vendas.findById(p.getMerchantId())
                .map(Merchant::getWebhookUrl)
                .filter(url -> url != null && !url.isBlank())
                .ifPresent(url -> {
                    String payload = """
                    {
                        "event": "payment.updated",
                        "id": "%s",
                        "status": "%s"
                    }
                """.formatted(p.getId(), p.getStatus());

                    webhookDispatcher.dispatchAsync(
                            url,
                            payload,
                            p.getId(),
                            "payment.updated"
                    );
                });
    }



    // ---------------------------------------------------------------------------------------
    // VALIDA TOKEN E EXTRAI ID DO MERCHANT
    // ---------------------------------------------------------------------------------------
    private Long validadorAndGettersId(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.replace("Bearer", "").trim();

        if (!token.startsWith("FAKE-")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        try {
            return Long.parseLong(token.substring("FAKE-".length()));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }


    private Optional<Payment> buscarPagamentoPorIdempotencia(String idemKey, Long merchantId) {
        if (idemKey == null) return Optional.empty();
        return pagamentos.findByIdempotencyKeyAndMerchantId(idemKey, merchantId);
    }


    // ---------------------------------------------------------------------------------------
    // MAPEA PAYMENT → PaymentResponse
    // ---------------------------------------------------------------------------------------
    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getStatus().name(),
                p.getMethod(),
                p.getAmount(),
                p.getInstallments(),
                p.getMonthlyInterest(),
                p.getTotalWithInterest()
        );
    }
}
