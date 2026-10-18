package moedaEstudantil.model;

import jakarta.persistence.*;

@Entity
@Table(name = "vantagem")
public class Vantagem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String descricao;
    private String fotoUrl;
    private Integer custoMoedas;

    // Exclusao logica: preserva o historico de resgates que referenciam a vantagem
    private Boolean ativa = true;

    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private EmpresaParceira empresa;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public Integer getCustoMoedas() { return custoMoedas; }
    public void setCustoMoedas(Integer custoMoedas) { this.custoMoedas = custoMoedas; }
    public Boolean getAtiva() { return ativa == null || ativa; }
    public void setAtiva(Boolean ativa) { this.ativa = ativa; }
    public EmpresaParceira getEmpresa() { return empresa; }
    public void setEmpresa(EmpresaParceira empresa) { this.empresa = empresa; }
}
