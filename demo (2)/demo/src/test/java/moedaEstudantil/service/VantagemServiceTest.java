package moedaEstudantil.service;

import moedaEstudantil.model.EmpresaParceira;
import moedaEstudantil.model.Vantagem;
import moedaEstudantil.repository.EmpresaParceiraRepository;
import moedaEstudantil.repository.VantagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VantagemServiceTest {

    private VantagemRepository vantagemRepository;
    private EmpresaParceiraRepository empresaRepository;
    private ArquivoStorageService storage;
    private VantagemService service;
    private EmpresaParceira empresa;

    @BeforeEach
    void setUp() {
        vantagemRepository = mock(VantagemRepository.class);
        empresaRepository = mock(EmpresaParceiraRepository.class);
        storage = mock(ArquivoStorageService.class);
        service = new VantagemService(vantagemRepository, empresaRepository, storage);

        empresa = new EmpresaParceira();
        empresa.setId(5L);
        empresa.setNome("Cantina");
        when(empresaRepository.findById(5L)).thenReturn(Optional.of(empresa));
        when(vantagemRepository.save(any(Vantagem.class))).thenAnswer(i -> i.getArgument(0));
    }

    private MockMultipartFile foto() {
        return new MockMultipartFile("foto", "x.png", "image/png", new byte[]{1, 2, 3});
    }

    private Vantagem vantagemExistente(Long id, EmpresaParceira dona) {
        Vantagem v = new Vantagem();
        v.setId(id);
        v.setNome("A");
        v.setDescricao("B");
        v.setCustoMoedas(10);
        v.setEmpresa(dona);
        v.setFotoUrl("/uploads/antiga.png");
        return v;
    }

    @Test
    void criaVantagemValida() {
        when(storage.salvarImagem(any())).thenReturn("/uploads/nova.png");

        var dto = service.criar(5L, " Combo ", " Lanche ", 40, foto());

        assertEquals("Combo", dto.getNome());
        assertEquals(40, dto.getCustoMoedas());
        assertEquals("/uploads/nova.png", dto.getFotoUrl());
        assertTrue(dto.getAtiva());
    }

    @Test
    void rejeitaCustoInvalidoENomeVazio() {
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.criar(5L, "A", "B", 0, foto())).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.criar(5L, " ", "B", 10, foto())).getStatusCode().value());
    }

    @Test
    void exigeFotoNaCriacao() {
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.criar(5L, "A", "B", 10, null)).getStatusCode().value());
    }

    @Test
    void naoPermiteEditarVantagemDeOutraEmpresa() {
        EmpresaParceira outra = new EmpresaParceira();
        outra.setId(99L);
        when(vantagemRepository.findById(1L)).thenReturn(Optional.of(vantagemExistente(1L, outra)));

        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.atualizar(5L, 1L, "A", "B", 10, null)).getStatusCode().value());
    }

    @Test
    void atualizarComNovaFotoRemoveArquivoAntigo() {
        when(vantagemRepository.findById(1L)).thenReturn(Optional.of(vantagemExistente(1L, empresa)));
        when(storage.salvarImagem(any())).thenReturn("/uploads/nova.png");

        var dto = service.atualizar(5L, 1L, "Novo", "Desc", 20, foto());

        assertEquals("/uploads/nova.png", dto.getFotoUrl());
        verify(storage).remover("/uploads/antiga.png");
    }

    @Test
    void removerFazExclusaoLogicaENaoApagaDoBanco() {
        Vantagem v = vantagemExistente(1L, empresa);
        when(vantagemRepository.findById(1L)).thenReturn(Optional.of(v));

        service.remover(5L, 1L);

        assertFalse(v.getAtiva());
        verify(vantagemRepository, never()).delete(any());
        verify(vantagemRepository, never()).deleteById(any());
    }

    @Test
    void vantagemInativaNaoEhEncontrada() {
        Vantagem v = vantagemExistente(1L, empresa);
        v.setAtiva(false);
        when(vantagemRepository.findById(1L)).thenReturn(Optional.of(v));

        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.buscar(5L, 1L)).getStatusCode().value());
    }
}
