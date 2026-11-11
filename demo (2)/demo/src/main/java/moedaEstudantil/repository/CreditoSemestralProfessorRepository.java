package moedaEstudantil.repository;

import moedaEstudantil.model.CreditoSemestralProfessor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditoSemestralProfessorRepository extends JpaRepository<CreditoSemestralProfessor, Long> {
    boolean existsByProfessorIdAndAnoAndSemestre(Long professorId, int ano, int semestre);
}
