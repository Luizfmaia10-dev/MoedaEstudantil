package moedaEstudantil.service;

import moedaEstudantil.dto.AlunoDistribuicaoDTO;
import moedaEstudantil.dto.DistribuicaoContextoDTO;
import moedaEstudantil.dto.DistribuicaoMoedasRequest;
import moedaEstudantil.dto.DistribuicaoMoedasResultadoDTO;
import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.Conta;
import moedaEstudantil.model.Envio;
import moedaEstudantil.model.Professor;
import moedaEstudantil.model.TipoTransacao;
import moedaEstudantil.repository.AlunoRepository;
import moedaEstudantil.repository.ContaRepository;
import moedaEstudantil.repository.ProfessorRepository;
import moedaEstudantil.repository.TransacaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;

@Service
public class DistribuicaoMoedasService {

    private final ProfessorRepository professorRepository;
    private final AlunoRepository alunoRepository;
    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;
    private final NotificacaoService notificacaoService;

    public DistribuicaoMoedasService(ProfessorRepository professorRepository,
                                     AlunoRepository alunoRepository,
                                     ContaRepository contaRepository,
                                     TransacaoRepository transacaoRepository,
                                     NotificacaoService notificacaoService) {
        this.professorRepository = professorRepository;
        this.alunoRepository = alunoRepository;
        this.contaRepository = contaRepository;
        this.transacaoRepository = transacaoRepository;
        this.notificacaoService = notificacaoService;
    }

    @Transactional(readOnly = true)
    public DistribuicaoContextoDTO contexto(Long professorId) {
        Professor professor = buscarProfessor(professorId);
        Conta conta = contaRepository.findByUsuarioId(professorId)
                .orElseThrow(() -> erro("O professor nao possui conta."));
        Long instituicaoId = professor.getInstituicao() == null ? null : professor.getInstituicao().getId();
        var alunos = instituicaoId == null ? java.util.List.<AlunoDistribuicaoDTO>of()
                : alunoRepository.findByInstituicaoIdOrderByNome(instituicaoId).stream()
                .map(a -> new AlunoDistribuicaoDTO(a.getId(), a.getNome(), a.getCurso())).toList();
        return new DistribuicaoContextoDTO(professorId, professor.getNome(), saldo(conta), alunos);
    }

    @Transactional
    public DistribuicaoMoedasResultadoDTO distribuir(Long professorId, DistribuicaoMoedasRequest request) {
        Professor professor = buscarProfessor(professorId);
        if (request == null || request.alunoId() == null) throw erro("Informe o aluno destinatario.");
        if (request.quantidade() == null || request.quantidade() <= 0) throw erro("A quantidade deve ser maior que zero.");
        String motivo = request.motivo() == null ? "" : request.motivo().trim();
        if (motivo.isEmpty()) throw erro("O motivo e obrigatorio.");
        if (motivo.length() > 500) throw erro("O motivo deve ter no maximo 500 caracteres.");

        Aluno aluno = alunoRepository.findById(request.alunoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno nao encontrado."));
        Long instituicaoProfessor = professor.getInstituicao() == null ? null : professor.getInstituicao().getId();
        Long instituicaoAluno = aluno.getInstituicao() == null ? null : aluno.getInstituicao().getId();
        if (instituicaoProfessor == null || !instituicaoProfessor.equals(instituicaoAluno)) {
            throw erro("O aluno deve pertencer a mesma instituicao do professor.");
        }

        Long primeiroId = Math.min(professorId, aluno.getId());
        Long segundoId = Math.max(professorId, aluno.getId());
        Conta primeira = contaRepository.findByUsuarioIdParaAtualizacao(primeiroId)
                .orElseThrow(() -> erro("Conta nao encontrada."));
        Conta segunda = contaRepository.findByUsuarioIdParaAtualizacao(segundoId)
                .orElseThrow(() -> erro("Conta nao encontrada."));
        Conta contaProfessor = primeiroId.equals(professorId) ? primeira : segunda;
        Conta contaAluno = primeiroId.equals(aluno.getId()) ? primeira : segunda;

        int quantidade = request.quantidade();
        if (saldo(contaProfessor) < quantidade) throw erro("Saldo insuficiente para realizar o envio.");
        contaProfessor.setSaldoCorrente(saldo(contaProfessor) - quantidade);
        contaAluno.setSaldoCorrente(saldo(contaAluno) + quantidade);
        contaRepository.save(contaProfessor);
        contaRepository.save(contaAluno);

        Date agora = new Date();
        transacaoRepository.save(novoEnvio(contaProfessor, professor, aluno, quantidade, motivo, TipoTransacao.SAIDA, agora));
        transacaoRepository.save(novoEnvio(contaAluno, professor, aluno, quantidade, motivo, TipoTransacao.ENTRADA, agora));
        notificacaoService.notificarRecebimento(aluno, professor, quantidade, motivo);
        return new DistribuicaoMoedasResultadoDTO(saldo(contaProfessor), saldo(contaAluno));
    }

    private Envio novoEnvio(Conta conta, Professor professor, Aluno aluno, int quantidade,
                            String motivo, TipoTransacao tipo, Date data) {
        Envio envio = new Envio();
        envio.setConta(conta);
        envio.setProfessor(professor);
        envio.setAluno(aluno);
        envio.setQuantidade(quantidade);
        envio.setMotivo(motivo);
        envio.setTipoMovimentacao(tipo);
        envio.setDataHora(data);
        return envio;
    }

    private Professor buscarProfessor(Long id) {
        return professorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor nao encontrado."));
    }

    private int saldo(Conta conta) {
        return conta.getSaldoCorrente() == null ? 0 : conta.getSaldoCorrente();
    }

    private ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
