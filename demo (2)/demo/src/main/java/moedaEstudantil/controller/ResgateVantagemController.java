package moedaEstudantil.controller;

import moedaEstudantil.dto.CatalogoVantagemDTO;
import moedaEstudantil.dto.ResgateResultadoDTO;
import moedaEstudantil.service.ResgateVantagemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alunos/{alunoId}")
@CrossOrigin(origins = "*")
public class ResgateVantagemController {

    private final ResgateVantagemService service;

    public ResgateVantagemController(ResgateVantagemService service) {
        this.service = service;
    }

    @GetMapping("/vantagens")
    public CatalogoVantagemDTO catalogo(@PathVariable Long alunoId) {
        return service.catalogo(alunoId);
    }

    @PostMapping("/resgates/{vantagemId}")
    public ResponseEntity<ResgateResultadoDTO> resgatar(@PathVariable Long alunoId,
                                                        @PathVariable Long vantagemId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.resgatar(alunoId, vantagemId));
    }
}
