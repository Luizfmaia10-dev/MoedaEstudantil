package moedaEstudantil.controller;

import moedaEstudantil.dto.EmpresaParceiraDTO;
import moedaEstudantil.model.EmpresaParceira;
import moedaEstudantil.repository.EmpresaParceiraRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/empresas")
@CrossOrigin(origins = "*")
public class EmpresaParceiraController {

    private final EmpresaParceiraRepository empresaRepository;

    public EmpresaParceiraController(EmpresaParceiraRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    @GetMapping
    public ResponseEntity<List<EmpresaParceiraDTO>> listarTodas() {
        List<EmpresaParceiraDTO> lista = empresaRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpresaParceiraDTO> buscarPorId(@PathVariable Long id) {
        return empresaRepository.findById(id)
                .map(e -> ResponseEntity.ok(toDTO(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EmpresaParceiraDTO> cadastrar(@RequestBody EmpresaParceiraDTO dto) {
        EmpresaParceira empresa = new EmpresaParceira();
        empresa.setNome(dto.getNome());
        empresa.setCnpj(dto.getCnpj());
        empresa.setEmail(dto.getEmail());
        empresa.setSenha(dto.getSenha());

        EmpresaParceira salva = empresaRepository.save(empresa);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(salva));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmpresaParceiraDTO> atualizar(@PathVariable Long id, @RequestBody EmpresaParceiraDTO dto) {
        return empresaRepository.findById(id).map(empresa -> {
            empresa.setNome(dto.getNome());
            empresa.setCnpj(dto.getCnpj());
            empresa.setEmail(dto.getEmail());
            if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
                empresa.setSenha(dto.getSenha());
            }

            EmpresaParceira atualizada = empresaRepository.save(empresa);
            return ResponseEntity.ok(toDTO(atualizada));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        if (empresaRepository.existsById(id)) {
            empresaRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private EmpresaParceiraDTO toDTO(EmpresaParceira e) {
        EmpresaParceiraDTO dto = new EmpresaParceiraDTO();
        dto.setId(e.getId());
        dto.setNome(e.getNome());
        dto.setCnpj(e.getCnpj());
        dto.setEmail(e.getEmail());
        return dto;
    }
}
