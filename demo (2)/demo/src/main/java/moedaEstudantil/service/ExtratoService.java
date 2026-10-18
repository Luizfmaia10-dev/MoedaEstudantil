package moedaEstudantil.service;

import moedaEstudantil.dto.ExtratoDTO;
import moedaEstudantil.dto.ExtratoDTO.TransacaoDTO;
import moedaEstudantil.model.*;
import moedaEstudantil.repository.ContaRepository;
import moedaEstudantil.repository.TransacaoRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ExtratoService {

    public static final String CATEGORIA_ENVIO = "ENVIO";
    public static final String CATEGORIA_RESGATE = "RESGATE";
    public static final String CATEGORIA_TRANSFERENCIA = "TRANSFERENCIA";

    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;

    public ExtratoService(ContaRepository contaRepository, TransacaoRepository transacaoRepository) {
        this.contaRepository = contaRepository;
        this.transacaoRepository = transacaoRepository;
    }

    /**
     * Monta o extrato do usuário (aluno ou professor).
     *
     * @param categoria filtro opcional (ENVIO, RESGATE, TRANSFERENCIA); null/vazio = todas
     * @return vazio se o usuário não possuir conta
     */
    @Transactional(readOnly = true)
    public Optional<ExtratoDTO> gerarExtrato(Long usuarioId, String categoria) {
        return contaRepository.findByUsuarioId(usuarioId).map(conta -> {
            Usuario usuario = (Usuario) Hibernate.unproxy(conta.getUsuario());

            List<TransacaoDTO> transacoes = transacaoRepository
                    .findByContaIdOrderByDataHoraDescIdDesc(conta.getId()).stream()
                    .map(t -> (TransacaoGeral) Hibernate.unproxy(t))
                    .map(t -> toDTO(t, usuario))
                    .filter(t -> categoria == null || categoria.isBlank()
                            || t.getCategoria().equalsIgnoreCase(categoria))
                    .toList();

            ExtratoDTO dto = new ExtratoDTO();
            dto.setUsuarioId(usuario.getId());
            dto.setTitularNome(nomeDe(usuario));
            dto.setTitularTipo(usuario instanceof Professor ? "PROFESSOR" : "ALUNO");
            dto.setSaldoAtual(conta.getSaldoCorrente() == null ? 0 : conta.getSaldoCorrente());
            dto.setTransacoes(transacoes);
            return dto;
        });
    }

    private TransacaoDTO toDTO(TransacaoGeral t, Usuario titular) {
        TransacaoDTO dto = new TransacaoDTO();
        dto.setId(t.getId());
        dto.setDataHora(t.getDataHora());
        dto.setQuantidade(t.getQuantidade());
        dto.setMovimentacao(t.getTipoMovimentacao() == null ? null : t.getTipoMovimentacao().name());

        boolean entrada = t.getTipoMovimentacao() == TipoTransacao.ENTRADA;

        if (t instanceof Envio e) {
            dto.setCategoria(CATEGORIA_ENVIO);
            String motivo = e.getMotivo() == null ? "" : " — " + e.getMotivo();
            dto.setDescricao(titular instanceof Professor
                    ? "Enviou para " + nomeDe(e.getAluno()) + motivo
                    : "Recebeu do Prof. " + nomeDe(e.getProfessor()) + motivo);
        } else if (t instanceof ResgateVantagem r) {
            dto.setCategoria(CATEGORIA_RESGATE);
            String vantagem = r.getVantagem() == null ? "vantagem" : r.getVantagem().getNome();
            String cupom = r.getCodigoCupom() == null ? "" : " (cupom " + r.getCodigoCupom() + ")";
            dto.setDescricao("Resgatou " + vantagem + cupom);
        } else if (t instanceof TransferenciaEntreAlunos tr) {
            dto.setCategoria(CATEGORIA_TRANSFERENCIA);
            SolicitacaoTransferencia s = tr.getSolicitacao();
            if (s == null) {
                dto.setDescricao(entrada ? "Transferência recebida" : "Transferência enviada");
            } else {
                dto.setDescricao(entrada
                        ? "Transferência recebida de " + nomeDe(s.getRemetente())
                        : "Transferência enviada para " + nomeDe(s.getDestinatario()));
            }
        } else {
            dto.setCategoria("OUTRO");
            dto.setDescricao("Movimentação");
        }
        return dto;
    }

    private String nomeDe(Usuario u) {
        if (u == null) return "—";
        u = (Usuario) Hibernate.unproxy(u);
        if (u instanceof Aluno a) return a.getNome();
        if (u instanceof Professor p) return p.getNome();
        return u.getEmail();
    }
}
