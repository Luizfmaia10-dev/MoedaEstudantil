package moedaEstudantil.service;

public class IaLocalIndisponivelException extends RuntimeException {

    public IaLocalIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
