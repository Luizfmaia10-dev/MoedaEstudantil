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
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

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

        UsuarioAutenticadoDTO resultado = service.autenticar(" ANA@PUC.BR ", "segredo");

        assertEquals(7L, resultado.id());
        assertEquals("Ana", resultado.nome());
        assertEquals("ALUNO", resultado.perfil());
    }

    @Test
    void autenticaProfessorPreCadastradoEMigraSenhaLegada() {
        Professor professor = new Professor();
        professor.setNome("Paulo");
        professor.setEmail("paulo@puc.br");
        professor.setSenha("senha123");
        when(alunos.findByEmailIgnoreCase("paulo@puc.br")).thenReturn(Optional.empty());
        when(professores.findByEmailIgnoreCase("paulo@puc.br")).thenReturn(Optional.of(professor));

        UsuarioAutenticadoDTO resultado = service.autenticar("paulo@puc.br", "senha123");

        assertEquals("PROFESSOR", resultado.perfil());
        assertTrue(senhas.estaCodificada(professor.getSenha()));
        verify(professores).save(professor);
    }

    @Test
    void autenticaEmpresaERejeitaSenhaIncorreta() {
        EmpresaParceira empresa = new EmpresaParceira();
        empresa.setNome("Loja");
        empresa.setEmail("loja@exemplo.com");
        empresa.setSenha(senhas.codificar("correta"));
        when(alunos.findByEmailIgnoreCase("loja@exemplo.com")).thenReturn(Optional.empty());
        when(professores.findByEmailIgnoreCase("loja@exemplo.com")).thenReturn(Optional.empty());
        when(empresas.findByEmailIgnoreCase("loja@exemplo.com")).thenReturn(Optional.of(empresa));

        assertEquals("EMPRESA", service.autenticar("loja@exemplo.com", "correta").perfil());
        assertThrows(ResponseStatusException.class,
                () -> service.autenticar("loja@exemplo.com", "incorreta"));
    }
}
