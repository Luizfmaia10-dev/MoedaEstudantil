package moedaEstudantil.controller;

import moedaEstudantil.dto.CriterioTransferenciaDTO;
import moedaEstudantil.service.CriterioTransferenciaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CriterioTransferenciaController {

    /** Corpo do PUT: {"criterios": "texto"} */
    public record CriterioRequest(String criterios) {}

    private final CriterioTransferenciaService service;

    public CriterioTransferenciaController(CriterioTransferenciaService service) {
        this.service = service;
    }

    @GetMapping("/professores/{professorId}/criterios")
    public CriterioTransferenciaDTO buscar(@PathVariable Long professorId) {
        return service.buscar(professorId);
    }

    @PutMapping("/professores/{professorId}/criterios")
    public CriterioTransferenciaDTO salvar(@PathVariable Long professorId, @RequestBody CriterioRequest body) {
        return service.salvar(professorId, body.criterios());
    }

    /** Somente leitura: criterios dos professores da instituicao do aluno. */
    @GetMapping("/alunos/{alunoId}/criterios-professores")
    public List<CriterioTransferenciaDTO> listarParaAluno(@PathVariable Long alunoId) {
        return service.listarParaAluno(alunoId);
    }
}
