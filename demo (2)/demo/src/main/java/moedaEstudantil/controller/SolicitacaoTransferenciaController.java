package moedaEstudantil.controller;

import moedaEstudantil.dto.ColegaDTO;
import moedaEstudantil.dto.SaldoDisponivelDTO;
import moedaEstudantil.dto.SolicitacaoTransferenciaDTO;
import moedaEstudantil.service.SolicitacaoTransferenciaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SolicitacaoTransferenciaController {

    public record NovaSolicitacaoRequest(
            Long destinatarioId,
            Long avaliadorId,
            Integer quantidade,
            String justificativa) {
    }

    public record AvaliacaoRequest(String observacao) {
    }

    private final SolicitacaoTransferenciaService service;

    public SolicitacaoTransferenciaController(SolicitacaoTransferenciaService service) {
        this.service = service;
    }

    /* ---------- Endpoints do Aluno (RF-012) ---------- */

    @GetMapping("/alunos/{alunoId}/colegas")
    public List<ColegaDTO> listarColegas(@PathVariable Long alunoId) {
        return service.listarColegas(alunoId);
    }

    @GetMapping("/alunos/{alunoId}/saldo-disponivel")
    public SaldoDisponivelDTO saldoDisponivel(@PathVariable Long alunoId) {
        return service.saldoDisponivel(alunoId);
    }

    @GetMapping("/alunos/{alunoId}/transferencias")
    public List<SolicitacaoTransferenciaDTO> listar(@PathVariable Long alunoId) {
        return service.listar(alunoId);
    }

    @PostMapping("/alunos/{alunoId}/transferencias")
    public ResponseEntity<SolicitacaoTransferenciaDTO> criar(@PathVariable Long alunoId,
                                                             @RequestBody NovaSolicitacaoRequest body) {
        SolicitacaoTransferenciaDTO criada = service.criar(
                alunoId,
                body.destinatarioId(),
                body.avaliadorId(),
                body.quantidade(),
                body.justificativa());
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @DeleteMapping("/alunos/{alunoId}/transferencias/{solicitacaoId}")
    public SolicitacaoTransferenciaDTO cancelar(@PathVariable Long alunoId,
                                                @PathVariable Long solicitacaoId) {
        return service.cancelar(alunoId, solicitacaoId);
    }

    /* ---------- Endpoints do Professor (RF-013) ---------- */

    @GetMapping("/professores/{professorId}/transferencias")
    public List<SolicitacaoTransferenciaDTO> listarParaProfessor(
            @PathVariable Long professorId,
            @RequestParam(required = false) String status) {
        return service.listarParaProfessor(professorId, status);
    }

    @PostMapping("/professores/{professorId}/transferencias/{solicitacaoId}/aprovar")
    public SolicitacaoTransferenciaDTO aprovar(@PathVariable Long professorId,
                                               @PathVariable Long solicitacaoId,
                                               @RequestBody(required = false) AvaliacaoRequest body) {
        return service.aprovar(professorId, solicitacaoId, body == null ? null : body.observacao());
    }

    @PostMapping("/professores/{professorId}/transferencias/{solicitacaoId}/recusar")
    public SolicitacaoTransferenciaDTO recusar(@PathVariable Long professorId,
                                               @PathVariable Long solicitacaoId,
                                               @RequestBody(required = false) AvaliacaoRequest body) {
        return service.recusar(professorId, solicitacaoId, body == null ? null : body.observacao());
    }
}
