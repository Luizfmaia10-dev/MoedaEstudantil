package moedaEstudantil.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InterpretadorTransferenciaLocalTest {

    private final InterpretadorTransferenciaLocal interpretador = new InterpretadorTransferenciaLocal();

    @Test
    void reconheceQuantidadePorExtensoESolicitaDestinatarioQuandoForGenerico() {
        InterpretadorTransferenciaIA.Intencao intencao = interpretador.interpretar(
                "Transferir dez moedas para um colega",
                new InterpretadorTransferenciaIA.Rascunho(null, null, null, null));

        assertEquals(10, intencao.quantidade());
        assertNull(intencao.destinatario());
    }

    @Test
    void reconheceDadosCompletosEmPortugues() {
        InterpretadorTransferenciaIA.Intencao intencao = interpretador.interpretar(
                "Mande vinte e cinco moedas para Ana pelo professor Carlos porque ela ajudou no projeto",
                new InterpretadorTransferenciaIA.Rascunho(null, null, null, null));

        assertEquals(25, intencao.quantidade());
        assertEquals("Ana", intencao.destinatario());
        assertEquals("Carlos", intencao.professor());
        assertEquals("ela ajudou no projeto", intencao.justificativa());
    }

    @Test
    void preservaRascunhoAoReceberComplemento() {
        InterpretadorTransferenciaIA.Intencao intencao = interpretador.interpretar(
                "pelo professor Carlos Lima",
                new InterpretadorTransferenciaIA.Rascunho("Ana Souza", null, 20, "Projeto"));

        assertEquals("Ana Souza", intencao.destinatario());
        assertEquals("Carlos Lima", intencao.professor());
        assertEquals(20, intencao.quantidade());
        assertEquals("Projeto", intencao.justificativa());
    }
}
