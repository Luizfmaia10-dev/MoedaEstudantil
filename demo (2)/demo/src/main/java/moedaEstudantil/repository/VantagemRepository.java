package moedaEstudantil.repository;

import moedaEstudantil.model.Vantagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VantagemRepository extends JpaRepository<Vantagem, Long> {

    // Registros antigos (coluna ativa nula) sao tratados como ativos
    @Query("select v from Vantagem v where v.empresa.id = :empresaId and (v.ativa is null or v.ativa = true) order by v.id desc")
    List<Vantagem> findAtivasByEmpresaId(@Param("empresaId") Long empresaId);
}
