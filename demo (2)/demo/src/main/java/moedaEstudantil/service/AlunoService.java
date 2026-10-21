package moedaEstudantil.service;

import moedaEstudantil.dto.AlunoDTO;
import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.Conta;
import moedaEstudantil.model.Instituicao;
import moedaEstudantil.repository.AlunoRepository;
import moedaEstudantil.repository.InstituicaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class AlunoService {
    private final AlunoRepository alunos;
    private final InstituicaoRepository instituicoes;
    private final SenhaService senhas;

    public AlunoService(AlunoRepository alunos, InstituicaoRepository instituicoes, SenhaService senhas) {
        this.alunos = alunos;
        this.instituicoes = instituicoes;
        this.senhas = senhas;
    }

    public List<AlunoDTO> listar() {
        return alunos.findAll().stream().map(this::toDTO).toList();
    }

    public AlunoDTO buscar(Long id) {
        return toDTO(obter(id));
    }

    @Transactional
    public AlunoDTO criar(AlunoDTO dto) {
        Instituicao instituicao = obterInstituicao(dto.getInstituicaoId());
        String email = normalizarEmail(dto.getEmail());
        if (alunos.findByEmailIgnoreCase(email).isPresent()) conflito("E-mail já cadastrado.");
        if (alunos.findByCpf(dto.getCpf()).isPresent()) conflito("CPF já cadastrado.");

        Aluno aluno = new Aluno();
        copiarDados(dto, aluno, instituicao);
        aluno.setEmail(email);
        aluno.setSenha(senhas.codificar(dto.getSenha()));
        Conta conta = new Conta();
        conta.setSaldoCorrente(0);
        conta.setUsuario(aluno);
        aluno.setConta(conta);
        return toDTO(alunos.save(aluno));
    }

    @Transactional
    public AlunoDTO atualizar(Long id, AlunoDTO dto) {
        Aluno aluno = obter(id);
        Instituicao instituicao = obterInstituicao(dto.getInstituicaoId());
        String email = normalizarEmail(dto.getEmail());
        alunos.findByEmailIgnoreCase(email)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> conflito("E-mail já cadastrado."));
        alunos.findByCpf(dto.getCpf())
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> conflito("CPF já cadastrado."));

        copiarDados(dto, aluno, instituicao);
        aluno.setEmail(email);
        if (dto.getSenha() != null && !dto.getSenha().isBlank()) aluno.setSenha(senhas.codificar(dto.getSenha()));
        return toDTO(alunos.save(aluno));
    }

    public void excluir(Long id) {
        if (!alunos.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        alunos.deleteById(id);
    }

    private void copiarDados(AlunoDTO dto, Aluno aluno, Instituicao instituicao) {
        aluno.setNome(dto.getNome());
        aluno.setCpf(dto.getCpf());
        aluno.setRg(dto.getRg());
        aluno.setCurso(dto.getCurso());
        aluno.setEndereco(dto.getEndereco());
        aluno.setInstituicao(instituicao);
    }

    private Instituicao obterInstituicao(Long id) {
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Selecione uma instituição participante.");
        return instituicoes.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Instituição não participante."));
    }

    private Aluno obter(Long id) {
        return alunos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private String normalizarEmail(String email) {
        if (email == null || email.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail obrigatório.");
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void conflito(String mensagem) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }

    public AlunoDTO toDTO(Aluno aluno) {
        AlunoDTO dto = new AlunoDTO();
        dto.setId(aluno.getId());
        dto.setNome(aluno.getNome());
        dto.setEmail(aluno.getEmail());
        dto.setCpf(aluno.getCpf());
        dto.setRg(aluno.getRg());
        dto.setCurso(aluno.getCurso());
        dto.setEndereco(aluno.getEndereco());
        dto.setInstituicaoId(aluno.getInstituicao().getId());
        dto.setInstituicaoNome(aluno.getInstituicao().getNome());
        dto.setSaldoMoedas(aluno.getConta() == null ? 0 : aluno.getConta().getSaldoCorrente());
        return dto;
    }
}
