package moedaEstudantil.service;

import moedaEstudantil.dto.AlunoDTO;
import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.Instituicao;
import moedaEstudantil.repository.AlunoRepository;
import moedaEstudantil.repository.InstituicaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
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
        assertEquals("ana@puc.br", persistido.getEmail());
        assertNotEquals("segredo", persistido.getSenha());
        assertTrue(senhas.estaCodificada(persistido.getSenha()));
        assertTrue(senhas.corresponde("segredo", persistido.getSenha()));
        assertNull(criado.getSenha());
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "cpf"})
    void rejeitaCadastroDuplicadoSemPersistir(String campo) {
        Instituicao puc = new Instituicao();
        when(instituicoes.findById(2L)).thenReturn(Optional.of(puc));
        if (campo.equals("email")) {
            when(alunos.findByEmailIgnoreCase("ana@puc.br")).thenReturn(Optional.of(new Aluno()));
        } else {
            when(alunos.findByCpf("123")).thenReturn(Optional.of(new Aluno()));
        }

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.criar(novoAluno()));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(alunos, never()).save(any());
    }

    @Test
    void atualizacaoCodificaNovaSenhaSemDevolveLa() {
        Aluno existente = alunoExistente();
        AlunoDTO dto = novoAluno();
        dto.setSenha("senha-nova");

        AlunoDTO resposta = service.atualizar(4L, dto);

        assertNotEquals("senha-nova", existente.getSenha());
        assertTrue(senhas.estaCodificada(existente.getSenha()));
        assertTrue(senhas.corresponde("senha-nova", existente.getSenha()));
        assertNull(resposta.getSenha());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void atualizacaoSemNovaSenhaPreservaHash(String senha) {
        Aluno existente = alunoExistente();
        String hashAnterior = existente.getSenha();
        AlunoDTO dto = novoAluno();
        dto.setSenha(senha);

        service.atualizar(4L, dto);

        assertEquals(hashAnterior, existente.getSenha());
    }

    private Aluno alunoExistente() {
        Instituicao puc = new Instituicao();
        puc.setId(2L);
        Aluno aluno = new Aluno();
        aluno.setId(4L);
        aluno.setSenha(senhas.codificar("senha-antiga"));
        when(instituicoes.findById(2L)).thenReturn(Optional.of(puc));
        when(alunos.findById(4L)).thenReturn(Optional.of(aluno));
        when(alunos.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return aluno;
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
