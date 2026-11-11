package moedaEstudantil.service;

import moedaEstudantil.dto.EmpresaParceiraDTO;
import moedaEstudantil.model.EmpresaParceira;
import moedaEstudantil.repository.EmpresaParceiraRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class EmpresaParceiraService {
    private final EmpresaParceiraRepository empresas;
    private final SenhaService senhas;

    public EmpresaParceiraService(EmpresaParceiraRepository empresas, SenhaService senhas) {
        this.empresas = empresas;
        this.senhas = senhas;
    }

    public List<EmpresaParceiraDTO> listar() {
        return empresas.findAll().stream().map(this::toDTO).toList();
    }

    public EmpresaParceiraDTO buscar(Long id) { return toDTO(obter(id)); }

    public EmpresaParceiraDTO criar(EmpresaParceiraDTO dto) {
        String email = normalizar(dto.getEmail());
        if (empresas.findByEmailIgnoreCase(email).isPresent()) conflito("E-mail já cadastrado.");
        if (empresas.findByCnpj(dto.getCnpj()).isPresent()) conflito("CNPJ já cadastrado.");
        EmpresaParceira empresa = new EmpresaParceira();
        copiar(dto, empresa, email);
        empresa.setSenha(senhas.codificar(dto.getSenha()));
        return toDTO(empresas.save(empresa));
    }

    public EmpresaParceiraDTO atualizar(Long id, EmpresaParceiraDTO dto) {
        EmpresaParceira empresa = obter(id);
        String email = normalizar(dto.getEmail());
        empresas.findByEmailIgnoreCase(email).filter(e -> !e.getId().equals(id))
                .ifPresent(e -> conflito("E-mail já cadastrado."));
        empresas.findByCnpj(dto.getCnpj()).filter(e -> !e.getId().equals(id))
                .ifPresent(e -> conflito("CNPJ já cadastrado."));
        copiar(dto, empresa, email);
        if (dto.getSenha() != null && !dto.getSenha().isBlank()) empresa.setSenha(senhas.codificar(dto.getSenha()));
        return toDTO(empresas.save(empresa));
    }

    public void excluir(Long id) {
        if (!empresas.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        empresas.deleteById(id);
    }

    private void copiar(EmpresaParceiraDTO dto, EmpresaParceira empresa, String email) {
        empresa.setNome(dto.getNome());
        empresa.setCnpj(dto.getCnpj());
        empresa.setEmail(email);
    }

    private EmpresaParceira obter(Long id) {
        return empresas.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private String normalizar(String email) {
        if (email == null || email.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail obrigatório.");
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void conflito(String mensagem) { throw new ResponseStatusException(HttpStatus.CONFLICT, mensagem); }

    private EmpresaParceiraDTO toDTO(EmpresaParceira empresa) {
        EmpresaParceiraDTO dto = new EmpresaParceiraDTO();
        dto.setId(empresa.getId());
        dto.setNome(empresa.getNome());
        dto.setCnpj(empresa.getCnpj());
        dto.setEmail(empresa.getEmail());
        return dto;
    }
}
