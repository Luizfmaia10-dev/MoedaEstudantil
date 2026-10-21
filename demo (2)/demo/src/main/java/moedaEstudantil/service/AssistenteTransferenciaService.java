package moedaEstudantil.service;

import moedaEstudantil.dto.AssistenteTransferenciaDTO;
import moedaEstudantil.dto.AssistenteTransferenciaDTO.Opcao;
import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.Professor;
import moedaEstudantil.repository.AlunoRepository;
import moedaEstudantil.repository.ProfessorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

@Service
public class AssistenteTransferenciaService {

    public record RascunhoAssistente(
            Long destinatarioId,
            String destinatarioNome,
            Long avaliadorId,
            String avaliadorNome,
            Integer quantidade,
            String justificativa) {

        public static RascunhoAssistente vazio() {
            return new RascunhoAssistente(null, null, null, null, null, null);
        }
    }

    private final AlunoRepository alunoRepository;
    private final ProfessorRepository professorRepository;
    private final InterpretadorTransferenciaIA interpretador;

    public AssistenteTransferenciaService(AlunoRepository alunoRepository,
                                          ProfessorRepository professorRepository,
                                          InterpretadorTransferenciaIA interpretador) {
        this.alunoRepository = alunoRepository;
        this.professorRepository = professorRepository;
        this.interpretador = interpretador;
    }

    @Transactional(readOnly = true)
    public AssistenteTransferenciaDTO interpretar(Long alunoId, String mensagem, RascunhoAssistente rascunhoRecebido) {
        Aluno aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado."));
        if (aluno.getInstituicao() == null || aluno.getInstituicao().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O aluno não está vinculado a uma instituição.");
        }

        RascunhoAssistente rascunho = rascunhoRecebido == null ? RascunhoAssistente.vazio() : rascunhoRecebido;
        InterpretadorTransferenciaIA.Intencao intencao = interpretador.interpretar(mensagem,
                new InterpretadorTransferenciaIA.Rascunho(
                        rascunho.destinatarioNome(), rascunho.avaliadorNome(),
                        rascunho.quantidade(), rascunho.justificativa()));

        Long instituicaoId = aluno.getInstituicao().getId();
        List<Aluno> colegas = alunoRepository.findByInstituicaoIdAndIdNotOrderByNome(instituicaoId, alunoId);
        List<Professor> professores = professorRepository.findByInstituicaoIdOrderByNome(instituicaoId);

        Integer quantidade = intencao.quantidade() != null ? intencao.quantidade() : rascunho.quantidade();
        if (quantidade == null || quantidade <= 0) {
            return resposta("PRECISA_QUANTIDADE", "Quantas moedas você deseja enviar?",
                    null, null, null, null, null, justificativa(intencao, rascunho), List.of());
        }

        List<Aluno> destinatarios = resolver(colegas, rascunho.destinatarioId(), rascunho.destinatarioNome(),
                intencao.destinatario(), Aluno::getId, Aluno::getNome);
        if (destinatarios.size() != 1) {
            List<Opcao> opcoes = (destinatarios.isEmpty() ? colegas : destinatarios).stream()
                    .map(c -> new Opcao("DESTINATARIO", c.getId(), c.getNome(), c.getCurso()))
                    .toList();
            return resposta("PRECISA_DESTINATARIO",
                    destinatarios.size() > 1 ? "Encontrei mais de um aluno. Qual deles?" : "Para qual aluno você deseja enviar?",
                    null, null, null, null, quantidade, justificativa(intencao, rascunho), opcoes);
        }

        Aluno destinatario = destinatarios.getFirst();
        List<Professor> avaliadores = resolver(professores, rascunho.avaliadorId(), rascunho.avaliadorNome(),
                intencao.professor(), Professor::getId, Professor::getNome);
        if (avaliadores.size() != 1) {
            List<Opcao> opcoes = (avaliadores.isEmpty() ? professores : avaliadores).stream()
                    .map(p -> new Opcao("PROFESSOR", p.getId(), p.getNome(), p.getDepartamento()))
                    .toList();
            return resposta("PRECISA_PROFESSOR",
                    avaliadores.size() > 1 ? "Encontrei mais de um professor. Qual deles avaliará?"
                            : "Qual professor deverá avaliar a solicitação?",
                    destinatario.getId(), destinatario.getNome(), null, null,
                    quantidade, justificativa(intencao, rascunho), opcoes);
        }

        Professor avaliador = avaliadores.getFirst();
        return resposta("PRONTO", "Revise os dados antes de confirmar a solicitação.",
                destinatario.getId(), destinatario.getNome(), avaliador.getId(), avaliador.getNome(),
                quantidade, justificativa(intencao, rascunho), List.of());
    }

    private <T> List<T> resolver(List<T> permitidos, Long idAtual, String nomeAtual, String nomeInterpretado,
                                 Function<T, Long> id, Function<T, String> nome) {
        String consulta = normalizar(nomeInterpretado);
        boolean manteveNomeAtual = consulta.isBlank() || consulta.equals(normalizar(nomeAtual));
        if (idAtual != null && manteveNomeAtual) {
            List<T> porId = permitidos.stream().filter(item -> Objects.equals(id.apply(item), idAtual)).toList();
            if (porId.size() == 1) return porId;
        }
        if (consulta.isBlank()) return List.of();
        List<T> exatos = permitidos.stream()
                .filter(item -> normalizar(nome.apply(item)).equals(consulta)).toList();
        if (!exatos.isEmpty()) return exatos;
        return permitidos.stream()
                .filter(item -> normalizar(nome.apply(item)).contains(consulta)).toList();
    }

    private String justificativa(InterpretadorTransferenciaIA.Intencao intencao, RascunhoAssistente rascunho) {
        String valor = intencao.justificativa() != null ? intencao.justificativa() : rascunho.justificativa();
        if (valor == null) return null;
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo.substring(0,
                Math.min(limpo.length(), SolicitacaoTransferenciaService.JUSTIFICATIVA_MAXIMA));
    }

    private String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }

    private AssistenteTransferenciaDTO resposta(String estado, String mensagem,
                                                Long destinatarioId, String destinatarioNome,
                                                Long avaliadorId, String avaliadorNome,
                                                Integer quantidade, String justificativa, List<Opcao> opcoes) {
        return new AssistenteTransferenciaDTO(estado, mensagem, destinatarioId, destinatarioNome,
                avaliadorId, avaliadorNome, quantidade, justificativa, opcoes);
    }
}
