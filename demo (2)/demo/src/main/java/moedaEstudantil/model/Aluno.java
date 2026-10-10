package moedaEstudantil.model;

import jakarta.persistence.*;

@Entity
@Table(name = "aluno")
@DiscriminatorValue("ALUNO")
@PrimaryKeyJoinColumn(name="usuario_id")
public class Aluno extends Usuario {
    private String nome;
    private String cpf;
    private String rg;
    private String endereco;
    private String curso;

    @ManyToOne
    @JoinColumn(name = "instituicao_id")
    private Instituicao instituicao;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getRg() { return rg; }
    public void setRg(String rg) { this.rg = rg; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public String getCurso() { return curso; }
    public void setCurso(String curso) { this.curso = curso; }
    public Instituicao getInstituicao() { return instituicao; }
    public void setInstituicao(Instituicao instituicao) { this.instituicao = instituicao; }
}
