package moedaEstudantil.dto;

public class AlunoDTO {
    private Long id;
    private String nome;
    private String email;
    private String senha;
    private String cpf;
    private String rg;
    private String curso;
    private Long instituicaoId;
    private String instituicaoNome;
    private String endereco;
    private Integer saldoMoedas;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getRg() { return rg; }
    public void setRg(String rg) { this.rg = rg; }

    public String getCurso() { return curso; }
    public void setCurso(String curso) { this.curso = curso; }

    public Long getInstituicaoId() { return instituicaoId; }
    public void setInstituicaoId(Long instituicaoId) { this.instituicaoId = instituicaoId; }

    public String getInstituicaoNome() { return instituicaoNome; }
    public void setInstituicaoNome(String instituicaoNome) { this.instituicaoNome = instituicaoNome; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public Integer getSaldoMoedas() { return saldoMoedas; }
    public void setSaldoMoedas(Integer saldoMoedas) { this.saldoMoedas = saldoMoedas; }
}
