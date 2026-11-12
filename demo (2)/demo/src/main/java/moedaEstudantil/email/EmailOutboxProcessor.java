package moedaEstudantil.email;

import moedaEstudantil.model.EmailPendente;
import moedaEstudantil.repository.EmailPendenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
public class EmailOutboxProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(EmailOutboxProcessor.class);
    private final EmailPendenteRepository repository;
    private final EmailSender sender;

    public EmailOutboxProcessor(EmailPendenteRepository repository, EmailSender sender) {
        this.repository = repository;
        this.sender = sender;
    }

    @Transactional
    public void processarProximo() {
        var prontos = repository.buscarProntos(EmailPendente.Status.PENDENTE, Instant.now(), PageRequest.of(0, 1));
        for (EmailPendente email : prontos) {
            try {
                sender.enviar(email.mensagem(), "moeda-email/" + email.getId());
                email.confirmarEnvio(Instant.now());
                LOG.info("Email {} aceito pelo provedor.", email.getId());
            } catch (RuntimeException ex) {
                // Persistir somente a categoria: mensagens externas podem conter segredos ou dados pessoais.
                email.registrarFalha(Instant.now(), ex.getClass().getSimpleName());
                LOG.warn("Email {}: tentativa {}, estado {}. Confira configuracao e painel do provedor.",
                        email.getId(), email.getTentativas(), email.getStatus());
            }
            repository.save(email);
        }
    }
}
