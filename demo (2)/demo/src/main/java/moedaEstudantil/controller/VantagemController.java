package moedaEstudantil.controller;

import moedaEstudantil.dto.VantagemDTO;
import moedaEstudantil.service.VantagemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/empresas/{empresaId}/vantagens")
@CrossOrigin(origins = "*")
public class VantagemController {

    private final VantagemService vantagemService;

    public VantagemController(VantagemService vantagemService) {
        this.vantagemService = vantagemService;
    }

    @GetMapping
    public List<VantagemDTO> listar(@PathVariable Long empresaId) {
        return vantagemService.listar(empresaId);
    }

    @GetMapping("/{id}")
    public VantagemDTO buscar(@PathVariable Long empresaId, @PathVariable Long id) {
        return vantagemService.buscar(empresaId, id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VantagemDTO> criar(@PathVariable Long empresaId,
                                             @RequestParam String nome,
                                             @RequestParam String descricao,
                                             @RequestParam Integer custoMoedas,
                                             @RequestParam(required = false) MultipartFile foto) {
        VantagemDTO criada = vantagemService.criar(empresaId, nome, descricao, custoMoedas, foto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VantagemDTO atualizar(@PathVariable Long empresaId,
                                 @PathVariable Long id,
                                 @RequestParam String nome,
                                 @RequestParam String descricao,
                                 @RequestParam Integer custoMoedas,
                                 @RequestParam(required = false) MultipartFile foto) {
        return vantagemService.atualizar(empresaId, id, nome, descricao, custoMoedas, foto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long empresaId, @PathVariable Long id) {
        vantagemService.remover(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}
