package moedaEstudantil.service;

import moedaEstudantil.dto.DistribuicaoMoedasRequest;
import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.Conta;
import moedaEstudantil.model.Envio;
import moedaEstudantil.model.Instituicao;
import moedaEstudantil.model.Professor;
import moedaEstudantil.repository.AlunoRepository;
import moedaEstudantil.repository.ContaRepository;
import moedaEstudantil.repository.ProfessorRepository;
import moedaEstudantil.repository.TransacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DistribuicaoMoedasServiceTest {

    private ProfessorRepository professorRepository;
    private AlunoRepository alunoRepository;
    private ContaRepository contaRepository;
    private TransacaoRepository transacaoRepository;
    private NotificacaoService notificacaoService;
    private DistribuicaoMoedasService service;
    private Professor professor;
    private Aluno aluno;
    private Conta contaProfessor;
    private Conta contaAluno;

    @BeforeEach
    void setUp() {
        professorRepository = mock(ProfessorRepository.class);
        alunoRepository = mock(AlunoRepository.class);
        contaRepository = mock(ContaRepository.class);
        transacaoRepository = mock(TransacaoRepository.class);
        notificacaoService = mock(NotificacaoService.class);
        service = new DistribuicaoMoedasService(professorRepository, alunoRepository, contaRepository,
                transacaoRepository, notificacaoService);

        Instituicao instituicao = new Instituicao();
        instituicao.setId(1L);
        professor = new Professor();
        professor.setId(1L);
        professor.setNome("Glender");
        professor.setInstituicao(instituicao);
        aluno = new Aluno();
        aluno.setId(4L);
        aluno.setNome("Aluno Teste");
        aluno.setCurso("Engenharia");
        aluno.setInstituicao(instituicao);

        contaProfessor = conta(1L, professor, 100);
        contaAluno = conta(4L, aluno, 20);
        when(professorRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(alunoRepository.findById(4L)).thenReturn(Optional.of(aluno));
        when(contaRepository.findByUsuarioIdParaAtualizacao(1L)).thenReturn(Optional.of(contaProfessor));
        when(contaRepository.findByUsuarioIdParaAtualizacao(4L)).thenReturn(Optional.of(contaAluno));
    }

    @Test
    void distribuiMoedasERegistraOsDoisLados() {
        var resultado = service.distribuir(1L,
                new DistribuicaoMoedasRequest(4L, 30, "Excelente participacao"));

        assertEquals(70, contaProfessor.getSaldoCorrente());
        assertEquals(50, contaAluno.getSaldoCorrente());
        assertEquals(70, resultado.saldoProfessor());
        ArgumentCaptor<Envio> captor = ArgumentCaptor.forClass(Envio.class);
        verify(transacaoRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals(List.of(30, 30), captor.getAllValues().stream().map(Envio::getQuantidade).toList());
        verify(notificacaoService).notificarRecebimento(aluno, professor, 30, "Excelente participacao");
    }

    @Test
    void rejeitaSaldoInsuficienteSemPersistir() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.distribuir(1L, new DistribuicaoMoedasRequest(4L, 101, "Motivo")));

        assertEquals(400, erro.getStatusCode().value());
        assertEquals(100, contaProfessor.getSaldoCorrente());
        verify(transacaoRepository, never()).save(any());
        verify(notificacaoService, never()).notificarRecebimento(any(), any(), any(Integer.class), any());
    }

    @Test
    void exigeMotivoEAlunoDaMesmaInstituicao() {
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.distribuir(1L, new DistribuicaoMoedasRequest(4L, 10, " "))).getStatusCode().value());

        Instituicao outra = new Instituicao();
        outra.setId(2L);
        aluno.setInstituicao(outra);
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.distribuir(1L, new DistribuicaoMoedasRequest(4L, 10, "Motivo"))).getStatusCode().value());
    }

    private Conta conta(Long id, moedaEstudantil.model.Usuario usuario, int saldo) {
        Conta conta = new Conta();
        conta.setId(id);
        conta.setUsuario(usuario);
        conta.setSaldoCorrente(saldo);
        return conta;
    }
}
