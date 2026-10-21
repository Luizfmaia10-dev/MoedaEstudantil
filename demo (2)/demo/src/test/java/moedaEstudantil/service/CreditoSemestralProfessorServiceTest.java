package moedaEstudantil.service;

import moedaEstudantil.model.Conta;
import moedaEstudantil.model.Professor;
import moedaEstudantil.repository.ContaRepository;
import moedaEstudantil.repository.CreditoSemestralProfessorRepository;
import moedaEstudantil.repository.ProfessorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreditoSemestralProfessorServiceTest {
    private ProfessorRepository professores;
    private ContaRepository contas;
    private CreditoSemestralProfessorRepository creditos;
    private CreditoSemestralProfessorService service;
    private Professor professor;
    private Conta conta;

    @BeforeEach
    void preparar() {
        professores = mock(ProfessorRepository.class);
        contas = mock(ContaRepository.class);
        creditos = mock(CreditoSemestralProfessorRepository.class);
        service = new CreditoSemestralProfessorService(professores, contas, creditos);
        professor = new Professor();
        professor.setId(5L);
        conta = new Conta();
        conta.setSaldoCorrente(250);
        when(professores.findAll()).thenReturn(List.of(professor));
        when(contas.findByUsuarioIdParaAtualizacao(5L)).thenReturn(Optional.of(conta));
    }

    @Test
    void creditaMilMoedasCumulativamenteNoPrimeiroSemestre() {
        int processados = service.processar(LocalDate.of(2026, 2, 10));

        assertEquals(1, processados);
        assertEquals(1250, conta.getSaldoCorrente());
        verify(creditos).save(argThat(c -> c.getAno() == 2026 && c.getSemestre() == 1
                && c.getQuantidade() == 1000));
    }

    @Test
    void naoDuplicaCreditoDoMesmoProfessorESemestre() {
        when(creditos.existsByProfessorIdAndAnoAndSemestre(5L, 2026, 2)).thenReturn(true);

        int processados = service.processar(LocalDate.of(2026, 10, 10));

        assertEquals(0, processados);
        assertEquals(250, conta.getSaldoCorrente());
        verify(creditos, never()).save(any());
    }
}
