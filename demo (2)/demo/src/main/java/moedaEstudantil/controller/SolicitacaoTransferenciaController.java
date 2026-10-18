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
@RequestMapping("/api/alunos/{alunoId}")
@CrossOrigin(origins = "*")
public class SolicitacaoTransferenciaController {

    public record NovaSolicitacaoRequest(
            Long destinatarioId,
            Long avaliadorId,
            Integer quantidade,
            String justificativa) {
    }

    private final SolicitacaoTransferenciaService service;

    public SolicitacaoTransferenciaController(SolicitacaoTransferenciaService service) {
        this.service = service;
    }

    @GetMapping("/colegas")
    public List<ColegaDTO> listarColegas(@PathVariable Long alunoId) {
        return service.listarColegas(alunoId);
    }

    @GetMapping("/saldo-disponivel")
    public SaldoDisponivelDTO saldoDisponivel(@PathVariable Long alunoId) {
        return service.saldoDisponivel(alunoId);
    }

    @GetMapping("/transferencias")
    public List<SolicitacaoTransferenciaDTO> listar(@PathVariable Long alunoId) {
        return service.listar(alunoId);
    }

    @PostMapping("/transferencias")
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

    @DeleteMapping("/transferencias/{solicitacaoId}")
    public SolicitacaoTransferenciaDTO cancelar(@PathVariable Long alunoId,
                                                @PathVariable Long solicitacaoId) {
        return service.cancelar(alunoId, solicitacaoId);
    }
}
