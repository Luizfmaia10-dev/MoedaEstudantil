package moedaEstudantil.repository;

import jakarta.persistence.LockModeType;
import moedaEstudantil.model.EmailPendente;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface EmailPendenteRepository extends JpaRepository<EmailPendente, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EmailPendente e where e.status = :status and e.proximaTentativa <= :agora order by e.proximaTentativa, e.id")
    List<EmailPendente> buscarProntos(@Param("status") EmailPendente.Status status,
                                    @Param("agora") Instant agora, Pageable pagina);
}
