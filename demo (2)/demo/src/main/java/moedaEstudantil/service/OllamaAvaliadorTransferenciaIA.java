package moedaEstudantil.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
public class OllamaAvaliadorTransferenciaIA implements AvaliadorTransferenciaIA {

    private static final String PROMPT = """
            Você avalia solicitações de transferência de moedas estudantis.
            Compare somente os dados fornecidos com os critérios escritos pelo professor.
            Não invente regras. Use PENDENTE quando os critérios forem ambíguos ou insuficientes.
            Responda exclusivamente no JSON definido pelo schema, com uma explicação curta.
            """;

    private final URI endpoint;
    private final String modelo;
    private final Duration timeout;
    private final JsonMapper json;
    private final HttpClient http;

    @Autowired
    public OllamaAvaliadorTransferenciaIA(
            @Value("${app.ai.ollama.url:http://localhost:11434}") String baseUrl,
            @Value("${app.ai.ollama.model:qwen2.5:0.5b-instruct}") String modelo,
            @Value("${app.ai.ollama.timeout:8s}") Duration timeout) {
        this(baseUrl, modelo, timeout, JsonMapper.builder().build());
    }

    OllamaAvaliadorTransferenciaIA(String baseUrl, String modelo, Duration timeout, JsonMapper json) {
        this.endpoint = URI.create(baseUrl.replaceAll("/+$", "") + "/api/chat");
        this.modelo = modelo;
        this.timeout = timeout;
        this.json = json;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public Resultado avaliar(Contexto contexto) {
        try {
            Map<String, Object> requisicao = Map.of(
                    "model", modelo,
                    "stream", false,
                    "format", schema(),
                    "options", Map.of("temperature", 0),
                    "messages", List.of(
                            Map.of("role", "system", "content", PROMPT),
                            Map.of("role", "user", "content", json.writeValueAsString(contexto))));

            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            json.writeValueAsString(requisicao), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Ollama respondeu HTTP " + response.statusCode());
            }
            JsonNode envelope = json.readTree(response.body());
            String conteudo = envelope.path("message").path("content").asText();
            if (conteudo.isBlank()) throw new IllegalStateException("Ollama devolveu resposta vazia");
            return validar(json.readValue(conteudo, Resultado.class));
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new IaLocalIndisponivelException("A avaliação automática não pôde ser concluída.", ex);
        }
    }

    private Resultado validar(Resultado resultado) {
        if (resultado == null || resultado.decisao() == null
                || !Double.isFinite(resultado.confianca())
                || resultado.confianca() < 0 || resultado.confianca() > 1
                || resultado.explicacao() == null || resultado.explicacao().isBlank()
                || resultado.explicacao().trim().length() > 300) {
            throw new IllegalArgumentException("Resposta de avaliação inválida");
        }
        return new Resultado(resultado.decisao(), resultado.confianca(), resultado.explicacao().trim());
    }

    private Map<String, Object> schema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "decisao", Map.of("type", "string", "enum", List.of("APROVAR", "RECUSAR", "PENDENTE")),
                        "confianca", Map.of("type", "number", "minimum", 0, "maximum", 1),
                        "explicacao", Map.of("type", "string", "maxLength", 300)),
                "required", List.of("decisao", "confianca", "explicacao"),
                "additionalProperties", false);
    }
}
