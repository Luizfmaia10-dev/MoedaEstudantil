package moedaEstudantil.model;

import jakarta.persistence.*;

@Entity
@Table(name = "professor")
@DiscriminatorValue("PROFESSOR")
@PrimaryKeyJoinColumn(name="usuario_id")
public class Professor extends Usuario {
    private String nome;
    private String cpf;
    private String departamento;
    private String criteriosTransferencia;

    @ManyToOne
    @JoinColumn(name = "instituicao_id")
    private Instituicao instituicao;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
    public String getCriteriosTransferencia() { return criteriosTransferencia; }
    public void setCriteriosTransferencia(String criteriosTransferencia) { this.criteriosTransferencia = criteriosTransferencia; }
    public Instituicao getInstituicao() { return instituicao; }
    public void setInstituicao(Instituicao instituicao) { this.instituicao = instituicao; }
}
