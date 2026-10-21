package moedaEstudantil.controller;

import moedaEstudantil.dto.LoginRequest;
import moedaEstudantil.dto.UsuarioAutenticadoDTO;
import moedaEstudantil.service.AutenticacaoService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AutenticacaoController {
    private final AutenticacaoService service;

    public AutenticacaoController(AutenticacaoService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ResponseEntity<UsuarioAutenticadoDTO> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(service.autenticar(request.email(), request.senha(), request.perfil()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> erroDeAutenticacao(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode()).body(Map.of("message", erro.getReason()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> persistenciaIndisponivel(DataAccessException erro) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Serviço temporariamente indisponível."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> corpoInvalido(HttpMessageNotReadableException erro) {
        return ResponseEntity.badRequest().body(Map.of("message", "Informe e-mail, senha e perfil."));
    }
}
