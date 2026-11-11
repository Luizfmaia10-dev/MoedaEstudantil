package moedaEstudantil.service;

import moedaEstudantil.dto.ColegaDTO;
import moedaEstudantil.dto.SaldoDisponivelDTO;
import moedaEstudantil.dto.SolicitacaoTransferenciaDTO;
import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.Conta;
import moedaEstudantil.model.Professor;
import moedaEstudantil.model.SolicitacaoTransferencia;
import moedaEstudantil.model.StatusSolicitacaoTransferencia;
import moedaEstudantil.model.TipoTransacao;
import moedaEstudantil.model.TransferenciaEntreAlunos;
import moedaEstudantil.repository.AlunoRepository;
import moedaEstudantil.repository.ContaRepository;
import moedaEstudantil.repository.ProfessorRepository;
import moedaEstudantil.repository.SolicitacaoTransferenciaRepository;
import moedaEstudantil.repository.TransacaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Solicitacoes (RF-012) e avaliacao (RF-013) de transferencia de moedas entre alunos.
 * Para evitar solicitacoes que somadas excedam o saldo, o limite na criacao e o saldo
 * disponivel = saldo corrente - soma das solicitacoes pendentes do proprio aluno.
 * A movimentacao efetiva das moedas e o registro no extrato ocorrem somente na aprovacao.
 */
@Service
public class SolicitacaoTransferenciaService {

    public static final int JUSTIFICATIVA_MAXIMA = 300;
    public static final int OBSERVACAO_MAXIMA = 300;

    private final SolicitacaoTransferenciaRepository solicitacaoRepository;
    private final AlunoRepository alunoRepository;
    private final ProfessorRepository professorRepository;
    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;

    public SolicitacaoTransferenciaService(SolicitacaoTransferenciaRepository solicitacaoRepository,
                                           AlunoRepository alunoRepository,
                                           ProfessorRepository professorRepository,
                                           ContaRepository contaRepository,
                                           TransacaoRepository transacaoRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.alunoRepository = alunoRepository;
        this.professorRepository = professorRepository;
        this.contaRepository = contaRepository;
        this.transacaoRepository = transacaoRepository;
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

    @Transactional(readOnly = true)
    public List<SolicitacaoTransferenciaDTO> listarParaProfessor(Long professorId, String statusFiltro) {
        buscarProfessor(professorId);
        return solicitacaoRepository.findByAvaliadorIdOrderByDataSolicitacaoDescIdDesc(professorId).stream()
                .filter(s -> statusFiltro == null || statusFiltro.isBlank()
                        || (s.getStatus() != null && s.getStatus().name().equalsIgnoreCase(statusFiltro)))
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public SolicitacaoTransferenciaDTO criar(Long remetenteId, Long destinatarioId, Long avaliadorId,
                                             Integer quantidade, String justificativa) {
        Aluno remetente = buscarAluno(remetenteId);

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

        Professor avaliador = buscarProfessor(avaliadorId);
        if (avaliador.getInstituicao() == null || !instituicaoId.equals(avaliador.getInstituicao().getId())) {
            throw erro("O professor avaliador deve ser da mesma instituicao.");
        }

        String texto = limparTexto(justificativa, JUSTIFICATIVA_MAXIMA, "A justificativa");

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
        s.setJustificativa(texto);
        s.setStatus(StatusSolicitacaoTransferencia.PENDENTE);
        s.setDataSolicitacao(new Date());
        return toDTO(solicitacaoRepository.save(s));
    }

    /** Cancela uma solicitacao pendente do proprio aluno; libera o saldo comprometido. */
    @Transactional
    public SolicitacaoTransferenciaDTO cancelar(Long alunoId, Long solicitacaoId) {
        contaRepository.findByUsuarioIdParaAtualizacao(alunoId);

        SolicitacaoTransferencia s = solicitacaoRepository.findByIdParaAtualizacao(solicitacaoId)
                .filter(x -> x.getRemetente() != null && alunoId.equals(x.getRemetente().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitacao nao encontrada."));
        if (s.getStatus() != StatusSolicitacaoTransferencia.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Apenas solicitacoes pendentes podem ser canceladas.");
        }
        s.setStatus(StatusSolicitacaoTransferencia.CANCELADA);
        return toDTO(solicitacaoRepository.save(s));
    }

    /**
     * Aprova uma solicitacao pendente atribuida ao professor (RF-013):
     * debita do remetente, credita no destinatario e registra as 2 transacoes no extrato.
     */
    @Transactional
    public SolicitacaoTransferenciaDTO aprovar(Long professorId, Long solicitacaoId, String observacao) {
        buscarProfessor(professorId);
        String obsLimpa = limparTexto(observacao, OBSERVACAO_MAXIMA, "A explicacao");

        SolicitacaoTransferencia s = buscarSolicitacaoPendenteDoProfessor(professorId, solicitacaoId);
        Long remId = s.getRemetente().getId();
        Long destId = s.getDestinatario().getId();

        // Trava as duas contas em ordem crescente de usuarioId para evitar deadlock
        Long primeiroId = Math.min(remId, destId);
        Long segundoId = Math.max(remId, destId);
        Conta primeira = contaRepository.findByUsuarioIdParaAtualizacao(primeiroId)
                .orElseThrow(() -> erro("Conta de aluno nao encontrada."));
        Conta segunda = contaRepository.findByUsuarioIdParaAtualizacao(segundoId)
                .orElseThrow(() -> erro("Conta de aluno nao encontrada."));

        Conta contaRemetente = primeiroId.equals(remId) ? primeira : segunda;
        Conta contaDestinatario = primeiroId.equals(destId) ? primeira : segunda;

        int saldoRem = MovimentacaoSaldo.saldo(contaRemetente);
        int qtd = s.getQuantidade();

        MovimentacaoSaldo.transferir(contaRemetente, contaDestinatario, qtd,
                "Saldo atual do aluno remetente (" + saldoRem
                        + " moedas) e insuficiente para concluir a transferencia.");
        contaRepository.save(contaRemetente);
        contaRepository.save(contaDestinatario);

        Date agora = new Date();
        transacaoRepository.save(novaTransacao(contaRemetente, s, qtd, TipoTransacao.SAIDA, agora));
        transacaoRepository.save(novaTransacao(contaDestinatario, s, qtd, TipoTransacao.ENTRADA, agora));

        s.setStatus(StatusSolicitacaoTransferencia.APROVADA);
        s.setObservacaoAvaliador(obsLimpa);
        s.setDataAvaliacao(agora);
        return toDTO(solicitacaoRepository.save(s));
    }

    /**
     * Recusa uma solicitacao pendente atribuida ao professor (RF-013):
     * nenhuma moeda e movimentada e o valor reservado volta a ficar disponivel ao remetente.
     */
    @Transactional
    public SolicitacaoTransferenciaDTO recusar(Long professorId, Long solicitacaoId, String observacao) {
        buscarProfessor(professorId);
        String obsLimpa = limparTexto(observacao, OBSERVACAO_MAXIMA, "A explicacao");

        SolicitacaoTransferencia s = buscarSolicitacaoPendenteDoProfessor(professorId, solicitacaoId);
        contaRepository.findByUsuarioIdParaAtualizacao(s.getRemetente().getId());

        s.setStatus(StatusSolicitacaoTransferencia.RECUSADA);
        s.setObservacaoAvaliador(obsLimpa);
        s.setDataAvaliacao(new Date());
        return toDTO(solicitacaoRepository.save(s));
    }

    private SolicitacaoTransferencia buscarSolicitacaoPendenteDoProfessor(Long professorId, Long solicitacaoId) {
        SolicitacaoTransferencia s = solicitacaoRepository.findByIdParaAtualizacao(solicitacaoId)
                .filter(x -> x.getAvaliador() != null && professorId.equals(x.getAvaliador().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitacao nao encontrada."));
        if (s.getStatus() != StatusSolicitacaoTransferencia.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Apenas solicitacoes pendentes podem ser avaliadas.");
        }
        return s;
    }

    private TransferenciaEntreAlunos novaTransacao(Conta conta, SolicitacaoTransferencia s,
                                                   int quantidade, TipoTransacao tipo, Date dataHora) {
        TransferenciaEntreAlunos t = new TransferenciaEntreAlunos();
        t.setConta(conta);
        t.setSolicitacao(s);
        t.setQuantidade(quantidade);
        t.setTipoMovimentacao(tipo);
        t.setDataHora(dataHora);
        return t;
    }

    private String limparTexto(String valor, int limite, String rotulo) {
        String limpo = valor == null ? "" : valor.trim();
        if (limpo.length() > limite) {
            throw erro(rotulo + " deve ter no maximo " + limite + " caracteres.");
        }
        return limpo.isEmpty() ? null : limpo;
    }

    private SaldoDisponivelDTO calcularSaldo(Long alunoId, Conta conta) {
        int corrente = MovimentacaoSaldo.saldo(conta);
        Long pendentes = solicitacaoRepository
                .somarQuantidadePorRemetenteEStatus(alunoId, StatusSolicitacaoTransferencia.PENDENTE);
        int pend = pendentes == null ? 0 : pendentes.intValue();
        return new SaldoDisponivelDTO(corrente, pend, Math.max(0, corrente - pend));
    }

    private Aluno buscarAluno(Long id) {
        return alunoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno nao encontrado."));
    }

    private Professor buscarProfessor(Long id) {
        return professorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor nao encontrado."));
    }

    private ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }

    private SolicitacaoTransferenciaDTO toDTO(SolicitacaoTransferencia s) {
        return new SolicitacaoTransferenciaDTO(
                s.getId(),
                s.getDataSolicitacao(),
                s.getDataAvaliacao(),
                s.getQuantidade(),
                s.getStatus() == null ? null : s.getStatus().name(),
                s.getJustificativa(),
                s.getObservacaoAvaliador(),
                s.getRemetente() == null ? null : s.getRemetente().getId(),
                s.getRemetente() == null ? null : s.getRemetente().getNome(),
                s.getRemetente() == null ? null : s.getRemetente().getCurso(),
                s.getDestinatario() == null ? null : s.getDestinatario().getId(),
                s.getDestinatario() == null ? null : s.getDestinatario().getNome(),
                s.getDestinatario() == null ? null : s.getDestinatario().getCurso(),
                s.getAvaliador() == null ? null : s.getAvaliador().getId(),
                s.getAvaliador() == null ? null : s.getAvaliador().getNome());
    }
}
