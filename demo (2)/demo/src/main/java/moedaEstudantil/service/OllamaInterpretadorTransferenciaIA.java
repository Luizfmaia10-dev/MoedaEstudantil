package moedaEstudantil.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class OllamaInterpretadorTransferenciaIA implements InterpretadorTransferenciaIA {

    private static final Logger LOG = LoggerFactory.getLogger(OllamaInterpretadorTransferenciaIA.class);

    private static final String PROMPT_SISTEMA = """
            Você interpreta pedidos de transferência de moedas estudantis em português brasileiro.
            Extraia somente destinatário, professor avaliador, quantidade inteira e justificativa.
            Preserve no resultado os valores já existentes no rascunho quando a nova mensagem não os substituir.
            Não invente pessoas, identificadores, saldo ou permissão. Campos desconhecidos devem ser null.
            Responda exclusivamente no JSON definido pelo schema.
            """;

    private final URI endpoint;
    private final String modelo;
    private final Duration timeout;
    private final JsonMapper json;
    private final HttpClient http;
    private final InterpretadorTransferenciaIA fallback = new InterpretadorTransferenciaLocal();

    @Autowired
    public OllamaInterpretadorTransferenciaIA(
            @Value("${app.ai.ollama.url:http://localhost:11434}") String baseUrl,
            @Value("${app.ai.ollama.model:qwen2.5:0.5b-instruct}") String modelo,
            @Value("${app.ai.ollama.timeout:8s}") Duration timeout) {
        this(baseUrl, modelo, timeout, JsonMapper.builder().build());
    }

    OllamaInterpretadorTransferenciaIA(String baseUrl, String modelo, Duration timeout, JsonMapper json) {
        this.endpoint = URI.create(baseUrl.replaceAll("/+$", "") + "/api/chat");
        this.modelo = modelo;
        this.timeout = timeout;
        this.json = json;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public Intencao interpretar(String mensagem, Rascunho rascunho) {
        try {
            String contexto = json.writeValueAsString(rascunho == null
                    ? new Rascunho(null, null, null, null)
                    : rascunho);
            Map<String, Object> requisicao = Map.of(
                    "model", modelo,
                    "stream", false,
                    "format", schema(),
                    "options", Map.of("temperature", 0),
                    "messages", List.of(
                            Map.of("role", "system", "content", PROMPT_SISTEMA),
                            Map.of("role", "user", "content",
                                    "Rascunho atual: " + contexto + "\nNova mensagem do aluno: " + mensagem)));

            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(requisicao), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Ollama respondeu HTTP " + response.statusCode());
            }
            JsonNode envelope = json.readTree(response.body());
            String conteudo = envelope.path("message").path("content").asText();
            if (conteudo.isBlank()) {
                throw new IllegalStateException("Ollama devolveu uma resposta vazia");
            }
            return json.readValue(conteudo, Intencao.class);
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warn("Ollama indisponível; usando interpretação local: {}", ex.getMessage());
            return fallback.interpretar(mensagem, rascunho);
        }
    }

    private Map<String, Object> schema() {
        Map<String, Object> textoOpcional = Map.of("type", List.of("string", "null"));
        Map<String, Object> inteiroOpcional = Map.of("type", List.of("integer", "null"));
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "destinatario", textoOpcional,
                        "professor", textoOpcional,
                        "quantidade", inteiroOpcional,
                        "justificativa", textoOpcional),
                "required", List.of("destinatario", "professor", "quantidade", "justificativa"),
                "additionalProperties", false);
    }
}
