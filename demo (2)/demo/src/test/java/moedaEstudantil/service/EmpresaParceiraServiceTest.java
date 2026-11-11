package moedaEstudantil.service;

import moedaEstudantil.dto.EmpresaParceiraDTO;
import moedaEstudantil.model.EmpresaParceira;
import moedaEstudantil.repository.EmpresaParceiraRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmpresaParceiraServiceTest {
    private EmpresaParceiraRepository repositorio;
    private SenhaService senhas;
    private EmpresaParceiraService service;

    @BeforeEach
    void preparar() {
        repositorio = mock(EmpresaParceiraRepository.class);
        senhas = new SenhaService();
        service = new EmpresaParceiraService(repositorio, senhas);
    }

    @Test
    void cadastroNormalizaEmailEProtegeSenha() {
        when(repositorio.findByEmailIgnoreCase("loja@exemplo.com")).thenReturn(Optional.empty());
        when(repositorio.findByCnpj("123")).thenReturn(Optional.empty());
        when(repositorio.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        EmpresaParceiraDTO dto = novaEmpresa();

        EmpresaParceiraDTO resposta = service.criar(dto);

        ArgumentCaptor<EmpresaParceira> captor = ArgumentCaptor.forClass(EmpresaParceira.class);
        verify(repositorio).save(captor.capture());
        assertEquals("loja@exemplo.com", captor.getValue().getEmail());
        assertNotEquals("segredo", captor.getValue().getSenha());
        assertTrue(senhas.estaCodificada(captor.getValue().getSenha()));
        assertTrue(senhas.corresponde("segredo", captor.getValue().getSenha()));
        assertNull(resposta.getSenha());
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "cnpj"})
    void rejeitaCadastroDuplicadoSemPersistir(String campo) {
        if (campo.equals("email")) {
            when(repositorio.findByEmailIgnoreCase("loja@exemplo.com"))
                    .thenReturn(Optional.of(new EmpresaParceira()));
        } else {
            when(repositorio.findByCnpj("123")).thenReturn(Optional.of(new EmpresaParceira()));
        }

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.criar(novaEmpresa()));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(repositorio, never()).save(any());
    }

    @Test
    void atualizacaoCodificaNovaSenhaSemDevolveLa() {
        EmpresaParceira existente = empresaExistente();
        EmpresaParceiraDTO dto = novaEmpresa();
        dto.setSenha("senha-nova");

        EmpresaParceiraDTO resposta = service.atualizar(5L, dto);

        assertNotEquals("senha-nova", existente.getSenha());
        assertTrue(senhas.estaCodificada(existente.getSenha()));
        assertTrue(senhas.corresponde("senha-nova", existente.getSenha()));
        assertNull(resposta.getSenha());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void atualizacaoSemNovaSenhaPreservaHash(String senha) {
        EmpresaParceira existente = empresaExistente();
        String hashAnterior = existente.getSenha();
        EmpresaParceiraDTO dto = novaEmpresa();
        dto.setSenha(senha);

        service.atualizar(5L, dto);

        assertEquals(hashAnterior, existente.getSenha());
    }

    private EmpresaParceira empresaExistente() {
        EmpresaParceira empresa = new EmpresaParceira();
        empresa.setId(5L);
        empresa.setSenha(senhas.codificar("senha-antiga"));
        when(repositorio.findById(5L)).thenReturn(Optional.of(empresa));
        when(repositorio.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return empresa;
    }

    private EmpresaParceiraDTO novaEmpresa() {
        EmpresaParceiraDTO dto = new EmpresaParceiraDTO();
        dto.setNome("Loja");
        dto.setCnpj("123");
        dto.setEmail(" LOJA@EXEMPLO.COM ");
        dto.setSenha("segredo");
        return dto;
    }
}
