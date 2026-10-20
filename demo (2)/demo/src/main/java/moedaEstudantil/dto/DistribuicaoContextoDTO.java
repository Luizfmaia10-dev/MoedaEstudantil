package moedaEstudantil.dto;

import java.util.List;

public record DistribuicaoContextoDTO(Long professorId, String professorNome, int saldo,
                                     List<AlunoDistribuicaoDTO> alunos) {}
