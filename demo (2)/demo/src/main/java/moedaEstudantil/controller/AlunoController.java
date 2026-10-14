package moedaEstudantil.controller;

import moedaEstudantil.dto.AlunoDTO;
import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.Conta;
import moedaEstudantil.model.Instituicao;
import moedaEstudantil.repository.AlunoRepository;
import moedaEstudantil.repository.InstituicaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AlunoController {

    private final AlunoRepository alunoRepository;
    private final InstituicaoRepository instituicaoRepository;

    public AlunoController(AlunoRepository alunoRepository, InstituicaoRepository instituicaoRepository) {
        this.alunoRepository = alunoRepository;
        this.instituicaoRepository = instituicaoRepository;
    }

    @GetMapping("/alunos")
    public ResponseEntity<List<AlunoDTO>> listarTodos() {
        List<AlunoDTO> list = alunoRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/alunos/{id}")
    public ResponseEntity<AlunoDTO> buscarPorId(@PathVariable Long id) {
        return alunoRepository.findById(id)
                .map(aluno -> ResponseEntity.ok(toDTO(aluno)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/alunos")
    public ResponseEntity<AlunoDTO> criar(@RequestBody AlunoDTO dto) {
        Aluno aluno = new Aluno();
        aluno.setNome(dto.getNome());
        aluno.setEmail(dto.getEmail());
        aluno.setSenha(dto.getSenha());
        aluno.setCpf(dto.getCpf());
        aluno.setRg(dto.getRg());
        aluno.setCurso(dto.getCurso());
        aluno.setEndereco(dto.getEndereco());

        if (dto.getInstituicaoId() != null) {
            Optional<Instituicao> inst = instituicaoRepository.findById(dto.getInstituicaoId());
            inst.ifPresent(aluno::setInstituicao);
        }

        Conta conta = new Conta();
        conta.setSaldoCorrente(dto.getSaldoMoedas() != null ? dto.getSaldoMoedas() : 0);
        conta.setUsuario(aluno);
        aluno.setConta(conta);

        Aluno saved = alunoRepository.save(aluno);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
    }

    @PutMapping("/alunos/{id}")
    public ResponseEntity<AlunoDTO> atualizar(@PathVariable Long id, @RequestBody AlunoDTO dto) {
        return alunoRepository.findById(id).map(aluno -> {
            aluno.setNome(dto.getNome());
            aluno.setEmail(dto.getEmail());
            if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
                aluno.setSenha(dto.getSenha());
            }
            aluno.setCpf(dto.getCpf());
            aluno.setRg(dto.getRg());
            aluno.setCurso(dto.getCurso());
            aluno.setEndereco(dto.getEndereco());

            if (dto.getInstituicaoId() != null) {
                Optional<Instituicao> inst = instituicaoRepository.findById(dto.getInstituicaoId());
                inst.ifPresent(aluno::setInstituicao);
            }

            if (aluno.getConta() != null && dto.getSaldoMoedas() != null) {
                aluno.getConta().setSaldoCorrente(dto.getSaldoMoedas());
            } else if (dto.getSaldoMoedas() != null) {
                Conta conta = new Conta();
                conta.setSaldoCorrente(dto.getSaldoMoedas());
                conta.setUsuario(aluno);
                aluno.setConta(conta);
            }

            Aluno updated = alunoRepository.save(aluno);
            return ResponseEntity.ok(toDTO(updated));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/alunos/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        if (alunoRepository.existsById(id)) {
            alunoRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/instituicoes")
    public ResponseEntity<List<Instituicao>> listarInstituicoes() {
        return ResponseEntity.ok(instituicaoRepository.findAll());
    }

    private AlunoDTO toDTO(Aluno a) {
        AlunoDTO dto = new AlunoDTO();
        dto.setId(a.getId());
        dto.setNome(a.getNome());
        dto.setEmail(a.getEmail());
        dto.setCpf(a.getCpf());
        dto.setRg(a.getRg());
        dto.setCurso(a.getCurso());
        dto.setEndereco(a.getEndereco());
        if (a.getInstituicao() != null) {
            dto.setInstituicaoId(a.getInstituicao().getId());
            dto.setInstituicaoNome(a.getInstituicao().getNome());
        }
        if (a.getConta() != null) {
            dto.setSaldoMoedas(a.getConta().getSaldoCorrente());
        } else {
            dto.setSaldoMoedas(0);
        }
        return dto;
    }
}
