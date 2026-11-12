package moedaEstudantil.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static moedaEstudantil.service.AvaliadorTransferenciaIA.Decisao.APROVAR;
import static org.junit.jupiter.api.Assertions.*;

class OllamaAvaliadorTransferenciaIATest {

    private final JsonMapper json = JsonMapper.builder().build();
    private HttpServer server;

    @AfterEach
    void encerrarServidor() {
        if (server != null) server.stop(0);
    }

    @Test
    void enviaCriteriosComSchemaEConverteDecisao() throws Exception {
        AtomicReference<String> corpoRecebido = new AtomicReference<>();
        iniciar(exchange -> responderDecisao(exchange, corpoRecebido));

        var resultado = cliente().avaliar(new AvaliadorTransferenciaIA.Contexto(
                "Aprovar colaboração em projetos", 20, "Ajuda no trabalho", "Ana", "Bia"));

        assertEquals(APROVAR, resultado.decisao());
        assertEquals(0.93, resultado.confianca());
        assertEquals("Critério atendido.", resultado.explicacao());

        JsonNode request = json.readTree(corpoRecebido.get());
        assertEquals("qwen2.5:0.5b-instruct", request.path("model").asText());
        assertEquals(0, request.path("options").path("temperature").asInt(-1));
        assertEquals("object", request.path("format").path("type").asText());
        assertNotNull(request.path("format").path("properties").get("confianca"));
        assertTrue(request.path("messages").get(1).path("content").asText().contains("Aprovar colaboração"));
    }

    @Test
    void naoDecideQuandoOllamaFalha() throws Exception {
        iniciar(exchange -> responder(exchange, 503, "indisponível"));

        assertThrows(IaLocalIndisponivelException.class,
                () -> cliente().avaliar(contexto()));
    }

    @Test
    void rejeitaConfiancaForaDoIntervalo() throws Exception {
        iniciar(exchange -> {
            String content = "{\"decisao\":\"APROVAR\",\"confianca\":1.2,\"explicacao\":\"Inválida.\"}";
            responder(exchange, 200, envelope(content));
        });

        assertThrows(IaLocalIndisponivelException.class,
                () -> cliente().avaliar(contexto()));
    }

    private AvaliadorTransferenciaIA.Contexto contexto() {
        return new AvaliadorTransferenciaIA.Contexto("Aprovar colaboração", 20,
                "Ajuda no trabalho", "Ana", "Bia");
    }

    private OllamaAvaliadorTransferenciaIA cliente() {
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        return new OllamaAvaliadorTransferenciaIA(
                baseUrl, "qwen2.5:0.5b-instruct", Duration.ofSeconds(2), json);
    }

    private void iniciar(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/chat", handler);
        server.start();
    }

    private void responderDecisao(HttpExchange exchange, AtomicReference<String> corpo) throws IOException {
        corpo.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        responder(exchange, 200, envelope(
                "{\"decisao\":\"APROVAR\",\"confianca\":0.93,\"explicacao\":\"Critério atendido.\"}"));
    }

    private String envelope(String content) {
        return json.createObjectNode()
                .set("message", json.createObjectNode().put("content", content))
                .toString();
    }

    private void responder(HttpExchange exchange, int status, String corpo) throws IOException {
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
