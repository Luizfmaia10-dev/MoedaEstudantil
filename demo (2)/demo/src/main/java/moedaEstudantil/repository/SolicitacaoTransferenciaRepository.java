package moedaEstudantil.repository;

import jakarta.persistence.LockModeType;
import moedaEstudantil.model.SolicitacaoTransferencia;
import moedaEstudantil.model.StatusSolicitacaoTransferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitacaoTransferenciaRepository extends JpaRepository<SolicitacaoTransferencia, Long> {

    List<SolicitacaoTransferencia> findByRemetenteIdOrderByDataSolicitacaoDescIdDesc(Long remetenteId);

    List<SolicitacaoTransferencia> findByAvaliadorIdOrderByDataSolicitacaoDescIdDesc(Long avaliadorId);

    /** Soma das moedas ja comprometidas em solicitacoes do remetente com o status informado. */
    @Query("select coalesce(sum(s.quantidade), 0) from SolicitacaoTransferencia s "
            + "where s.remetente.id = :remetenteId and s.status = :status")
    Long somarQuantidadePorRemetenteEStatus(@Param("remetenteId") Long remetenteId,
                                            @Param("status") StatusSolicitacaoTransferencia status);

    /** Bloqueio pessimista da solicitacao para evitar corrida entre cancelamento e avaliacao. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitacaoTransferencia s where s.id = :id")
    Optional<SolicitacaoTransferencia> findByIdParaAtualizacao(@Param("id") Long id);
}
