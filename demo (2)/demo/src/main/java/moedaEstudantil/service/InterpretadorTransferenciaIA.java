package moedaEstudantil.service;

public interface InterpretadorTransferenciaIA {

    record Rascunho(String destinatario, String professor, Integer quantidade, String justificativa) {
    }

    record Intencao(String destinatario, String professor, Integer quantidade, String justificativa) {
    }

    Intencao interpretar(String mensagem, Rascunho rascunho);
}
