package moedaEstudantil.controller;

import moedaEstudantil.dto.ExtratoDTO;
import moedaEstudantil.service.ExtratoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contas")
@CrossOrigin(origins = "*")
public class ExtratoController {

    private final ExtratoService extratoService;

    public ExtratoController(ExtratoService extratoService) {
        this.extratoService = extratoService;
    }

    @GetMapping("/{usuarioId}/extrato")
    public ResponseEntity<ExtratoDTO> extrato(@PathVariable Long usuarioId,
                                              @RequestParam(required = false) String categoria) {
        return extratoService.gerarExtrato(usuarioId, categoria)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
