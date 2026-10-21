package moedaEstudantil.controller;

import moedaEstudantil.dto.LoginRequest;
import moedaEstudantil.dto.UsuarioAutenticadoDTO;
import moedaEstudantil.service.AutenticacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
