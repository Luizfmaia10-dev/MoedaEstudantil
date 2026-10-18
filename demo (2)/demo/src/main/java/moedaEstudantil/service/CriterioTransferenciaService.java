package moedaEstudantil.service;

import moedaEstudantil.dto.CriterioTransferenciaDTO;
import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.Professor;
import moedaEstudantil.repository.AlunoRepository;
import moedaEstudantil.repository.ProfessorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Criterios de transferencia sao um texto informativo do professor, consultado
 * pelo aluno antes de solicitar uma transferencia. Nao ha regra automatica.
 */
@Service
public class CriterioTransferenciaService {

    public static final int TAMANHO_MAXIMO = 1000;

    private final ProfessorRepository professorRepository;
    private final AlunoRepository alunoRepository;

    public CriterioTransferenciaService(ProfessorRepository professorRepository, AlunoRepository alunoRepository) {
        this.professorRepository = professorRepository;
        this.alunoRepository = alunoRepository;
    }

    @Transactional(readOnly = true)
    public CriterioTransferenciaDTO buscar(Long professorId) {
        return toDTO(buscarProfessor(professorId));
    }

    /** Salva o texto. Texto vazio e permitido e limpa o criterio. */
    @Transactional
    public CriterioTransferenciaDTO salvar(Long professorId, String texto) {
        String limpo = texto == null ? "" : texto.trim();
        if (limpo.length() > TAMANHO_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Os criterios devem ter no maximo " + TAMANHO_MAXIMO + " caracteres.");
        }
        Professor p = buscarProfessor(professorId);
        p.setCriteriosTransferencia(limpo.isEmpty() ? null : limpo);
        return toDTO(professorRepository.save(p));
    }

    /** Criterios dos professores da mesma instituicao do aluno (somente quem preencheu). */
    @Transactional(readOnly = true)
    public List<CriterioTransferenciaDTO> listarParaAluno(Long alunoId) {
        Aluno aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno nao encontrado."));
        if (aluno.getInstituicao() == null) {
            return List.of();
        }
        return professorRepository.findComCriteriosByInstituicaoId(aluno.getInstituicao().getId())
                .stream().map(this::toDTO).toList();
    }

    private Professor buscarProfessor(Long id) {
        return professorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor nao encontrado."));
    }

    private CriterioTransferenciaDTO toDTO(Professor p) {
        CriterioTransferenciaDTO dto = new CriterioTransferenciaDTO();
        dto.setProfessorId(p.getId());
        dto.setProfessorNome(p.getNome());
        dto.setDepartamento(p.getDepartamento());
        dto.setCriterios(p.getCriteriosTransferencia());
        return dto;
    }
}
