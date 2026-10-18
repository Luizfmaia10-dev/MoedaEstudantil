package moedaEstudantil.repository;

import moedaEstudantil.model.TransacaoGeral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransacaoRepository extends JpaRepository<TransacaoGeral, Long> {
    List<TransacaoGeral> findByContaIdOrderByDataHoraDescIdDesc(Long contaId);
}
