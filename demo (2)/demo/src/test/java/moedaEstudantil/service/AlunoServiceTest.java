package moedaEstudantil.service;

import moedaEstudantil.dto.AlunoDTO;
import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.Instituicao;
import moedaEstudantil.repository.AlunoRepository;
import moedaEstudantil.repository.InstituicaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

class AlunoServiceTest {
    private AlunoRepository alunos;
    private InstituicaoRepository instituicoes;
    private SenhaService senhas;
    private AlunoService service;

    @BeforeEach
    void preparar() {
        alunos = mock(AlunoRepository.class);
        instituicoes = mock(InstituicaoRepository.class);
        senhas = new SenhaService();
        service = new AlunoService(alunos, instituicoes, senhas);
    }

    @Test
    void cadastraNaInstituicaoExistenteComSaldoZeroESenhaProtegida() {
        Instituicao puc = new Instituicao();
        puc.setId(2L);
        puc.setNome("PUC Minas");
        when(instituicoes.findById(2L)).thenReturn(Optional.of(puc));
        when(alunos.findByEmailIgnoreCase("ana@puc.br")).thenReturn(Optional.empty());
        when(alunos.findByCpf("123")).thenReturn(Optional.empty());
        when(alunos.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AlunoDTO dto = novoAluno();
        dto.setSaldoMoedas(9999);
        AlunoDTO criado = service.criar(dto);

        assertEquals(0, criado.getSaldoMoedas());
        assertEquals("PUC Minas", criado.getInstituicaoNome());
        ArgumentCaptor<Aluno> captor = ArgumentCaptor.forClass(Aluno.class);
        verify(alunos).save(captor.capture());
        Aluno persistido = captor.getValue();
        assertTrue(senhas.corresponde("segredo", persistido.getSenha()));
    }

    @Test
    void rejeitaCadastroSemInstituicaoParticipante() {
        AlunoDTO dto = novoAluno();
        dto.setInstituicaoId(null);

        assertThrows(ResponseStatusException.class, () -> service.criar(dto));
        verify(alunos, never()).save(any());
    }

    @Test
    void rejeitaInstituicaoNaoCadastrada() {
        when(instituicoes.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.criar(novoAluno()));
        verify(alunos, never()).save(any());
    }

    private AlunoDTO novoAluno() {
        AlunoDTO dto = new AlunoDTO();
        dto.setNome("Ana");
        dto.setEmail(" ANA@PUC.BR ");
        dto.setSenha("segredo");
        dto.setCpf("123");
        dto.setRg("MG1");
        dto.setCurso("Sistemas");
        dto.setEndereco("Rua A");
        dto.setInstituicaoId(2L);
        return dto;
    }
}
