package moedaEstudantil.service;

import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.Professor;
import moedaEstudantil.model.Vantagem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificacaoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificacaoService.class);

    private final JavaMailSender mailSender;
    private final String remetente;

    public NotificacaoService(JavaMailSender mailSender, @Value("${app.mail.from}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    public void notificarRecebimento(Aluno aluno, Professor professor, int quantidade, String motivo) {
        enviar(aluno.getEmail(), "Voce recebeu moedas estudantis",
                "Ola, " + aluno.getNome() + "!\n\nVoce recebeu " + quantidade
                        + " moedas do professor " + professor.getNome() + ".\nMotivo: " + motivo + ".");
    }

    public void notificarResgate(Aluno aluno, Vantagem vantagem, String codigo) {
        enviar(aluno.getEmail(), "Cupom da vantagem resgatada",
                "Ola, " + aluno.getNome() + "!\n\nSeu resgate de " + vantagem.getNome()
                        + " foi confirmado.\nCodigo para a troca presencial: " + codigo + ".");
        enviar(vantagem.getEmpresa().getEmail(), "Novo resgate de vantagem",
                "A vantagem " + vantagem.getNome() + " foi resgatada por " + aluno.getNome()
                        + ".\nCodigo para conferencia: " + codigo + ".");
    }

    private void enviar(String destinatario, String assunto, String texto) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(destinatario);
        mensagem.setSubject(assunto);
        mensagem.setText(texto);
        try {
            mailSender.send(mensagem);
        } catch (MailException erro) {
            LOGGER.warn("E-mail para {} nao enviado; a operacao principal foi preservada: {}",
                    destinatario, erro.getMessage());
        }
    }
}
