package moedaEstudantil.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "transacao")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_transacao")
public abstract class TransacaoGeral {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Date dataHora;
    private Integer quantidade;

    @Enumerated(EnumType.STRING)
    private TipoTransacao tipoMovimentacao;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conta_id", nullable = false)
    private Conta conta;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Date getDataHora() { return dataHora; }
    public void setDataHora(Date dataHora) { this.dataHora = dataHora; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public TipoTransacao getTipoMovimentacao() { return tipoMovimentacao; }
    public void setTipoMovimentacao(TipoTransacao tipoMovimentacao) { this.tipoMovimentacao = tipoMovimentacao; }
    public Conta getConta() { return conta; }
    public void setConta(Conta conta) { this.conta = conta; }
}
