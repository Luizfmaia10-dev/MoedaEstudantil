package moedaEstudantil.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OllamaInterpretadorTransferenciaIATest {

    private final JsonMapper objectMapper = JsonMapper.builder().build();
    private HttpServer server;

    @AfterEach
    void encerrarServidor() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void enviaSchemaEstruturadoEConverteIntencao() throws Exception {
        AtomicReference<String> corpoRecebido = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/chat", exchange -> responder(exchange, corpoRecebido));
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        OllamaInterpretadorTransferenciaIA cliente = new OllamaInterpretadorTransferenciaIA(
                baseUrl, "qwen2.5:0.5b-instruct", Duration.ofSeconds(2), objectMapper);

        InterpretadorTransferenciaIA.Intencao intencao = cliente.interpretar(
                "Mande vinte moedas para Ana pelo professor Carlos",
                new InterpretadorTransferenciaIA.Rascunho(null, null, null, null));

        assertEquals("Ana", intencao.destinatario());
        assertEquals("Carlos", intencao.professor());
        assertEquals(20, intencao.quantidade());

        JsonNode request = objectMapper.readTree(corpoRecebido.get());
        assertEquals("qwen2.5:0.5b-instruct", request.path("model").asText());
        assertFalse(request.path("stream").asBoolean(true));
        assertEquals(0, request.path("options").path("temperature").asInt(-1));
        assertEquals("object", request.path("format").path("type").asText());
        assertNotNull(request.path("format").path("properties").get("quantidade"));
    }

    @Test
    void usaInterpretadorLocalQuandoModeloNaoEstaDisponivel() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/chat", exchange -> {
            byte[] bytes = "modelo nao encontrado".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        OllamaInterpretadorTransferenciaIA cliente = new OllamaInterpretadorTransferenciaIA(
                baseUrl, "modelo-ausente", Duration.ofSeconds(2), objectMapper);

        InterpretadorTransferenciaIA.Intencao intencao = cliente.interpretar(
                "Transferir dez moedas para um colega",
                new InterpretadorTransferenciaIA.Rascunho(null, null, null, null));

        assertEquals(10, intencao.quantidade());
        assertEquals(null, intencao.destinatario());
    }

    private void responder(HttpExchange exchange, AtomicReference<String> corpoRecebido) throws IOException {
        corpoRecebido.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        String content = "{\"destinatario\":\"Ana\",\"professor\":\"Carlos\","
                + "\"quantidade\":20,\"justificativa\":null}";
        String resposta = objectMapper.createObjectNode()
                .set("message", objectMapper.createObjectNode().put("content", content))
                .toString();
        byte[] bytes = resposta.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
