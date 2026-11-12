package moedaEstudantil.dto;

public record AutomacaoTransferenciaDTO(
        boolean ativa,
        boolean possuiCriterios,
        String criterios,
        long pendentes) {
}
