package moedaEstudantil.service;

import moedaEstudantil.dto.VantagemDTO;
import moedaEstudantil.model.EmpresaParceira;
import moedaEstudantil.model.Vantagem;
import moedaEstudantil.repository.EmpresaParceiraRepository;
import moedaEstudantil.repository.VantagemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class VantagemService {

    private final VantagemRepository vantagemRepository;
    private final EmpresaParceiraRepository empresaRepository;
    private final ArquivoStorageService storage;

    public VantagemService(VantagemRepository vantagemRepository,
                           EmpresaParceiraRepository empresaRepository,
                           ArquivoStorageService storage) {
        this.vantagemRepository = vantagemRepository;
        this.empresaRepository = empresaRepository;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<VantagemDTO> listar(Long empresaId) {
        buscarEmpresa(empresaId);
        return vantagemRepository.findAtivasByEmpresaId(empresaId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public VantagemDTO buscar(Long empresaId, Long id) {
        return toDTO(buscarDaEmpresa(empresaId, id));
    }

    @Transactional
    public VantagemDTO criar(Long empresaId, String nome, String descricao, Integer custo, MultipartFile foto) {
        EmpresaParceira empresa = buscarEmpresa(empresaId);
        validar(nome, descricao, custo);
        if (foto == null || foto.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A foto do produto e obrigatoria.");
        }

        Vantagem v = new Vantagem();
        v.setEmpresa(empresa);
        v.setNome(nome.trim());
        v.setDescricao(descricao.trim());
        v.setCustoMoedas(custo);
        v.setAtiva(true);
        v.setFotoUrl(storage.salvarImagem(foto));
        return toDTO(vantagemRepository.save(v));
    }

    @Transactional
    public VantagemDTO atualizar(Long empresaId, Long id, String nome, String descricao, Integer custo, MultipartFile foto) {
        Vantagem v = buscarDaEmpresa(empresaId, id);
        validar(nome, descricao, custo);

        v.setNome(nome.trim());
        v.setDescricao(descricao.trim());
        v.setCustoMoedas(custo);
        if (foto != null && !foto.isEmpty()) {
            String antiga = v.getFotoUrl();
            v.setFotoUrl(storage.salvarImagem(foto));
            storage.remover(antiga);
        }
        return toDTO(vantagemRepository.save(v));
    }

    /** Exclusao logica: a vantagem some da listagem, mas resgates antigos continuam validos. */
    @Transactional
    public void remover(Long empresaId, Long id) {
        Vantagem v = buscarDaEmpresa(empresaId, id);
        v.setAtiva(false);
        vantagemRepository.save(v);
    }

    private EmpresaParceira buscarEmpresa(Long empresaId) {
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada."));
    }

    /** Garante que a vantagem existe, esta ativa e pertence a empresa informada. */
    private Vantagem buscarDaEmpresa(Long empresaId, Long id) {
        Vantagem v = vantagemRepository.findById(id)
                .filter(x -> x.getAtiva() && x.getEmpresa() != null && x.getEmpresa().getId().equals(empresaId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vantagem nao encontrada."));
        return v;
    }

    private void validar(String nome, String descricao, Integer custo) {
        if (nome == null || nome.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome da vantagem e obrigatorio.");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A descricao da vantagem e obrigatoria.");
        }
        if (custo == null || custo <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O custo em moedas deve ser maior que zero.");
        }
    }

    private VantagemDTO toDTO(Vantagem v) {
        VantagemDTO dto = new VantagemDTO();
        dto.setId(v.getId());
        dto.setNome(v.getNome());
        dto.setDescricao(v.getDescricao());
        dto.setFotoUrl(v.getFotoUrl());
        dto.setCustoMoedas(v.getCustoMoedas());
        dto.setAtiva(v.getAtiva());
        if (v.getEmpresa() != null) {
            dto.setEmpresaId(v.getEmpresa().getId());
            dto.setEmpresaNome(v.getEmpresa().getNome());
        }
        return dto;
    }
}
