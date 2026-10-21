package moedaEstudantil.controller;

import moedaEstudantil.dto.AssistenteTransferenciaDTO;
import moedaEstudantil.service.AssistenteTransferenciaService;
import moedaEstudantil.service.IaLocalIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssistenteTransferenciaControllerTest {

    private AssistenteTransferenciaService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(AssistenteTransferenciaService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AssistenteTransferenciaController(service)).build();
    }

    @Test
    void interpretaSemCriarTransferencia() throws Exception {
        AssistenteTransferenciaDTO resposta = new AssistenteTransferenciaDTO(
                "PRONTO", "Revise os dados.", 6L, "Ana Souza", 2L, "Carlos Lima",
                20, null, List.of());
        when(service.interpretar(eq(4L), eq("mande vinte moedas para Ana"), any()))
                .thenReturn(resposta);

        mvc.perform(post("/api/alunos/4/transferencias/assistente/interpretar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mensagem":"mande vinte moedas para Ana",
                                  "rascunho":{"destinatarioId":null,"avaliadorId":null,"quantidade":null,"justificativa":null}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PRONTO"))
                .andExpect(jsonPath("$.destinatarioId").value(6))
                .andExpect(jsonPath("$.avaliadorId").value(2))
                .andExpect(jsonPath("$.quantidade").value(20));
    }

    @Test
    void recusaMensagemEmBrancoAntesDoService() throws Exception {
        mvc.perform(post("/api/alunos/4/transferencias/assistente/interpretar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensagem\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Escreva como deseja transferir as moedas."));
        verifyNoInteractions(service);
    }

    @Test
    void indisponibilidadeDaIaRetorna503SemDetalhesInternos() throws Exception {
        when(service.interpretar(eq(4L), eq("envie 10 para Ana"), any()))
                .thenThrow(new IaLocalIndisponivelException("segredo interno", new RuntimeException("host")));

        mvc.perform(post("/api/alunos/4/transferencias/assistente/interpretar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensagem\":\"envie 10 para Ana\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(
                        "A IA local está indisponível. Você ainda pode usar o formulário manual."));
    }
}
