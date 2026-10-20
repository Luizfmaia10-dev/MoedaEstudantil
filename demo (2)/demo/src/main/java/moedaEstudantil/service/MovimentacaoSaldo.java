package moedaEstudantil.service;

import moedaEstudantil.model.Conta;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Regras puras para leitura e movimentacao do saldo de contas. */
public final class MovimentacaoSaldo {

    private MovimentacaoSaldo() {
    }

    public static int saldo(Conta conta) {
        return conta.getSaldoCorrente() == null ? 0 : conta.getSaldoCorrente();
    }

    public static void transferir(Conta origem, Conta destino, int quantidade, String mensagemSaldoInsuficiente) {
        if (quantidade <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A quantidade deve ser maior que zero.");
        }
        int saldoOrigem = saldo(origem);
        if (saldoOrigem < quantidade) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagemSaldoInsuficiente);
        }
        origem.setSaldoCorrente(saldoOrigem - quantidade);
        destino.setSaldoCorrente(saldo(destino) + quantidade);
    }

    public static void debitar(Conta conta, int quantidade, String mensagemSaldoInsuficiente) {
        if (quantidade <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A quantidade deve ser maior que zero.");
        }
        int saldoAtual = saldo(conta);
        if (saldoAtual < quantidade) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagemSaldoInsuficiente);
        }
        conta.setSaldoCorrente(saldoAtual - quantidade);
    }
}
