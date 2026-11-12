package moedaEstudantil.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mail.delivery-enabled", havingValue = "true", matchIfMissing = true)
public class EmailOutboxScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(EmailOutboxScheduler.class);
    private final EmailOutboxProcessor processor;

    public EmailOutboxScheduler(EmailOutboxProcessor processor) { this.processor = processor; }

    // Uma mensagem por rodada respeita o limite padrao do provedor em uma instancia.
    @Scheduled(fixedDelayString = "${app.mail.poll-interval-ms:1000}", initialDelayString = "${app.mail.initial-delay-ms:5000}")
    public void enviarPendentes() {
        try {
            processor.processarProximo();
        } catch (RuntimeException ex) {
            LOG.warn("Processamento de emails adiado: {}", ex.getClass().getSimpleName());
        }
    }
}
