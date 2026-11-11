package moedaEstudantil.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "credito_semestral_professor", uniqueConstraints = @UniqueConstraint(
        name = "uk_credito_professor_periodo", columnNames = {"professor_id", "ano", "semestre"}))
public class CreditoSemestralProfessor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    private int ano;
    private int semestre;
    private int quantidade;
    private LocalDateTime dataCredito;

    public Long getId() { return id; }
    public Professor getProfessor() { return professor; }
    public void setProfessor(Professor professor) { this.professor = professor; }
    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }
    public int getSemestre() { return semestre; }
    public void setSemestre(int semestre) { this.semestre = semestre; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public LocalDateTime getDataCredito() { return dataCredito; }
    public void setDataCredito(LocalDateTime dataCredito) { this.dataCredito = dataCredito; }
}
