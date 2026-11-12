package moedaEstudantil.service;

import moedaEstudantil.dto.AutomacaoTransferenciaDTO;
import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.Professor;
import moedaEstudantil.model.SolicitacaoTransferencia;
import moedaEstudantil.model.StatusSolicitacaoTransferencia;
import moedaEstudantil.repository.ProfessorRepository;
import moedaEstudantil.repository.SolicitacaoTransferenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static moedaEstudantil.service.AvaliadorTransferenciaIA.Decisao.APROVAR;
import static moedaEstudantil.service.AvaliadorTransferenciaIA.Decisao.PENDENTE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AutomacaoTransferenciaServiceTest {

    private ProfessorRepository professorRepository;
    private SolicitacaoTransferenciaRepository solicitacaoRepository;
    private AvaliadorTransferenciaIA avaliador;
    private SolicitacaoTransferenciaService solicitacaoService;
    private AutomacaoTransferenciaService service;
    private Professor professor;
    private SolicitacaoTransferencia solicitacao;

    @BeforeEach
    void setUp() {
        professorRepository = mock(ProfessorRepository.class);
        solicitacaoRepository = mock(SolicitacaoTransferenciaRepository.class);
        avaliador = mock(AvaliadorTransferenciaIA.class);
        solicitacaoService = mock(SolicitacaoTransferenciaService.class);
        service = new AutomacaoTransferenciaService(
                professorRepository, solicitacaoRepository, avaliador, solicitacaoService);

        professor = new Professor();
        professor.setId(1L);
        professor.setNome("Carlos");
        professor.setCriteriosTransferencia("Aprovar colaboração comprovada em projetos.");
        professor.setAutomacaoTransferenciasAtiva(true);
        when(professorRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(professorRepository.save(any(Professor.class))).thenAnswer(i -> i.getArgument(0));

        Aluno remetente = new Aluno();
        remetente.setNome("Ana");
        Aluno destinatario = new Aluno();
        destinatario.setNome("Bia");
        solicitacao = new SolicitacaoTransferencia();
        solicitacao.setId(10L);
        solicitacao.setAvaliador(professor);
        solicitacao.setRemetente(remetente);
        solicitacao.setDestinatario(destinatario);
        solicitacao.setQuantidade(20);
        solicitacao.setJustificativa("Ajuda no projeto final");
        solicitacao.setStatus(StatusSolicitacaoTransferencia.PENDENTE);
        when(solicitacaoRepository.findById(10L)).thenReturn(Optional.of(solicitacao));
    }

    @Test
    void impedeAtivacaoSemCriterios() {
        professor.setAutomacaoTransferenciasAtiva(false);
        professor.setCriteriosTransferencia("   ");

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.configurar(1L, true));

        assertEquals(400, erro.getStatusCode().value());
        assertFalse(professor.isAutomacaoTransferenciasAtiva());
    }

    @Test
    void baixaConfiancaMantemSolicitacaoPendente() {
        when(avaliador.avaliar(any())).thenReturn(
                new AvaliadorTransferenciaIA.Resultado(APROVAR, 0.79, "Confiança insuficiente."));

        service.avaliarSeAtiva(10L);

        verifyNoInteractions(solicitacaoService);
    }

    @Test
    void decisaoConfiavelUsaAprovacaoTransacionalAuditada() {
        when(avaliador.avaliar(any())).thenReturn(
                new AvaliadorTransferenciaIA.Resultado(APROVAR, 0.90, "Critérios atendidos."));

        service.avaliarSeAtiva(10L);

        verify(solicitacaoService).aprovarAutomaticamente(
                1L, 10L, "Critérios atendidos.", 0.90);
    }

    @Test
    void decisaoPendenteNaoAlteraSolicitacao() {
        when(avaliador.avaliar(any())).thenReturn(
                new AvaliadorTransferenciaIA.Resultado(PENDENTE, 0.95, "Critérios insuficientes."));

        service.avaliarSeAtiva(10L);

        verifyNoInteractions(solicitacaoService);
    }

    @Test
    void ativacaoProcessaPendenciasEInterrompeLoteSeIaFalhar() {
        professor.setAutomacaoTransferenciasAtiva(false);
        SolicitacaoTransferencia segunda = new SolicitacaoTransferencia();
        segunda.setId(11L);
        when(solicitacaoRepository.findByAvaliadorIdAndStatusOrderByDataSolicitacaoAscIdAsc(
                1L, StatusSolicitacaoTransferencia.PENDENTE)).thenReturn(List.of(solicitacao, segunda));
        when(avaliador.avaliar(any())).thenThrow(
                new IaLocalIndisponivelException("fora", new RuntimeException("host")));

        AutomacaoTransferenciaDTO dto = service.configurar(1L, true);

        assertTrue(dto.ativa());
        verify(solicitacaoRepository).findById(10L);
        verify(solicitacaoRepository, never()).findById(11L);
    }
}
