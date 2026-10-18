package moedaEstudantil.dto;

import java.util.Date;

public record SolicitacaoTransferenciaDTO(
        Long id,
        Date dataSolicitacao,
        Date dataAvaliacao,
        Integer quantidade,
        String status,
        String justificativa,
        String observacaoAvaliador,
        Long remetenteId,
        String remetenteNome,
        String remetenteCurso,
        Long destinatarioId,
        String destinatarioNome,
        String destinatarioCurso,
        Long avaliadorId,
        String avaliadorNome) {
}
