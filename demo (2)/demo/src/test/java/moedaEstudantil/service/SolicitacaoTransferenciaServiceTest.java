package moedaEstudantil.service;

import moedaEstudantil.dto.SaldoDisponivelDTO;
import moedaEstudantil.dto.SolicitacaoTransferenciaDTO;
import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.Conta;
import moedaEstudantil.model.Instituicao;
import moedaEstudantil.model.Professor;
import moedaEstudantil.model.SolicitacaoTransferencia;
import moedaEstudantil.model.StatusSolicitacaoTransferencia;
import moedaEstudantil.model.TipoTransacao;
import moedaEstudantil.model.TransferenciaEntreAlunos;
import moedaEstudantil.repository.AlunoRepository;
import moedaEstudantil.repository.ContaRepository;
import moedaEstudantil.repository.ProfessorRepository;
import moedaEstudantil.repository.SolicitacaoTransferenciaRepository;
import moedaEstudantil.repository.TransacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SolicitacaoTransferenciaServiceTest {

    private SolicitacaoTransferenciaRepository solicitacaoRepository;
    private AlunoRepository alunoRepository;
    private ProfessorRepository professorRepository;
    private ContaRepository contaRepository;
    private TransacaoRepository transacaoRepository;
    private SolicitacaoTransferenciaService service;

    private Instituicao puc;
    private Instituicao ufmg;
    private Aluno remetente;
    private Aluno destinatario;
    private Professor avaliador;
    private Conta contaRemetente;
    private Conta contaDestinatario;

    @BeforeEach
    void setUp() {
        solicitacaoRepository = mock(SolicitacaoTransferenciaRepository.class);
        alunoRepository = mock(AlunoRepository.class);
        professorRepository = mock(ProfessorRepository.class);
        contaRepository = mock(ContaRepository.class);
        transacaoRepository = mock(TransacaoRepository.class);
        service = new SolicitacaoTransferenciaService(
                solicitacaoRepository, alunoRepository, professorRepository, contaRepository, transacaoRepository);

        puc = new Instituicao();
        puc.setId(1L);
        puc.setNome("PUC Minas");

        ufmg = new Instituicao();
        ufmg.setId(2L);
        ufmg.setNome("UFMG");

        remetente = new Aluno();
        remetente.setId(4L);
        remetente.setNome("Aluno Remetente");
        remetente.setCurso("Engenharia de Software");
        remetente.setInstituicao(puc);

        destinatario = new Aluno();
        destinatario.setId(6L);
        destinatario.setNome("Colega PUC");
        destinatario.setCurso("Ciencia da Computacao");
        destinatario.setInstituicao(puc);

        avaliador = new Professor();
        avaliador.setId(1L);
        avaliador.setNome("Glender");
        avaliador.setInstituicao(puc);

        contaRemetente = new Conta();
        contaRemetente.setId(4L);
        contaRemetente.setSaldoCorrente(70);
        contaRemetente.setUsuario(remetente);

        contaDestinatario = new Conta();
        contaDestinatario.setId(6L);
        contaDestinatario.setSaldoCorrente(50);
        contaDestinatario.setUsuario(destinatario);

        when(alunoRepository.findById(4L)).thenReturn(Optional.of(remetente));
        when(alunoRepository.findById(6L)).thenReturn(Optional.of(destinatario));
        when(professorRepository.findById(1L)).thenReturn(Optional.of(avaliador));
        when(contaRepository.findByUsuarioIdParaAtualizacao(4L)).thenReturn(Optional.of(contaRemetente));
        when(contaRepository.findByUsuarioIdParaAtualizacao(6L)).thenReturn(Optional.of(contaDestinatario));
        when(contaRepository.findByUsuarioId(4L)).thenReturn(Optional.of(contaRemetente));
        when(solicitacaoRepository.somarQuantidadePorRemetenteEStatus(4L, StatusSolicitacaoTransferencia.PENDENTE))
                .thenReturn(0L);
        when(solicitacaoRepository.save(any(SolicitacaoTransferencia.class))).thenAnswer(i -> {
            SolicitacaoTransferencia s = i.getArgument(0);
            if (s.getId() == null) s.setId(10L);
            return s;
        });
    }

    private SolicitacaoTransferencia solicitacaoPendente(int quantidade) {
        SolicitacaoTransferencia s = new SolicitacaoTransferencia();
        s.setId(10L);
        s.setRemetente(remetente);
        s.setDestinatario(destinatario);
        s.setAvaliador(avaliador);
        s.setQuantidade(quantidade);
        s.setStatus(StatusSolicitacaoTransferencia.PENDENTE);
        when(solicitacaoRepository.findByIdParaAtualizacao(10L)).thenReturn(Optional.of(s));
        return s;
    }

    /* ---------- Testes RF-012 (Solicitacao e Cancelamento pelo Aluno) ---------- */

    @Test
    void criaSolicitacaoValidaComBloqueioPessimista() {
        SolicitacaoTransferenciaDTO dto = service.criar(4L, 6L, 1L, 30, "  Ajuda no trabalho  ");

        assertEquals(10L, dto.id());
        assertEquals(30, dto.quantidade());
        assertEquals("PENDENTE", dto.status());
        assertEquals("Ajuda no trabalho", dto.justificativa());
        assertEquals(6L, dto.destinatarioId());
        assertEquals(1L, dto.avaliadorId());
        verify(contaRepository).findByUsuarioIdParaAtualizacao(4L);
    }

    @Test
    void rejeitaQuantidadeZeroOuNegativa() {
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.criar(4L, 6L, 1L, 0, null)).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.criar(4L, 6L, 1L, -10, null)).getStatusCode().value());
    }

    @Test
    void rejeitaQuandoUltrapassaSaldoConsiderandoPendentes() {
        when(solicitacaoRepository.somarQuantidadePorRemetenteEStatus(4L, StatusSolicitacaoTransferencia.PENDENTE))
                .thenReturn(50L);

        SaldoDisponivelDTO saldo = service.saldoDisponivel(4L);
        assertEquals(70, saldo.saldoCorrente());
        assertEquals(50, saldo.moedasPendentes());
        assertEquals(20, saldo.saldoDisponivel());

        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.criar(4L, 6L, 1L, 25, null)).getStatusCode().value());
    }

    @Test
    void rejeitaTransferenciaParaSiMesmo() {
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.criar(4L, 4L, 1L, 10, null)).getStatusCode().value());
    }

    @Test
    void rejeitaDestinatarioDeOutraInstituicao() {
        destinatario.setInstituicao(ufmg);
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.criar(4L, 6L, 1L, 10, null)).getStatusCode().value());
    }

    @Test
    void rejeitaProfessorDeOutraInstituicao() {
        avaliador.setInstituicao(ufmg);
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.criar(4L, 6L, 1L, 10, null)).getStatusCode().value());
    }

    @Test
    void rejeitaJustificativaAcimaDoLimite() {
        String longa = "x".repeat(SolicitacaoTransferenciaService.JUSTIFICATIVA_MAXIMA + 1);
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.criar(4L, 6L, 1L, 10, longa)).getStatusCode().value());
    }

    @Test
    void cancelaSolicitacaoPendenteDoProprioAluno() {
        solicitacaoPendente(20);

        SolicitacaoTransferenciaDTO dto = service.cancelar(4L, 10L);

        assertEquals("CANCELADA", dto.status());
        verify(contaRepository).findByUsuarioIdParaAtualizacao(4L);
    }

    @Test
    void naoPermiteCancelarSolicitacaoJaAvaliada() {
        SolicitacaoTransferencia s = solicitacaoPendente(20);
        s.setStatus(StatusSolicitacaoTransferencia.APROVADA);

        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.cancelar(4L, 10L)).getStatusCode().value());
    }

    /* ---------- Testes RF-013 (Avaliacao pelo Professor) ---------- */

    @Test
    void aprovarDebitaRemetenteCreditaDestinatarioEGeraTransacoesNoExtrato() {
        solicitacaoPendente(25);

        SolicitacaoTransferenciaDTO dto = service.aprovar(1L, 10L, "  Criterio atendido  ");

        assertEquals("APROVADA", dto.status());
        assertEquals("Criterio atendido", dto.observacaoAvaliador());
        assertNotNull(dto.dataAvaliacao());
        assertEquals(45, contaRemetente.getSaldoCorrente());
        assertEquals(75, contaDestinatario.getSaldoCorrente());

        ArgumentCaptor<TransferenciaEntreAlunos> captor = ArgumentCaptor.forClass(TransferenciaEntreAlunos.class);
        verify(transacaoRepository, times(2)).save(captor.capture());
        List<TransferenciaEntreAlunos> gravadas = captor.getAllValues();

        assertEquals(TipoTransacao.SAIDA, gravadas.get(0).getTipoMovimentacao());
        assertEquals(contaRemetente, gravadas.get(0).getConta());
        assertEquals(25, gravadas.get(0).getQuantidade());

        assertEquals(TipoTransacao.ENTRADA, gravadas.get(1).getTipoMovimentacao());
        assertEquals(contaDestinatario, gravadas.get(1).getConta());
        assertEquals(25, gravadas.get(1).getQuantidade());
    }

    @Test
    void recusarMudaStatusSemMovimentarSaldoNemGerarTransacoes() {
        solicitacaoPendente(25);

        SolicitacaoTransferenciaDTO dto = service.recusar(1L, 10L, "Faltou participacao nos laboratorios");

        assertEquals("RECUSADA", dto.status());
        assertEquals("Faltou participacao nos laboratorios", dto.observacaoAvaliador());
        assertEquals(70, contaRemetente.getSaldoCorrente());
        assertEquals(50, contaDestinatario.getSaldoCorrente());
        verify(transacaoRepository, never()).save(any());
    }

    @Test
    void naoPermiteOutroProfessorAvaliarSolicitacaoAlheia() {
        solicitacaoPendente(20);
        Professor outro = new Professor();
        outro.setId(2L);
        when(professorRepository.findById(2L)).thenReturn(Optional.of(outro));

        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.aprovar(2L, 10L, null)).getStatusCode().value());
    }

    @Test
    void naoPermiteAvaliarSolicitacaoQueNaoEstejaPendente() {
        SolicitacaoTransferencia s = solicitacaoPendente(20);
        s.setStatus(StatusSolicitacaoTransferencia.CANCELADA);

        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.aprovar(1L, 10L, null)).getStatusCode().value());
    }

    @Test
    void rejeitaAprovacaoSeSaldoAtualDoRemetenteFicouInsuficiente() {
        solicitacaoPendente(50);
        contaRemetente.setSaldoCorrente(30);

        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.aprovar(1L, 10L, null)).getStatusCode().value());
        verify(transacaoRepository, never()).save(any());
    }
}
