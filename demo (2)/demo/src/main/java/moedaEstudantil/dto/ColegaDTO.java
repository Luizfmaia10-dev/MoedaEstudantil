package moedaEstudantil.dto;

/** Colega da mesma instituicao: expoe somente o necessario para escolher o destinatario. */
public record ColegaDTO(Long id, String nome, String curso) {
}
