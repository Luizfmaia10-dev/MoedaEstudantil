package moedaEstudantil.service;

import moedaEstudantil.model.Conta;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovimentacaoSaldoTest {

    @Test
    void consideraSaldoNuloComoZero() {
        assertEquals(0, MovimentacaoSaldo.saldo(new Conta()));
    }

    @Test
    void transfereSaldoEntreContas() {
        Conta origem = conta(10);
        Conta destino = conta(2);

        MovimentacaoSaldo.transferir(origem, destino, 4, "Saldo insuficiente.");

        assertEquals(6, origem.getSaldoCorrente());
        assertEquals(6, destino.getSaldoCorrente());
    }

    @Test
    void rejeitaTransferenciaSemSaldo() {
        Conta origem = conta(3);
        Conta destino = conta(1);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> MovimentacaoSaldo.transferir(origem, destino, 4, "Saldo insuficiente."));

        assertEquals("Saldo insuficiente.", erro.getReason());
        assertEquals(3, origem.getSaldoCorrente());
        assertEquals(1, destino.getSaldoCorrente());
    }

    private Conta conta(int saldo) {
        Conta conta = new Conta();
        conta.setSaldoCorrente(saldo);
        return conta;
    }
}
