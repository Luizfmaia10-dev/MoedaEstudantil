package moedaEstudantil.dto;

/** saldoDisponivel = saldoCorrente - moedas em solicitacoes pendentes. */
public record SaldoDisponivelDTO(int saldoCorrente, int moedasPendentes, int saldoDisponivel) {
}
