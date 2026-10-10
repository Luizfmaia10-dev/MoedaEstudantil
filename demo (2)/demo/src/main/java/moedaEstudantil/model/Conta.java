package moedaEstudantil.model;

import jakarta.persistence.*;

@Entity
public class Conta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Integer saldoCorrente;

    @OneToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getSaldoCorrente() { return saldoCorrente; }
    public void setSaldoCorrente(Integer saldoCorrente) { this.saldoCorrente = saldoCorrente; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
