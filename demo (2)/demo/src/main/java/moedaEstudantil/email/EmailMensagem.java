package moedaEstudantil.email;

/** Conteudo imutavel, independente do provedor de entrega. */
public record EmailMensagem(String destinatario, String assunto, String texto) {}
