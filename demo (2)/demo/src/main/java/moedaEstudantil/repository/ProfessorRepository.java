package moedaEstudantil.repository;

import moedaEstudantil.model.Professor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfessorRepository extends JpaRepository<Professor, Long> {
    Optional<Professor> findByEmailIgnoreCase(String email);

    /** Professores da instituicao que preencheram o criterio (nao nulo e nao vazio). */
    @Query("select p from Professor p where p.instituicao.id = :instituicaoId "
            + "and p.criteriosTransferencia is not null and trim(p.criteriosTransferencia) <> '' "
            + "order by p.nome")
    List<Professor> findComCriteriosByInstituicaoId(@Param("instituicaoId") Long instituicaoId);
}
