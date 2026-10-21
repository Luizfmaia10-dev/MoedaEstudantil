package moedaEstudantil.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SenhaServiceTest {

    private final SenhaService service = new SenhaService();

    @Test
    void codificaESempreValidaPeloTextoOriginal() {
        String hash = service.codificar("senha123");

        assertNotEquals("senha123", hash);
        assertTrue(service.corresponde("senha123", hash));
        assertFalse(service.corresponde("errada", hash));
    }

    @Test
    void reconheceSenhaLegadaSemConfundirSenhaIncorreta() {
        assertTrue(service.corresponde("legada", "legada"));
        assertFalse(service.corresponde("outra", "legada"));
        assertFalse(service.estaCodificada("legada"));
    }
}
