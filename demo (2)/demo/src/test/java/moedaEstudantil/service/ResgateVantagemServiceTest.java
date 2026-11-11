package moedaEstudantil.service;

import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.Conta;
import moedaEstudantil.model.EmpresaParceira;
import moedaEstudantil.model.ResgateVantagem;
import moedaEstudantil.model.Vantagem;
import moedaEstudantil.repository.AlunoRepository;
import moedaEstudantil.repository.ContaRepository;
import moedaEstudantil.repository.TransacaoRepository;
import moedaEstudantil.repository.VantagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResgateVantagemServiceTest {

    private AlunoRepository alunoRepository;
    private ContaRepository contaRepository;
    private VantagemRepository vantagemRepository;
    private TransacaoRepository transacaoRepository;
    private NotificacaoService notificacaoService;
    private ResgateVantagemService service;
    private Aluno aluno;
    private Conta conta;
    private Vantagem vantagem;

    @BeforeEach
    void setUp() {
        alunoRepository = mock(AlunoRepository.class);
        contaRepository = mock(ContaRepository.class);
        vantagemRepository = mock(VantagemRepository.class);
        transacaoRepository = mock(TransacaoRepository.class);
        notificacaoService = mock(NotificacaoService.class);
        service = new ResgateVantagemService(alunoRepository, contaRepository, vantagemRepository,
                transacaoRepository, notificacaoService);

        aluno = new Aluno();
        aluno.setId(4L);
        aluno.setNome("Aluno Teste");
        EmpresaParceira empresa = new EmpresaParceira();
        empresa.setId(5L);
        empresa.setNome("Cantina");
        vantagem = new Vantagem();
        vantagem.setId(1L);
        vantagem.setNome("Combo Lanche");
        vantagem.setDescricao("Lanche e suco");
        vantagem.setCustoMoedas(40);
        vantagem.setAtiva(true);
        vantagem.setEmpresa(empresa);
        conta = new Conta();
        conta.setId(4L);
        conta.setUsuario(aluno);
        conta.setSaldoCorrente(70);

        when(alunoRepository.findById(4L)).thenReturn(Optional.of(aluno));
        when(contaRepository.findByUsuarioIdParaAtualizacao(4L)).thenReturn(Optional.of(conta));
        when(contaRepository.findByUsuarioId(4L)).thenReturn(Optional.of(conta));
        when(vantagemRepository.findById(1L)).thenReturn(Optional.of(vantagem));
        when(vantagemRepository.findAtivas()).thenReturn(List.of(vantagem));
    }

    @Test
    void resgataVantagemDebitaSaldoEGeraCupom() {
        var resultado = service.resgatar(4L, 1L);

        assertEquals(30, conta.getSaldoCorrente());
        assertEquals(30, resultado.saldoAtual());
        assertTrue(resultado.codigoCupom().matches("CUPOM-[A-F0-9]{8}"));
        ArgumentCaptor<ResgateVantagem> captor = ArgumentCaptor.forClass(ResgateVantagem.class);
        verify(transacaoRepository).save(captor.capture());
        assertEquals(resultado.codigoCupom(), captor.getValue().getCodigoCupom());
        verify(notificacaoService).notificarResgate(aluno, vantagem, resultado.codigoCupom());
    }

    @Test
    void rejeitaSaldoInsuficiente() {
        conta.setSaldoCorrente(39);

        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.resgatar(4L, 1L)).getStatusCode().value());
        verify(transacaoRepository, never()).save(any());
    }

    @Test
    void rejeitaVantagemInativa() {
        vantagem.setAtiva(false);

        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.resgatar(4L, 1L)).getStatusCode().value());
    }

    @Test
    void montaCatalogoComSaldoEVantagensAtivas() {
        var catalogo = service.catalogo(4L);

        assertEquals("Aluno Teste", catalogo.alunoNome());
        assertEquals(70, catalogo.saldo());
        assertEquals("Cantina", catalogo.vantagens().get(0).empresaNome());
    }
}
