package moedaEstudantil.repository;

import moedaEstudantil.model.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlunoRepository extends JpaRepository<Aluno, Long> {
    Optional<Aluno> findByCpf(String cpf);
    Optional<Aluno> findByEmail(String email);

    /** Colegas da mesma instituicao, exceto o proprio aluno. */
    List<Aluno> findByInstituicaoIdAndIdNotOrderByNome(Long instituicaoId, Long id);
}
