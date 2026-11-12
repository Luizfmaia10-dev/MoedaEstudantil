package moedaEstudantil.email;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class SmtpEmailSenderTest {
    @Test void entregaConteudoPeloSmtp() {
        var smtp = mock(JavaMailSender.class);
        new SmtpEmailSender(smtp, "origem@teste.com")
                .enviar(new EmailMensagem("destino@teste.com", "Assunto", "Cupom"), "123");
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(smtp).send(captor.capture());
        assertEquals("origem@teste.com", captor.getValue().getFrom());
        assertArrayEquals(new String[]{"destino@teste.com"}, captor.getValue().getTo());
        assertEquals("Cupom", captor.getValue().getText());
    }

    @Test void propagaFalhaParaPermitirNovaTentativaNaFila() {
        var smtp = mock(JavaMailSender.class);
        doThrow(new MailSendException("Offline")).when(smtp).send(any(SimpleMailMessage.class));
        assertThrows(MailSendException.class, () -> new SmtpEmailSender(smtp, "origem@teste.com")
                .enviar(new EmailMensagem("destino@teste.com", "A", "B"), "123"));
    }
}
