package moedaEstudantil.email;

public interface EmailSender {
    void enviar(EmailMensagem mensagem, String chaveIdempotencia);
}
