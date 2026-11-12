package moedaEstudantil.controller;

import moedaEstudantil.dto.AutomacaoTransferenciaDTO;
import moedaEstudantil.service.AutomacaoTransferenciaService;
import moedaEstudantil.service.SolicitacaoTransferenciaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SolicitacaoTransferenciaControllerTest {

    private AutomacaoTransferenciaService automacao;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        automacao = mock(AutomacaoTransferenciaService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SolicitacaoTransferenciaController(
                mock(SolicitacaoTransferenciaService.class), automacao)).build();
    }

    @Test
    void consultaConfiguracaoDaAutomacao() throws Exception {
        when(automacao.consultar(1L)).thenReturn(
                new AutomacaoTransferenciaDTO(false, true, "Aprovar colaboração", 3));

        mvc.perform(get("/api/professores/1/transferencias/automacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(false))
                .andExpect(jsonPath("$.possuiCriterios").value(true))
                .andExpect(jsonPath("$.pendentes").value(3));
    }

    @Test
    void ativaAutomacaoDoProfessor() throws Exception {
        when(automacao.configurar(1L, true)).thenReturn(
                new AutomacaoTransferenciaDTO(true, true, "Aprovar colaboração", 0));

        mvc.perform(put("/api/professores/1/transferencias/automacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativa\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(true));

        verify(automacao).configurar(1L, true);
    }

    @Test
    void rejeitaConfiguracaoSemEstado() throws Exception {
        mvc.perform(put("/api/professores/1/transferencias/automacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(automacao);
    }
}
