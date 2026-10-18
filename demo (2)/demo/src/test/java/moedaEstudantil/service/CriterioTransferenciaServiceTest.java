package moedaEstudantil.service;

import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.Instituicao;
import moedaEstudantil.model.Professor;
import moedaEstudantil.repository.AlunoRepository;
import moedaEstudantil.repository.ProfessorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CriterioTransferenciaServiceTest {

    private ProfessorRepository professorRepository;
    private AlunoRepository alunoRepository;
    private CriterioTransferenciaService service;
    private Professor professor;

    @BeforeEach
    void setUp() {
        professorRepository = mock(ProfessorRepository.class);
        alunoRepository = mock(AlunoRepository.class);
        service = new CriterioTransferenciaService(professorRepository, alunoRepository);

        professor = new Professor();
        professor.setId(1L);
        professor.setNome("Glender");
        when(professorRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(professorRepository.save(any(Professor.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void salvaTextoComTrim() {
        var dto = service.salvar(1L, "  Nota acima de 80  ");
        assertEquals("Nota acima de 80", dto.getCriterios());
    }

    @Test
    void textoVazioLimpaCriterio() {
        professor.setCriteriosTransferencia("antigo");
        var dto = service.salvar(1L, "   ");
        assertNull(dto.getCriterios());
    }

    @Test
    void rejeitaTextoMaiorQueOLimite() {
        String longo = "a".repeat(CriterioTransferenciaService.TAMANHO_MAXIMO + 1);
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.salvar(1L, longo)).getStatusCode().value());
    }

    @Test
    void aceitaTextoNoLimite() {
        String limite = "a".repeat(CriterioTransferenciaService.TAMANHO_MAXIMO);
        assertEquals(limite, service.salvar(1L, limite).getCriterios());
    }

    @Test
    void professorInexistenteRetorna404() {
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.salvar(99L, "x")).getStatusCode().value());
    }

    @Test
    void alunoVeSomenteProfessoresDaPropriaInstituicao() {
        Instituicao inst = new Instituicao();
        inst.setId(7L);
        Aluno aluno = new Aluno();
        aluno.setInstituicao(inst);
        when(alunoRepository.findById(4L)).thenReturn(Optional.of(aluno));
        professor.setCriteriosTransferencia("Criterio");
        when(professorRepository.findComCriteriosByInstituicaoId(7L)).thenReturn(List.of(professor));

        var lista = service.listarParaAluno(4L);

        assertEquals(1, lista.size());
        verify(professorRepository).findComCriteriosByInstituicaoId(7L);
    }

    @Test
    void alunoInexistenteRetorna404() {
        when(alunoRepository.findById(9L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.listarParaAluno(9L)).getStatusCode().value());
    }
}
