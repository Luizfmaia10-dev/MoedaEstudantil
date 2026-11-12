package moedaEstudantil.service;

import moedaEstudantil.dto.AutomacaoTransferenciaDTO;
import moedaEstudantil.model.Professor;
import moedaEstudantil.model.SolicitacaoTransferencia;
import moedaEstudantil.model.StatusSolicitacaoTransferencia;
import moedaEstudantil.repository.ProfessorRepository;
import moedaEstudantil.repository.SolicitacaoTransferenciaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AutomacaoTransferenciaService {

    public static final double CONFIANCA_MINIMA = 0.80;

    private final ProfessorRepository professorRepository;
    private final SolicitacaoTransferenciaRepository solicitacaoRepository;
    private final AvaliadorTransferenciaIA avaliador;
    private final SolicitacaoTransferenciaService solicitacaoService;

    public AutomacaoTransferenciaService(ProfessorRepository professorRepository,
                                         SolicitacaoTransferenciaRepository solicitacaoRepository,
                                         AvaliadorTransferenciaIA avaliador,
                                         SolicitacaoTransferenciaService solicitacaoService) {
        this.professorRepository = professorRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.avaliador = avaliador;
        this.solicitacaoService = solicitacaoService;
    }

    public AutomacaoTransferenciaDTO consultar(Long professorId) {
        return toDTO(buscarProfessor(professorId));
    }

    public AutomacaoTransferenciaDTO configurar(Long professorId, boolean ativa) {
        Professor professor = buscarProfessor(professorId);
        if (ativa && !possuiCriterios(professor)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cadastre os criterios de transferencia antes de ativar a automacao.");
        }
        professor.setAutomacaoTransferenciasAtiva(ativa);
        professorRepository.save(professor);

        if (ativa) {
            List<Long> pendentes = solicitacaoRepository
                    .findByAvaliadorIdAndStatusOrderByDataSolicitacaoAscIdAsc(
                            professorId, StatusSolicitacaoTransferencia.PENDENTE)
                    .stream().map(SolicitacaoTransferencia::getId).toList();
            for (Long solicitacaoId : pendentes) {
                try {
                    avaliarSeAtiva(solicitacaoId);
                } catch (IaLocalIndisponivelException ex) {
                    break;
                } catch (ResponseStatusException ignored) {
                    // A solicitacao pode ter sido resolvida enquanto o lote era processado.
                }
            }
        }
        return toDTO(professor);
    }

    public void avaliarSeAtiva(Long solicitacaoId) {
        SolicitacaoTransferencia solicitacao = solicitacaoRepository.findById(solicitacaoId).orElse(null);
        if (solicitacao == null || solicitacao.getStatus() != StatusSolicitacaoTransferencia.PENDENTE
                || solicitacao.getAvaliador() == null
                || !solicitacao.getAvaliador().isAutomacaoTransferenciasAtiva()
                || !possuiCriterios(solicitacao.getAvaliador())) {
            return;
        }

        Professor professor = solicitacao.getAvaliador();
        AvaliadorTransferenciaIA.Resultado resultado = avaliador.avaliar(
                new AvaliadorTransferenciaIA.Contexto(
                        professor.getCriteriosTransferencia(),
                        solicitacao.getQuantidade() == null ? 0 : solicitacao.getQuantidade(),
                        solicitacao.getJustificativa(),
                        nome(solicitacao.getRemetente()),
                        nome(solicitacao.getDestinatario())));

        if (resultado.decisao() == AvaliadorTransferenciaIA.Decisao.PENDENTE
                || resultado.confianca() < CONFIANCA_MINIMA) {
            return;
        }

        Professor estadoAtual = buscarProfessor(professor.getId());
        if (!estadoAtual.isAutomacaoTransferenciasAtiva()) return;

        if (resultado.decisao() == AvaliadorTransferenciaIA.Decisao.APROVAR) {
            solicitacaoService.aprovarAutomaticamente(professor.getId(), solicitacaoId,
                    resultado.explicacao(), resultado.confianca());
        } else {
            solicitacaoService.recusarAutomaticamente(professor.getId(), solicitacaoId,
                    resultado.explicacao(), resultado.confianca());
        }
    }

    private String nome(moedaEstudantil.model.Aluno aluno) {
        return aluno == null ? null : aluno.getNome();
    }

    private Professor buscarProfessor(Long professorId) {
        return professorRepository.findById(professorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Professor nao encontrado."));
    }

    private boolean possuiCriterios(Professor professor) {
        return professor.getCriteriosTransferencia() != null
                && !professor.getCriteriosTransferencia().isBlank();
    }

    private AutomacaoTransferenciaDTO toDTO(Professor professor) {
        boolean possuiCriterios = possuiCriterios(professor);
        long pendentes = solicitacaoRepository.countByAvaliadorIdAndStatus(
                professor.getId(), StatusSolicitacaoTransferencia.PENDENTE);
        return new AutomacaoTransferenciaDTO(professor.isAutomacaoTransferenciasAtiva(),
                possuiCriterios, professor.getCriteriosTransferencia(), pendentes);
    }
}
