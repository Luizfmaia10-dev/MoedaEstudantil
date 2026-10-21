package moedaEstudantil.controller;

import moedaEstudantil.model.Aluno;
import moedaEstudantil.repository.AlunoRepository;
import moedaEstudantil.repository.EmpresaParceiraRepository;
import moedaEstudantil.repository.ProfessorRepository;
import moedaEstudantil.service.AutenticacaoService;
import moedaEstudantil.service.SenhaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AutenticacaoControllerTest {
    private AlunoRepository alunos;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        alunos = mock(AlunoRepository.class);
        AutenticacaoService service = new AutenticacaoService(alunos,
                mock(ProfessorRepository.class), mock(EmpresaParceiraRepository.class), new SenhaService());
        mvc = MockMvcBuilders.standaloneSetup(new AutenticacaoController(service)).build();
        Aluno aluno = new Aluno();
        aluno.setId(4L);
        aluno.setNome("Aluno Teste");
        aluno.setEmail("aluno@puc.br");
        aluno.setSenha(new SenhaService().codificar("senha123"));
        when(alunos.findByEmailIgnoreCase("aluno@puc.br")).thenReturn(Optional.of(aluno));
    }

    @Test
    void devolveSomenteIdentidadeSeguraAoAutenticar() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"aluno@puc.br","senha":"senha123","perfil":"ALUNO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":4,"nome":"Aluno Teste","email":"aluno@puc.br","perfil":"ALUNO"}
                        """))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"email\":\"desconhecido@puc.br\",\"senha\":\"senha123\",\"perfil\":\"ALUNO\"}",
            "{\"email\":\"aluno@puc.br\",\"senha\":\"errada\",\"perfil\":\"ALUNO\"}",
            "{\"email\":\"aluno@puc.br\",\"senha\":\"senha123\",\"perfil\":\"EMPRESA\"}"
    })
    void devolve401GenericoParaCredenciaisOuPerfilIncorretos(String request) throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail, senha ou perfil inválido."));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"email\":\"aluno@puc.br\",\"senha\":\"senha123\"}",
            "{\"email\":\" \",\"senha\":\"senha123\",\"perfil\":\"ALUNO\"}",
            "{\"email\":\"aluno@puc.br\",\"senha\":\" \",\"perfil\":\"ALUNO\"}"
    })
    void devolve400ComMensagemParaEntradaIncompleta(String request) throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Informe e-mail, senha e perfil."));
    }

    @Test
    void devolve503SemExporDetalhesDaPersistencia() throws Exception {
        when(alunos.findByEmailIgnoreCase("aluno@puc.br"))
                .thenThrow(new DataAccessResourceFailureException("banco offline: detalhes internos"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"aluno@puc.br","senha":"senha123","perfil":"ALUNO"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"message\":\"Serviço temporariamente indisponível.\"}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{\"email\":"})
    void devolve400SemDetalhesInternosParaCorpoAusenteOuJsonMalformado(String request) throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"message\":\"Informe e-mail, senha e perfil.\"}"));
        verifyNoInteractions(alunos);
    }
}
