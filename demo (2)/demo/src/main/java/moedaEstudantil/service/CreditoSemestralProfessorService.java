package moedaEstudantil.service;

import moedaEstudantil.model.Conta;
import moedaEstudantil.model.CreditoSemestralProfessor;
import moedaEstudantil.model.Professor;
import moedaEstudantil.repository.ContaRepository;
import moedaEstudantil.repository.CreditoSemestralProfessorRepository;
import moedaEstudantil.repository.ProfessorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class CreditoSemestralProfessorService {
    public static final int CARGA_SEMESTRAL = 1000;

    private final ProfessorRepository professores;
    private final ContaRepository contas;
    private final CreditoSemestralProfessorRepository creditos;

    public CreditoSemestralProfessorService(ProfessorRepository professores, ContaRepository contas,
                                             CreditoSemestralProfessorRepository creditos) {
        this.professores = professores;
        this.contas = contas;
        this.creditos = creditos;
    }

    @Transactional
    public int processar(LocalDate data) {
        int ano = data.getYear();
        int semestre = data.getMonthValue() <= 6 ? 1 : 2;
        int processados = 0;
        for (Professor professor : professores.findAll()) {
            if (creditos.existsByProfessorIdAndAnoAndSemestre(professor.getId(), ano, semestre)) continue;

            Conta conta = contas.findByUsuarioIdParaAtualizacao(professor.getId())
                    .orElseThrow(() -> new IllegalStateException("Professor sem conta: " + professor.getId()));
            if (creditos.existsByProfessorIdAndAnoAndSemestre(professor.getId(), ano, semestre)) continue;

            conta.setSaldoCorrente(conta.getSaldoCorrente() + CARGA_SEMESTRAL);
            CreditoSemestralProfessor credito = new CreditoSemestralProfessor();
            credito.setProfessor(professor);
            credito.setAno(ano);
            credito.setSemestre(semestre);
            credito.setQuantidade(CARGA_SEMESTRAL);
            credito.setDataCredito(LocalDateTime.now());
            creditos.save(credito);
            processados++;
        }
        return processados;
    }
}
