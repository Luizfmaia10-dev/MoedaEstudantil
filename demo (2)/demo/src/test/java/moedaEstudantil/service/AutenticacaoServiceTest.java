package moedaEstudantil.service;

import moedaEstudantil.dto.UsuarioAutenticadoDTO;
import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.EmpresaParceira;
import moedaEstudantil.model.Professor;
import moedaEstudantil.repository.AlunoRepository;
import moedaEstudantil.repository.EmpresaParceiraRepository;
import moedaEstudantil.repository.ProfessorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AutenticacaoServiceTest {
    private AlunoRepository alunos;
    private ProfessorRepository professores;
    private EmpresaParceiraRepository empresas;
    private SenhaService senhas;
    private AutenticacaoService service;

    @BeforeEach
    void preparar() {
        alunos = mock(AlunoRepository.class);
        professores = mock(ProfessorRepository.class);
        empresas = mock(EmpresaParceiraRepository.class);
        senhas = new SenhaService();
        service = new AutenticacaoService(alunos, professores, empresas, senhas);
    }

    @Test
    void autenticaAlunoNormalizandoEmail() {
        Aluno aluno = new Aluno();
        aluno.setId(7L);
        aluno.setNome("Ana");
        aluno.setEmail("ana@puc.br");
        aluno.setSenha(senhas.codificar("segredo"));
        when(alunos.findByEmailIgnoreCase("ana@puc.br")).thenReturn(Optional.of(aluno));

        UsuarioAutenticadoDTO resultado = service.autenticar(" ANA@PUC.BR ", "segredo", "ALUNO");

        assertEquals(7L, resultado.id());
        assertEquals("Ana", resultado.nome());
        assertEquals("ana@puc.br", resultado.email());
        assertEquals("ALUNO", resultado.perfil());
        assertEquals("{\"id\":7,\"nome\":\"Ana\",\"email\":\"ana@puc.br\",\"perfil\":\"ALUNO\"}",
                JsonMapper.builder().build().writeValueAsString(resultado));
        verifyNoInteractions(professores, empresas);
        verify(alunos, never()).save(any());
    }

    @Test
    void autenticaProfessorPreCadastradoEMigraSenhaLegada() {
        Professor professor = new Professor();
        professor.setNome("Paulo");
        professor.setEmail("paulo@puc.br");
        professor.setSenha("senha123");
        when(professores.findByEmailIgnoreCase("paulo@puc.br")).thenReturn(Optional.of(professor));

        UsuarioAutenticadoDTO resultado = service.autenticar("paulo@puc.br", "senha123", "PROFESSOR");

        assertEquals("PROFESSOR", resultado.perfil());
        assertTrue(senhas.estaCodificada(professor.getSenha()));
        verify(professores).save(professor);
        assertTrue(senhas.corresponde("senha123", professor.getSenha()));
        verifyNoInteractions(alunos, empresas);
    }

    @Test
    void autenticaEmpresaERejeitaSenhaIncorreta() {
        EmpresaParceira empresa = new EmpresaParceira();
        empresa.setNome("Loja");
        empresa.setEmail("loja@exemplo.com");
        empresa.setSenha(senhas.codificar("correta"));
        when(empresas.findByEmailIgnoreCase("loja@exemplo.com")).thenReturn(Optional.of(empresa));

        assertEquals("EMPRESA", service.autenticar("loja@exemplo.com", "correta", "EMPRESA").perfil());
        assertThrows(ResponseStatusException.class,
                () -> service.autenticar("loja@exemplo.com", "incorreta", "EMPRESA"));
        verifyNoInteractions(alunos, professores);
        verify(empresas, never()).save(any());
    }

    @Test
    void rejeitaUsuarioQuandoPerfilSelecionadoNaoCorresponde() {
        when(professores.findByEmailIgnoreCase("ana@puc.br")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> service.autenticar("ana@puc.br", "segredo", "PROFESSOR"));
        verify(alunos, never()).findByEmailIgnoreCase(anyString());
    }

    @ParameterizedTest
    @MethodSource("entradasIncompletas")
    void rejeitaEntradaIncompletaAntesDeConsultarRepositorio(String email, String senha, String perfil) {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.autenticar(email, senha, perfil));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        assertEquals("Informe e-mail, senha e perfil.", erro.getReason());
        verifyNoInteractions(alunos, professores, empresas);
    }

    static Stream<Arguments> entradasIncompletas() {
        return Stream.of(
                Arguments.of(null, "senha123", "ALUNO"),
                Arguments.of("", "senha123", "ALUNO"),
                Arguments.of("  ", "senha123", "ALUNO"),
                Arguments.of("ana@puc.br", null, "ALUNO"),
                Arguments.of("ana@puc.br", "", "ALUNO"),
                Arguments.of("ana@puc.br", "  ", "ALUNO"),
                Arguments.of("ana@puc.br", "senha123", null),
                Arguments.of("ana@puc.br", "senha123", ""),
                Arguments.of("ana@puc.br", "senha123", "  "));
    }

    @Test
    void rejeitaCredenciaisIncorretasComMensagemUnicaSemMigrarSenha() {
        Aluno aluno = new Aluno();
        aluno.setSenha("correta");
        when(alunos.findByEmailIgnoreCase("ana@puc.br")).thenReturn(Optional.of(aluno));

        ResponseStatusException erroSenha = assertThrows(ResponseStatusException.class,
                () -> service.autenticar("ana@puc.br", "errada", "ALUNO"));
        ResponseStatusException erroPerfil = assertThrows(ResponseStatusException.class,
                () -> service.autenticar("ana@puc.br", "correta", "PROFESSOR"));

        assertEquals(HttpStatus.UNAUTHORIZED, erroSenha.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, erroPerfil.getStatusCode());
        assertEquals("E-mail, senha ou perfil inválido.", erroSenha.getReason());
        assertEquals(erroSenha.getReason(), erroPerfil.getReason());
        assertEquals("correta", aluno.getSenha());
        verify(alunos, never()).save(any());
        verify(professores, never()).save(any());
        verifyNoInteractions(empresas);
    }

    @Test
    void migraSenhaLegadaDeAluno() {
        Aluno aluno = new Aluno();
        aluno.setSenha("legada");
        when(alunos.findByEmailIgnoreCase("ana@puc.br")).thenReturn(Optional.of(aluno));

        service.autenticar("ana@puc.br", "legada", "ALUNO");

        assertTrue(senhas.estaCodificada(aluno.getSenha()));
        assertTrue(senhas.corresponde("legada", aluno.getSenha()));
        verify(alunos).save(aluno);
    }

    @Test
    void migraSenhaLegadaDeEmpresaSemRemoverEspacosDaSenha() {
        EmpresaParceira empresa = new EmpresaParceira();
        empresa.setSenha(" legada ");
        when(empresas.findByEmailIgnoreCase("loja@exemplo.com")).thenReturn(Optional.of(empresa));

        assertEquals("EMPRESA", service.autenticar("LOJA@EXEMPLO.COM", " legada ", " empresa ").perfil());

        assertTrue(senhas.estaCodificada(empresa.getSenha()));
        assertTrue(senhas.corresponde(" legada ", empresa.getSenha()));
        assertFalse(senhas.corresponde("legada", empresa.getSenha()));
        verify(empresas).save(empresa);
    }
}
