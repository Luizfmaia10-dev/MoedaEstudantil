package moedaEstudantil.service;

import moedaEstudantil.dto.AssistenteTransferenciaDTO;
import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.Instituicao;
import moedaEstudantil.model.Professor;
import moedaEstudantil.repository.AlunoRepository;
import moedaEstudantil.repository.ProfessorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssistenteTransferenciaServiceTest {

    private AlunoRepository alunoRepository;
    private ProfessorRepository professorRepository;
    private InterpretadorTransferenciaIA interpretador;
    private AssistenteTransferenciaService service;
    private Aluno remetente;
    private Aluno ana;
    private Professor carlos;

    @BeforeEach
    void setUp() {
        alunoRepository = mock(AlunoRepository.class);
        professorRepository = mock(ProfessorRepository.class);
        interpretador = mock(InterpretadorTransferenciaIA.class);
        service = new AssistenteTransferenciaService(alunoRepository, professorRepository, interpretador);

        Instituicao instituicao = new Instituicao();
        instituicao.setId(1L);
        instituicao.setNome("PUC Minas");

        remetente = aluno(4L, "João Remetente", instituicao);
        ana = aluno(6L, "Ana Souza", instituicao);
        carlos = professor(2L, "Carlos Lima", instituicao);

        when(alunoRepository.findById(4L)).thenReturn(Optional.of(remetente));
        when(alunoRepository.findByInstituicaoIdAndIdNotOrderByNome(1L, 4L)).thenReturn(List.of(ana));
        when(professorRepository.findByInstituicaoIdOrderByNome(1L)).thenReturn(List.of(carlos));
    }

    @Test
    void resolveNomesDaInstituicaoEDevolveResumoProntoSemPersistir() {
        when(interpretador.interpretar("mande vinte moedas para Ana pelo Carlos",
                new InterpretadorTransferenciaIA.Rascunho(null, null, null, null)))
                .thenReturn(new InterpretadorTransferenciaIA.Intencao("Ana", "Carlos", 20, "Projeto em grupo"));

        AssistenteTransferenciaDTO resposta = service.interpretar(4L,
                "mande vinte moedas para Ana pelo Carlos", AssistenteTransferenciaService.RascunhoAssistente.vazio());

        assertEquals("PRONTO", resposta.estado());
        assertEquals(6L, resposta.destinatarioId());
        assertEquals("Ana Souza", resposta.destinatarioNome());
        assertEquals(2L, resposta.avaliadorId());
        assertEquals("Carlos Lima", resposta.avaliadorNome());
        assertEquals(20, resposta.quantidade());
        assertEquals("Projeto em grupo", resposta.justificativa());
        assertEquals(List.of(), resposta.opcoes());
    }

    @Test
    void nomeAmbiguoPedeEscolhaSemExporAlunoDeOutraInstituicao() {
        Instituicao instituicao = remetente.getInstituicao();
        Aluno anaClara = aluno(7L, "Ana Clara", instituicao);
        when(alunoRepository.findByInstituicaoIdAndIdNotOrderByNome(1L, 4L)).thenReturn(List.of(ana, anaClara));
        when(interpretador.interpretar("envie 10 para Ana", new InterpretadorTransferenciaIA.Rascunho(null, null, null, null)))
                .thenReturn(new InterpretadorTransferenciaIA.Intencao("Ana", null, 10, null));

        AssistenteTransferenciaDTO resposta = service.interpretar(4L, "envie 10 para Ana",
                AssistenteTransferenciaService.RascunhoAssistente.vazio());

        assertEquals("PRECISA_DESTINATARIO", resposta.estado());
        assertEquals(2, resposta.opcoes().size());
        assertEquals(List.of(6L, 7L), resposta.opcoes().stream().map(AssistenteTransferenciaDTO.Opcao::id).toList());
    }

    @Test
    void solicitaProfessorQuandoNaoFoiInformado() {
        when(interpretador.interpretar("envie 10 para Ana", new InterpretadorTransferenciaIA.Rascunho(null, null, null, null)))
                .thenReturn(new InterpretadorTransferenciaIA.Intencao("Ana", null, 10, null));

        AssistenteTransferenciaDTO resposta = service.interpretar(4L, "envie 10 para Ana",
                AssistenteTransferenciaService.RascunhoAssistente.vazio());

        assertEquals("PRECISA_PROFESSOR", resposta.estado());
        assertEquals(6L, resposta.destinatarioId());
        assertEquals(1, resposta.opcoes().size());
        assertEquals(2L, resposta.opcoes().getFirst().id());
    }

    @Test
    void quantidadeInvalidaTemPrioridadeENaoInventaValor() {
        when(interpretador.interpretar("mande moedas para Ana", new InterpretadorTransferenciaIA.Rascunho(null, null, null, null)))
                .thenReturn(new InterpretadorTransferenciaIA.Intencao("Ana", "Carlos", 0, null));

        AssistenteTransferenciaDTO resposta = service.interpretar(4L, "mande moedas para Ana",
                AssistenteTransferenciaService.RascunhoAssistente.vazio());

        assertEquals("PRECISA_QUANTIDADE", resposta.estado());
        assertNull(resposta.quantidade());
    }

    @Test
    void recusaIdentificadorDoRascunhoQueNaoPertenceAoContextoPermitido() {
        AssistenteTransferenciaService.RascunhoAssistente adulterado =
                new AssistenteTransferenciaService.RascunhoAssistente(999L, "Pessoa externa", 2L, "Carlos Lima", 20, null);
        when(interpretador.interpretar("confirmar dados", new InterpretadorTransferenciaIA.Rascunho("Pessoa externa", "Carlos Lima", 20, null)))
                .thenReturn(new InterpretadorTransferenciaIA.Intencao("Pessoa externa", "Carlos Lima", 20, null));

        AssistenteTransferenciaDTO resposta = service.interpretar(4L, "confirmar dados", adulterado);

        assertEquals("PRECISA_DESTINATARIO", resposta.estado());
        assertNull(resposta.destinatarioId());
    }

    @Test
    void novaMensagemPodeTrocarDestinatarioDoRascunho() {
        Aluno beatriz = aluno(8L, "Beatriz Alves", remetente.getInstituicao());
        when(alunoRepository.findByInstituicaoIdAndIdNotOrderByNome(1L, 4L)).thenReturn(List.of(ana, beatriz));
        AssistenteTransferenciaService.RascunhoAssistente anterior =
                new AssistenteTransferenciaService.RascunhoAssistente(6L, "Ana Souza", 2L, "Carlos Lima", 20, null);
        when(interpretador.interpretar("na verdade, envie para Beatriz",
                new InterpretadorTransferenciaIA.Rascunho("Ana Souza", "Carlos Lima", 20, null)))
                .thenReturn(new InterpretadorTransferenciaIA.Intencao("Beatriz", "Carlos Lima", 20, null));

        AssistenteTransferenciaDTO resposta = service.interpretar(4L, "na verdade, envie para Beatriz", anterior);

        assertEquals("PRONTO", resposta.estado());
        assertEquals(8L, resposta.destinatarioId());
        assertEquals("Beatriz Alves", resposta.destinatarioNome());
    }

    @Test
    void alunoInexistenteRetornaNotFoundAntesDeConsultarIa() {
        when(alunoRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.interpretar(99L, "envie 10 para Ana", AssistenteTransferenciaService.RascunhoAssistente.vazio()));

        assertEquals(404, erro.getStatusCode().value());
    }

    private Aluno aluno(Long id, String nome, Instituicao instituicao) {
        Aluno aluno = new Aluno();
        aluno.setId(id);
        aluno.setNome(nome);
        aluno.setCurso("Engenharia de Software");
        aluno.setInstituicao(instituicao);
        return aluno;
    }

    private Professor professor(Long id, String nome, Instituicao instituicao) {
        Professor professor = new Professor();
        professor.setId(id);
        professor.setNome(nome);
        professor.setDepartamento("Computação");
        professor.setInstituicao(instituicao);
        return professor;
    }
}
