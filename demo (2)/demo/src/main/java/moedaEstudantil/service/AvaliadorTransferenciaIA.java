package moedaEstudantil.service;

public interface AvaliadorTransferenciaIA {

    Resultado avaliar(Contexto contexto);

    enum Decisao { APROVAR, RECUSAR, PENDENTE }

    record Contexto(String criterios, int quantidade, String justificativa,
                    String remetente, String destinatario) {
    }

    record Resultado(Decisao decisao, double confianca, String explicacao) {
    }
}
