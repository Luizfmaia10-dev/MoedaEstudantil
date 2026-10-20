package moedaEstudantil.service;

import moedaEstudantil.dto.CatalogoVantagemDTO;
import moedaEstudantil.dto.ResgateResultadoDTO;
import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.Conta;
import moedaEstudantil.model.ResgateVantagem;
import moedaEstudantil.model.TipoTransacao;
import moedaEstudantil.model.Vantagem;
import moedaEstudantil.repository.AlunoRepository;
import moedaEstudantil.repository.ContaRepository;
import moedaEstudantil.repository.TransacaoRepository;
import moedaEstudantil.repository.VantagemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.UUID;

@Service
public class ResgateVantagemService {

    private final AlunoRepository alunoRepository;
    private final ContaRepository contaRepository;
    private final VantagemRepository vantagemRepository;
    private final TransacaoRepository transacaoRepository;
    private final NotificacaoService notificacaoService;

    public ResgateVantagemService(AlunoRepository alunoRepository, ContaRepository contaRepository,
                                  VantagemRepository vantagemRepository, TransacaoRepository transacaoRepository,
                                  NotificacaoService notificacaoService) {
        this.alunoRepository = alunoRepository;
        this.contaRepository = contaRepository;
        this.vantagemRepository = vantagemRepository;
        this.transacaoRepository = transacaoRepository;
        this.notificacaoService = notificacaoService;
    }

    @Transactional(readOnly = true)
    public CatalogoVantagemDTO catalogo(Long alunoId) {
        Aluno aluno = buscarAluno(alunoId);
        Conta conta = contaRepository.findByUsuarioId(alunoId)
                .orElseThrow(() -> erro("O aluno nao possui conta."));
        var itens = vantagemRepository.findAtivas().stream().map(this::item).toList();
        return new CatalogoVantagemDTO(alunoId, aluno.getNome(), saldo(conta), itens);
    }

    @Transactional
    public ResgateResultadoDTO resgatar(Long alunoId, Long vantagemId) {
        Aluno aluno = buscarAluno(alunoId);
        Vantagem vantagem = vantagemRepository.findById(vantagemId)
                .filter(Vantagem::getAtiva)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vantagem nao encontrada."));
        if (vantagem.getEmpresa() == null) throw erro("A vantagem nao possui empresa responsavel.");

        Conta conta = contaRepository.findByUsuarioIdParaAtualizacao(alunoId)
                .orElseThrow(() -> erro("O aluno nao possui conta."));
        int custo = vantagem.getCustoMoedas() == null ? 0 : vantagem.getCustoMoedas();
        if (custo <= 0) throw erro("A vantagem possui custo invalido.");
        if (saldo(conta) < custo) throw erro("Saldo insuficiente para resgatar esta vantagem.");

        conta.setSaldoCorrente(saldo(conta) - custo);
        contaRepository.save(conta);
        String codigo = "CUPOM-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase();
        ResgateVantagem resgate = new ResgateVantagem();
        resgate.setConta(conta);
        resgate.setAluno(aluno);
        resgate.setVantagem(vantagem);
        resgate.setQuantidade(custo);
        resgate.setCodigoCupom(codigo);
        resgate.setTipoMovimentacao(TipoTransacao.SAIDA);
        resgate.setDataHora(new Date());
        transacaoRepository.save(resgate);
        notificacaoService.notificarResgate(aluno, vantagem, codigo);
        return new ResgateResultadoDTO(codigo, saldo(conta));
    }

    private CatalogoVantagemDTO.Item item(Vantagem v) {
        return new CatalogoVantagemDTO.Item(v.getId(), v.getNome(), v.getDescricao(), v.getFotoUrl(),
                v.getCustoMoedas(), v.getEmpresa().getId(), v.getEmpresa().getNome());
    }

    private Aluno buscarAluno(Long id) {
        return alunoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno nao encontrado."));
    }

    private int saldo(Conta conta) {
        return conta.getSaldoCorrente() == null ? 0 : conta.getSaldoCorrente();
    }

    private ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
