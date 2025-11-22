# FiadoPay API  
Simulador de Gateway de Pagamentos (Spring Boot + H2 + Webhooks)

A FiadoPay API é um simulador realista de gateway de pagamentos, com criação de merchants, autenticação, processamento assíncrono, antifraude por anotações e webhook dispatch.

----------

## Como executar

```bash
mvn spring-boot:run
```

Acessos úteis:

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2

----------

## Endpoints Principais

| Recurso              | Método | Caminho                                  |
|----------------------|--------|-------------------------------------------|
| Criar Merchant       | POST   | `/fiadopay/admin/merchants`               |
| Gerar Token          | POST   | `/fiadopay/auth/token`                    |
| Criar Pagamento      | POST   | `/fiadopay/gateway/payments`              |
| Consultar Pagamento  | GET    | `/fiadopay/gateway/payments/{id}`         |
| Solicitar Reembolso  | POST   | `/fiadopay/gateway/refunds`               |

----------

## Fluxo Básico de Uso

### 1. Criar Merchant

```bash
curl -X POST http://localhost:8080/fiadopay/admin/merchants \
 -H "Content-Type: application/json" \
 -d '{"name":"Minha Loja","webhookUrl":"http://localhost:8081/webhooks"}'
```

### 2. Obter Token de Acesso

```bash
curl -X POST http://localhost:8080/fiadopay/auth/token \
 -H "Content-Type: application/json" \
 -d '{"client_id":"<clientId>","client_secret":"<clientSecret>"}'
```

### 3. Criar Pagamento

```bash
curl -X POST http://localhost:8080/fiadopay/gateway/payments \
 -H "Authorization: Bearer FAKE-1" \
 -H "Idempotency-Key: 123" \
 -H "Content-Type: application/json" \
 -d '{"method":"CARD","currency":"BRL","amount":250.0}'
```

### 4. Consultar Pagamento

```bash
curl http://localhost:8080/fiadopay/gateway/payments/<paymentId>
```

----------

## Passo a passo detalhado para testar

1. Execute a API com `mvn spring-boot:run`
2. Crie um merchant e anote `clientId` e `clientSecret`
3. Gere um token de acesso
4. Envie um pagamento usando o token
5. Consulte o pagamento pelo ID
6. Aguarde o processamento assíncrono (status muda automaticamente)
7. Caso tenha informado `webhookUrl`, configure um endpoint para receber notificações

----------

## Tecnologias Utilizadas

- Java 21+
- Spring Boot
- Spring Data JPA
- H2 Database
- ExecutorService (assíncrono)
- Lombok
- Swagger / OpenAPI

----------


## Licença

Uso livre para fins acadêmicos, estudos e prototipação.
