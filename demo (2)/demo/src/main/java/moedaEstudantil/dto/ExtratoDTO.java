package moedaEstudantil.dto;

import java.util.Date;
import java.util.List;

public class ExtratoDTO {
    private Long usuarioId;
    private String titularNome;
    private String titularTipo; // ALUNO ou PROFESSOR
    private Integer saldoAtual;
    private List<TransacaoDTO> transacoes;

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getTitularNome() { return titularNome; }
    public void setTitularNome(String titularNome) { this.titularNome = titularNome; }

    public String getTitularTipo() { return titularTipo; }
    public void setTitularTipo(String titularTipo) { this.titularTipo = titularTipo; }

    public Integer getSaldoAtual() { return saldoAtual; }
    public void setSaldoAtual(Integer saldoAtual) { this.saldoAtual = saldoAtual; }

    public List<TransacaoDTO> getTransacoes() { return transacoes; }
    public void setTransacoes(List<TransacaoDTO> transacoes) { this.transacoes = transacoes; }

    public static class TransacaoDTO {
        private Long id;
        private Date dataHora;
        private Integer quantidade;
        private String movimentacao; // ENTRADA ou SAIDA
        private String categoria;    // ENVIO, RESGATE ou TRANSFERENCIA
        private String descricao;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Date getDataHora() { return dataHora; }
        public void setDataHora(Date dataHora) { this.dataHora = dataHora; }

        public Integer getQuantidade() { return quantidade; }
        public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

        public String getMovimentacao() { return movimentacao; }
        public void setMovimentacao(String movimentacao) { this.movimentacao = movimentacao; }

        public String getCategoria() { return categoria; }
        public void setCategoria(String categoria) { this.categoria = categoria; }

        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }
    }
}
