package moedaEstudantil.controller;

import moedaEstudantil.dto.AssistenteTransferenciaDTO;
import moedaEstudantil.service.AssistenteTransferenciaService;
import moedaEstudantil.service.AssistenteTransferenciaService.RascunhoAssistente;
import moedaEstudantil.service.IaLocalIndisponivelException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/alunos/{alunoId}/transferencias/assistente")
@CrossOrigin(origins = "*")
public class AssistenteTransferenciaController {

    public record RascunhoRequest(
            Long destinatarioId,
            String destinatarioNome,
            Long avaliadorId,
            String avaliadorNome,
            Integer quantidade,
            String justificativa) {
    }

    public record InterpretarRequest(String mensagem, RascunhoRequest rascunho) {
    }

    private final AssistenteTransferenciaService service;

    public AssistenteTransferenciaController(AssistenteTransferenciaService service) {
        this.service = service;
    }

    @PostMapping("/interpretar")
    public AssistenteTransferenciaDTO interpretar(@PathVariable Long alunoId,
                                                  @RequestBody InterpretarRequest body) {
        if (body == null || body.mensagem() == null || body.mensagem().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Escreva como deseja transferir as moedas.");
        }
        RascunhoRequest recebido = body.rascunho();
        RascunhoAssistente rascunho = recebido == null
                ? RascunhoAssistente.vazio()
                : new RascunhoAssistente(recebido.destinatarioId(), recebido.destinatarioNome(),
                recebido.avaliadorId(), recebido.avaliadorNome(), recebido.quantidade(), recebido.justificativa());
        return service.interpretar(alunoId, body.mensagem().trim(), rascunho);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> erroDeValidacao(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode()).body(Map.of("message", erro.getReason()));
    }

    @ExceptionHandler(IaLocalIndisponivelException.class)
    public ResponseEntity<Map<String, String>> iaIndisponivel(IaLocalIndisponivelException erro) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "message", "A IA local está indisponível. Você ainda pode usar o formulário manual."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> corpoInvalido(HttpMessageNotReadableException erro) {
        return ResponseEntity.badRequest().body(Map.of(
                "message", "Escreva como deseja transferir as moedas."));
    }
}
