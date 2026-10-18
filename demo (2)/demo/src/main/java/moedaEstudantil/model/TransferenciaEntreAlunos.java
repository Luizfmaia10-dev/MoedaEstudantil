package moedaEstudantil.model;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("TRANSFERENCIA")
public class TransferenciaEntreAlunos extends TransacaoGeral {
    @ManyToOne
    @JoinColumn(name = "solicitacao_id")
    private SolicitacaoTransferencia solicitacao;

    public SolicitacaoTransferencia getSolicitacao() { return solicitacao; }
    public void setSolicitacao(SolicitacaoTransferencia solicitacao) { this.solicitacao = solicitacao; }
}
