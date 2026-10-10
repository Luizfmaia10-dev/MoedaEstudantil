package moedaEstudantil.model;

import jakarta.persistence.*;

@Entity
@Table(name = "empresa_parceira")
@DiscriminatorValue("EMPRESA")
@PrimaryKeyJoinColumn(name="usuario_id")
public class EmpresaParceira extends Usuario {
    private String nome;
    private String cnpj;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
}
