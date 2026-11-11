package moedaEstudantil.dto;

import java.util.List;

public record CatalogoVantagemDTO(Long alunoId, String alunoNome, int saldo, List<Item> vantagens) {
    public record Item(Long id, String nome, String descricao, String fotoUrl, int custoMoedas,
                       Long empresaId, String empresaNome) {}
}
