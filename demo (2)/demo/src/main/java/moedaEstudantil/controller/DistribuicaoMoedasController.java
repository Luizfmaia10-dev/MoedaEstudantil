package moedaEstudantil.controller;

import moedaEstudantil.dto.DistribuicaoContextoDTO;
import moedaEstudantil.dto.DistribuicaoMoedasRequest;
import moedaEstudantil.dto.DistribuicaoMoedasResultadoDTO;
import moedaEstudantil.service.DistribuicaoMoedasService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/professores/{professorId}")
@CrossOrigin(origins = "*")
public class DistribuicaoMoedasController {

    private final DistribuicaoMoedasService service;

    public DistribuicaoMoedasController(DistribuicaoMoedasService service) {
        this.service = service;
    }

    @GetMapping("/distribuicao")
    public DistribuicaoContextoDTO contexto(@PathVariable Long professorId) {
        return service.contexto(professorId);
    }

    @PostMapping("/envios")
    public ResponseEntity<DistribuicaoMoedasResultadoDTO> distribuir(@PathVariable Long professorId,
                                                                      @RequestBody DistribuicaoMoedasRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.distribuir(professorId, request));
    }
}
