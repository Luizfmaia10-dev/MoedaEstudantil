package moedaEstudantil.model;

import jakarta.persistence.*;
import moedaEstudantil.email.EmailMensagem;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_pendente", indexes = @Index(name = "idx_email_entrega", columnList = "status,proximaTentativa"))
public class EmailPendente {
    public enum Status { PENDENTE, ENVIADO, FALHOU }

    @Id
    @Column(length = 36)
    private String id;
    @Column(nullable = false)
    private String destinatario;
    @Column(nullable = false)
    private String assunto;
    @Column(nullable = false, length = 10000)
    private String texto;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;
    private int tentativas;
    @Column(nullable = false)
    private Instant criadoEm;
    @Column(nullable = false)
    private Instant proximaTentativa;
    private Instant enviadoEm;
    private String ultimoErro;

    protected EmailPendente() {}

    public EmailPendente(EmailMensagem mensagem) {
        id = UUID.randomUUID().toString();
        destinatario = mensagem.destinatario();
        assunto = mensagem.assunto();
        texto = mensagem.texto();
        status = Status.PENDENTE;
        criadoEm = Instant.now();
        proximaTentativa = criadoEm;
    }

    public void confirmarEnvio(Instant agora) {
        tentativas++;
        status = Status.ENVIADO;
        enviadoEm = agora;
        ultimoErro = null;
    }

    public void registrarFalha(Instant agora, String erro) {
        tentativas++;
        ultimoErro = erro;
        // Todas as tentativas ficam dentro das 24 horas de idempotencia do Resend.
        if (tentativas >= 5 || criadoEm.plusSeconds(23 * 3600).isBefore(agora)) {
            status = Status.FALHOU;
        } else {
            proximaTentativa = agora.plusSeconds(30L << (tentativas - 1));
        }
    }

    public EmailMensagem mensagem() { return new EmailMensagem(destinatario, assunto, texto); }
    public String getId() { return id; }
    public Status getStatus() { return status; }
    public int getTentativas() { return tentativas; }
    public Instant getProximaTentativa() { return proximaTentativa; }
    public Instant getEnviadoEm() { return enviadoEm; }
    public String getUltimoErro() { return ultimoErro; }
}
