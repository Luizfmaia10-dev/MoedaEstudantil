package moedaEstudantil.dto;

import java.util.List;

public record AssistenteTransferenciaDTO(
        String estado,
        String mensagem,
        Long destinatarioId,
        String destinatarioNome,
        Long avaliadorId,
        String avaliadorNome,
        Integer quantidade,
        String justificativa,
        List<Opcao> opcoes) {

    public record Opcao(String tipo, Long id, String nome, String detalhe) {
    }
}
