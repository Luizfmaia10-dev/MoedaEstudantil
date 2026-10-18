package moedaEstudantil.dto;

import java.util.Date;

public record SolicitacaoTransferenciaDTO(
        Long id,
        Date dataSolicitacao,
        Integer quantidade,
        String status,
        String justificativa,
        Long destinatarioId,
        String destinatarioNome,
        Long avaliadorId,
        String avaliadorNome) {
}
