package moedaEstudantil.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AutomacaoTransferenciaEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(AutomacaoTransferenciaEventListener.class);
    private final AutomacaoTransferenciaService automacao;

    public AutomacaoTransferenciaEventListener(AutomacaoTransferenciaService automacao) {
        this.automacao = automacao;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void analisar(SolicitacaoTransferenciaCriadaEvent event) {
        try {
            automacao.avaliarSeAtiva(event.solicitacaoId());
        } catch (IaLocalIndisponivelException ex) {
            LOG.warn("IA indisponível para a solicitação {}", event.solicitacaoId());
        } catch (ResponseStatusException ex) {
            LOG.info("Solicitação {} não foi avaliada automaticamente: {}",
                    event.solicitacaoId(), ex.getReason());
        }
    }
}
