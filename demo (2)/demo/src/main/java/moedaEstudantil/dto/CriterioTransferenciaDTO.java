package moedaEstudantil.dto;

public class CriterioTransferenciaDTO {
    private Long professorId;
    private String professorNome;
    private String departamento;
    private String criterios;

    public Long getProfessorId() { return professorId; }
    public void setProfessorId(Long professorId) { this.professorId = professorId; }

    public String getProfessorNome() { return professorNome; }
    public void setProfessorNome(String professorNome) { this.professorNome = professorNome; }

    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }

    public String getCriterios() { return criterios; }
    public void setCriterios(String criterios) { this.criterios = criterios; }
}
