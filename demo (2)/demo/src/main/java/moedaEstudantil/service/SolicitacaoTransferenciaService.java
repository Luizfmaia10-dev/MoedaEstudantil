package moedaEstudantil.service;

import moedaEstudantil.dto.ColegaDTO;
import moedaEstudantil.dto.SaldoDisponivelDTO;
import moedaEstudantil.dto.SolicitacaoTransferenciaDTO;
import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.Conta;
import moedaEstudantil.model.Professor;
import moedaEstudantil.model.SolicitacaoTransferencia;
import moedaEstudantil.model.StatusSolicitacaoTransferencia;
import moedaEstudantil.repository.AlunoRepository;
import moedaEstudantil.repository.ContaRepository;
import moedaEstudantil.repository.ProfessorRepository;
import moedaEstudantil.repository.SolicitacaoTransferenciaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Solicitacoes de transferencia de moedas entre alunos (RF-012).
 * Criar uma solicitacao NAO movimenta moedas: a aprovacao pelo professor e outro requisito.
 * Para evitar solicitacoes que somadas excedam o saldo, o limite usado e o saldo
 * disponivel = saldo corrente - soma das solicitacoes pendentes do proprio aluno.
 */
@Service
public class SolicitacaoTransferenciaService {

    public static final int JUSTIFICATIVA_MAXIMA = 300;

    private final SolicitacaoTransferenciaRepository solicitacaoRepository;
    private final AlunoRepository alunoRepository;
    private final ProfessorRepository professorRepository;
    private final ContaRepository contaRepository;

    public SolicitacaoTransferenciaService(SolicitacaoTransferenciaRepository solicitacaoRepository,
                                           AlunoRepository alunoRepository,
                                           ProfessorRepository professorRepository,
                                           ContaRepository contaRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.alunoRepository = alunoRepository;
        this.professorRepository = professorRepository;
        this.contaRepository = contaRepository;
    }

    @Transactional(readOnly = true)
    public List<ColegaDTO> listarColegas(Long alunoId) {
        Aluno aluno = buscarAluno(alunoId);
        Long instituicaoId = aluno.getInstituicao() == null ? null : aluno.getInstituicao().getId();
        if (instituicaoId == null) return List.of();
        return alunoRepository.findByInstituicaoIdAndIdNotOrderByNome(instituicaoId, alunoId).stream()
                .map(a -> new ColegaDTO(a.getId(), a.getNome(), a.getCurso()))
                .toList();
    }

    @Transactional(readOnly = true)
    public SaldoDisponivelDTO saldoDisponivel(Long alunoId) {
        buscarAluno(alunoId);
        Conta conta = contaRepository.findByUsuarioId(alunoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "O aluno nao possui conta."));
        return calcularSaldo(alunoId, conta);
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoTransferenciaDTO> listar(Long alunoId) {
        buscarAluno(alunoId);
        return solicitacaoRepository.findByRemetenteIdOrderByDataSolicitacaoDescIdDesc(alunoId).stream()
                .map(this::toDTO).toList();
    }

    @Transactional
    public SolicitacaoTransferenciaDTO criar(Long remetenteId, Long destinatarioId, Long avaliadorId,
                                             Integer quantidade, String justificativa) {
        Aluno remetente = buscarAluno(remetenteId);

        // Validacoes que nao dependem do saldo
        if (quantidade == null || quantidade <= 0) {
            throw erro("A quantidade deve ser maior que zero.");
        }
        if (destinatarioId == null || avaliadorId == null) {
            throw erro("Informe o aluno destinatario e o professor avaliador.");
        }
        if (Objects.equals(remetenteId, destinatarioId)) {
            throw erro("Voce nao pode transferir moedas para si mesmo.");
        }
        Long instituicaoId = remetente.getInstituicao() == null ? null : remetente.getInstituicao().getId();
        if (instituicaoId == null) {
            throw erro("O aluno nao esta vinculado a uma instituicao.");
        }

        Aluno destinatario = alunoRepository.findById(destinatarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno destinatario nao encontrado."));
        if (destinatario.getInstituicao() == null || !instituicaoId.equals(destinatario.getInstituicao().getId())) {
            throw erro("O destinatario deve ser da mesma instituicao.");
        }

        Professor avaliador = professorRepository.findById(avaliadorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor avaliador nao encontrado."));
        if (avaliador.getInstituicao() == null || !instituicaoId.equals(avaliador.getInstituicao().getId())) {
            throw erro("O professor avaliador deve ser da mesma instituicao.");
        }

        String texto = justificativa == null ? "" : justificativa.trim();
        if (texto.length() > JUSTIFICATIVA_MAXIMA) {
            throw erro("A justificativa deve ter no maximo " + JUSTIFICATIVA_MAXIMA + " caracteres.");
        }

        // Bloqueio pessimista da conta do remetente: serializa solicitacoes concorrentes
        // do mesmo aluno, impedindo que a soma das pendentes ultrapasse o saldo.
        Conta conta = contaRepository.findByUsuarioIdParaAtualizacao(remetenteId)
                .orElseThrow(() -> erro("O aluno nao possui conta."));
        SaldoDisponivelDTO saldo = calcularSaldo(remetenteId, conta);
        if (quantidade > saldo.saldoDisponivel()) {
            throw erro("Saldo insuficiente. Disponivel para transferencia: " + saldo.saldoDisponivel() + " moedas.");
        }

        SolicitacaoTransferencia s = new SolicitacaoTransferencia();
        s.setRemetente(remetente);
        s.setDestinatario(destinatario);
        s.setAvaliador(avaliador);
        s.setQuantidade(quantidade);
        s.setJustificativa(texto.isEmpty() ? null : texto);
        s.setStatus(StatusSolicitacaoTransferencia.PENDENTE);
        s.setDataSolicitacao(new Date());
        return toDTO(solicitacaoRepository.save(s));
    }

    /** Cancela uma solicitacao pendente do proprio aluno; libera o saldo comprometido. */
    @Transactional
    public SolicitacaoTransferenciaDTO cancelar(Long alunoId, Long solicitacaoId) {
        // Mesmo bloqueio da criacao: evita corrida com outras operacoes sobre o saldo do aluno
        contaRepository.findByUsuarioIdParaAtualizacao(alunoId);

        SolicitacaoTransferencia s = solicitacaoRepository.findById(solicitacaoId)
                .filter(x -> x.getRemetente() != null && alunoId.equals(x.getRemetente().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitacao nao encontrada."));
        if (s.getStatus() != StatusSolicitacaoTransferencia.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Apenas solicitacoes pendentes podem ser canceladas.");
        }
        s.setStatus(StatusSolicitacaoTransferencia.CANCELADA);
        return toDTO(solicitacaoRepository.save(s));
    }

    private SaldoDisponivelDTO calcularSaldo(Long alunoId, Conta conta) {
        int corrente = conta.getSaldoCorrente() == null ? 0 : conta.getSaldoCorrente();
        Long pendentes = solicitacaoRepository
                .somarQuantidadePorRemetenteEStatus(alunoId, StatusSolicitacaoTransferencia.PENDENTE);
        int pend = pendentes == null ? 0 : pendentes.intValue();
        return new SaldoDisponivelDTO(corrente, pend, Math.max(0, corrente - pend));
    }

    private Aluno buscarAluno(Long id) {
        return alunoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno nao encontrado."));
    }

    private ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }

    private SolicitacaoTransferenciaDTO toDTO(SolicitacaoTransferencia s) {
        return new SolicitacaoTransferenciaDTO(
                s.getId(),
                s.getDataSolicitacao(),
                s.getQuantidade(),
                s.getStatus() == null ? null : s.getStatus().name(),
                s.getJustificativa(),
                s.getDestinatario() == null ? null : s.getDestinatario().getId(),
                s.getDestinatario() == null ? null : s.getDestinatario().getNome(),
                s.getAvaliador() == null ? null : s.getAvaliador().getId(),
                s.getAvaliador() == null ? null : s.getAvaliador().getNome());
    }
}
