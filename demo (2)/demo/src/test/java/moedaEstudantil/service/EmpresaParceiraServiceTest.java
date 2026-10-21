package moedaEstudantil.service;

import moedaEstudantil.dto.EmpresaParceiraDTO;
import moedaEstudantil.model.EmpresaParceira;
import moedaEstudantil.repository.EmpresaParceiraRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmpresaParceiraServiceTest {
    @Test
    void cadastroNormalizaEmailEProtegeSenha() {
        EmpresaParceiraRepository repositorio = mock(EmpresaParceiraRepository.class);
        SenhaService senhas = new SenhaService();
        EmpresaParceiraService service = new EmpresaParceiraService(repositorio, senhas);
        when(repositorio.findByEmailIgnoreCase("loja@exemplo.com")).thenReturn(Optional.empty());
        when(repositorio.findByCnpj("123")).thenReturn(Optional.empty());
        when(repositorio.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        EmpresaParceiraDTO dto = new EmpresaParceiraDTO();
        dto.setNome("Loja");
        dto.setCnpj("123");
        dto.setEmail(" LOJA@EXEMPLO.COM ");
        dto.setSenha("segredo");

        service.criar(dto);

        ArgumentCaptor<EmpresaParceira> captor = ArgumentCaptor.forClass(EmpresaParceira.class);
        verify(repositorio).save(captor.capture());
        assertTrue(senhas.corresponde("segredo", captor.getValue().getSenha()));
    }
}
