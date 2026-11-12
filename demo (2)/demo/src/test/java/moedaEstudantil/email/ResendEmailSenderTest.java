package moedaEstudantil.email;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import tools.jackson.databind.json.JsonMapper;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class ResendEmailSenderTest {
    private HttpServer server;
    private ResendEmailSender sender;
    private final AtomicReference<String> body = new AtomicReference<>();
    private final AtomicReference<String> auth = new AtomicReference<>();
    private final AtomicReference<String> key = new AtomicReference<>();
    private int status = 200;
    private String response = "{\"id\":\"email-123\"}";
    private long delay;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/emails", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            key.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            if (delay > 0) {
                try { Thread.sleep(delay); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            try {
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } finally { exchange.close(); }
        });
        server.start();
        sender = new ResendEmailSender("chave-de-teste", "remetente@teste.com", Duration.ofSeconds(1), endpoint());
    }

    private URI endpoint() { return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/emails"); }
    @AfterEach void tearDown() { server.stop(0); }

    @Test
    void enviaJsonAutenticadoComChaveIdempotente() {
        sender.enviar(new EmailMensagem("destino@teste.com", "Assunto", "Motivo: \"trabalho\"\nCupom"), "moeda/123");
        var json = JsonMapper.builder().build().readTree(body.get());
        assertEquals("Bearer chave-de-teste", auth.get());
        assertEquals("moeda/123", key.get());
        assertEquals("remetente@teste.com", json.path("from").asText());
        assertEquals("destino@teste.com", json.path("to").get(0).asText());
        assertEquals("Motivo: \"trabalho\"\nCupom", json.path("text").asText());
    }

    @Test
    void rejeitaErroSemExporRespostaOuChave() {
        status = 403; response = "dados-pessoais-e-segredos";
        var erro = assertThrows(IllegalStateException.class,
                () -> sender.enviar(new EmailMensagem("destino@teste.com", "A", "B"), "123"));
        assertTrue(erro.getMessage().contains("403"));
        assertFalse(erro.getMessage().contains(response));
        assertFalse(erro.getMessage().contains("chave-de-teste"));
    }

    @Test
    void rejeitaSucessoSemIdentificador() {
        response = "{}";
        assertThrows(IllegalStateException.class,
                () -> sender.enviar(new EmailMensagem("destino@teste.com", "A", "B"), "123"));
    }

    @Test
    void limitaTempoDeEspera() {
        delay = 300;
        sender = new ResendEmailSender("teste", "origem@teste.com", Duration.ofMillis(50), endpoint());
        assertThrows(IllegalStateException.class,
                () -> sender.enviar(new EmailMensagem("destino@teste.com", "A", "B"), "123"));
    }

    @Test
    void exigeChaveAoSelecionarResend() {
        assertThrows(IllegalArgumentException.class,
                () -> new ResendEmailSender("", "origem@teste.com", Duration.ofSeconds(1), endpoint()));
    }
}
