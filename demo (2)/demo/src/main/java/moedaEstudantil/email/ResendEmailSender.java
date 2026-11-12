package moedaEstudantil.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "resend")
public class ResendEmailSender implements EmailSender {
    private final String apiKey;
    private final String remetente;
    private final HttpClient http;
    private final URI endpoint;
    private final Duration timeout;
    private final JsonMapper json = JsonMapper.builder().build();

    @org.springframework.beans.factory.annotation.Autowired
    public ResendEmailSender(@Value("${app.mail.resend.api-key:}") String apiKey,
                             @Value("${app.mail.from}") String remetente,
                             @Value("${app.mail.timeout:10s}") Duration timeout) {
        this(apiKey, remetente, timeout, URI.create("https://api.resend.com/emails"));
    }

    // Endpoint substituivel apenas nos testes; em producao a chave vai somente para o Resend.
    ResendEmailSender(String apiKey, String remetente, Duration timeout, URI endpoint) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Configure RESEND_API_KEY para usar MAIL_PROVIDER=resend.");
        }
        this.apiKey = apiKey;
        this.remetente = remetente;
        this.timeout = timeout;
        this.endpoint = endpoint;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public void enviar(EmailMensagem email, String chaveIdempotencia) {
        String body = json.writeValueAsString(Map.of(
                "from", remetente, "to", List.of(email.destinatario()),
                "subject", email.assunto(), "text", email.texto()));
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(timeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("User-Agent", "MoedaEstudantil/1.0")
                .header("Idempotency-Key", chaveIdempotencia)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        try {
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                // Nao inclui resposta remota: pode conter dados pessoais ou credenciais.
                throw new IllegalStateException("Resend respondeu HTTP " + response.statusCode()
                        + ". Confira chave, remetente, destinatario e limites no painel Resend.");
            }
            if (json.readTree(response.body()).path("id").asText().isBlank()) {
                throw new IllegalStateException("Resend nao confirmou o identificador do email.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envio Resend interrompido.");
        } catch (IOException ex) {
            throw new IllegalStateException("Falha de conexao com Resend.");
        }
    }
}
