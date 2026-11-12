package moedaEstudantil.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailSender implements EmailSender {
    private final JavaMailSender sender;
    private final String remetente;

    public SmtpEmailSender(JavaMailSender sender, @Value("${app.mail.from}") String remetente) {
        this.sender = sender;
        this.remetente = remetente;
    }

    @Override
    public void enviar(EmailMensagem email, String chaveIdempotencia) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(email.destinatario());
        mensagem.setSubject(email.assunto());
        mensagem.setText(email.texto());
        sender.send(mensagem);
    }
}
