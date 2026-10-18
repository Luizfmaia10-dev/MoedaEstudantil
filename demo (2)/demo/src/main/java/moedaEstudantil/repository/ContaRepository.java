package moedaEstudantil.repository;

import moedaEstudantil.model.Conta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContaRepository extends JpaRepository<Conta, Long> {
    Optional<Conta> findByUsuarioId(Long usuarioId);

    /**
     * Busca a conta com bloqueio pessimista (SELECT ... FOR UPDATE). Serializa operacoes
     * concorrentes sobre o saldo do mesmo usuario; deve ser chamado dentro de uma transacao.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conta c where c.usuario.id = :usuarioId")
    Optional<Conta> findByUsuarioIdParaAtualizacao(@Param("usuarioId") Long usuarioId);
}
