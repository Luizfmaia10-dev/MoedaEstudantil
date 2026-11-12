package moedaEstudantil.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AutomacaoTransferenciaEventListenerTest {

    @Test
    void delegaSolicitacaoConfirmadaParaAutomacao() {
        AutomacaoTransferenciaService automacao = mock(AutomacaoTransferenciaService.class);
        AutomacaoTransferenciaEventListener listener = new AutomacaoTransferenciaEventListener(automacao);

        listener.analisar(new SolicitacaoTransferenciaCriadaEvent(10L));

        verify(automacao).avaliarSeAtiva(10L);
    }
}
