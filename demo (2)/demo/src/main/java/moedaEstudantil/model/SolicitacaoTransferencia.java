package moedaEstudantil.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;

@Entity
@Table(name = "solicitacao_transferencia")
public class SolicitacaoTransferencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Date dataSolicitacao;
    private Integer quantidade;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private StatusSolicitacaoTransferencia status;

    // Justificativa opcional do aluno para o professor avaliador
    @Column(length = 300)
    private String justificativa;

    // Explicacao opcional do professor ao aprovar ou recusar (RF-013)
    @Column(length = 300)
    private String observacaoAvaliador;

    private Date dataAvaliacao;
    private Boolean avaliacaoAutomatica = false;
    private Double confiancaAvaliacaoAutomatica;

    @ManyToOne
    @JoinColumn(name = "remetente_id")
    private Aluno remetente;

    @ManyToOne
    @JoinColumn(name = "destinatario_id")
    private Aluno destinatario;

    @ManyToOne
    @JoinColumn(name = "avaliador_id")
    private Professor avaliador;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Date getDataSolicitacao() { return dataSolicitacao; }
    public void setDataSolicitacao(Date dataSolicitacao) { this.dataSolicitacao = dataSolicitacao; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public StatusSolicitacaoTransferencia getStatus() { return status; }
    public void setStatus(StatusSolicitacaoTransferencia status) { this.status = status; }
    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }
    public String getObservacaoAvaliador() { return observacaoAvaliador; }
    public void setObservacaoAvaliador(String observacaoAvaliador) { this.observacaoAvaliador = observacaoAvaliador; }
    public Date getDataAvaliacao() { return dataAvaliacao; }
    public void setDataAvaliacao(Date dataAvaliacao) { this.dataAvaliacao = dataAvaliacao; }
    public boolean isAvaliacaoAutomatica() { return Boolean.TRUE.equals(avaliacaoAutomatica); }
    public void setAvaliacaoAutomatica(boolean avaliacaoAutomatica) { this.avaliacaoAutomatica = avaliacaoAutomatica; }
    public Double getConfiancaAvaliacaoAutomatica() { return confiancaAvaliacaoAutomatica; }
    public void setConfiancaAvaliacaoAutomatica(Double confianca) { this.confiancaAvaliacaoAutomatica = confianca; }
    public Aluno getRemetente() { return remetente; }
    public void setRemetente(Aluno remetente) { this.remetente = remetente; }
    public Aluno getDestinatario() { return destinatario; }
    public void setDestinatario(Aluno destinatario) { this.destinatario = destinatario; }
    public Professor getAvaliador() { return avaliador; }
    public void setAvaliador(Professor avaliador) { this.avaliador = avaliador; }
}
