package moedaEstudantil.email;

import moedaEstudantil.model.EmailPendente;
import moedaEstudantil.repository.EmailPendenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class EmailOutboxProcessorTest {
    private EmailPendenteRepository repository;
    private EmailSender sender;
    private EmailOutboxProcessor processor;
    private EmailPendente email;

    @BeforeEach void setUp() {
        repository = mock(EmailPendenteRepository.class);
        sender = mock(EmailSender.class);
        processor = new EmailOutboxProcessor(repository, sender);
        email = new EmailPendente(new EmailMensagem("aluno@teste.com", "Assunto", "Texto"));
        when(repository.buscarProntos(eq(EmailPendente.Status.PENDENTE), any(), any())).thenReturn(List.of(email));
    }

    @Test void confirmaAceiteEPersisteData() {
        processor.processarProximo();
        assertEquals(EmailPendente.Status.ENVIADO, email.getStatus());
        assertEquals(1, email.getTentativas());
        assertNotNull(email.getEnviadoEm());
        verify(sender).enviar(email.mensagem(), "moeda-email/" + email.getId());
        verify(repository).save(email);
    }

    @Test void falhaMantemMensagemPendenteComAtrasoESemDadosSensiveis() {
        doThrow(new IllegalStateException("segredo")).when(sender).enviar(any(), anyString());
        Instant antes = Instant.now();
        assertDoesNotThrow(processor::processarProximo);
        assertEquals(EmailPendente.Status.PENDENTE, email.getStatus());
        assertEquals(1, email.getTentativas());
        assertTrue(email.getProximaTentativa().isAfter(antes.plusSeconds(29)));
        assertFalse(email.getUltimoErro().contains("segredo"));
        verify(repository).save(email);
    }

    @Test void novaTentativaMantemChaveIdempotente() {
        doThrow(new IllegalStateException()).doNothing().when(sender).enviar(any(), anyString());
        processor.processarProximo();
        processor.processarProximo();
        verify(sender, times(2)).enviar(email.mensagem(), "moeda-email/" + email.getId());
        assertEquals(EmailPendente.Status.ENVIADO, email.getStatus());
        assertNull(email.getUltimoErro());
    }

    @Test void encerraAposCincoFalhasComAtrasoExponencial() {
        Instant agora = Instant.now();
        for (int i = 0; i < 4; i++) {
            email.registrarFalha(agora, "Falha");
            assertEquals(agora.plusSeconds(30L << i), email.getProximaTentativa());
            assertEquals(EmailPendente.Status.PENDENTE, email.getStatus());
        }
        email.registrarFalha(agora, "Falha");
        assertEquals(EmailPendente.Status.FALHOU, email.getStatus());
        assertEquals(5, email.getTentativas());
    }

    @Test void filaVaziaNaoChamaProvedor() {
        when(repository.buscarProntos(any(), any(), any())).thenReturn(List.of());
        processor.processarProximo();
        verifyNoInteractions(sender);
    }
}
