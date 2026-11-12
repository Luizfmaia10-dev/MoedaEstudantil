package moedaEstudantil.email;

import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.Professor;
import moedaEstudantil.model.Vantagem;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class NotificacaoTemplates {
    public List<EmailMensagem> distribuicao(Aluno aluno, Professor professor, int quantidade, String motivo) {
        return List.of(
                new EmailMensagem(aluno.getEmail(), "Você recebeu moedas estudantis",
                        "Olá, " + aluno.getNome() + "!\n\nVocê recebeu " + quantidade
                                + " moedas do professor " + professor.getNome() + ".\nMotivo: " + motivo),
                new EmailMensagem(professor.getEmail(), "Confirmação do envio de moedas estudantis",
                        "Olá, professor(a) " + professor.getNome() + "!\n\nSeu envio de " + quantidade
                                + " moedas para " + aluno.getNome() + " foi confirmado.\nMotivo: " + motivo));
    }

    public List<EmailMensagem> resgate(Aluno aluno, Vantagem vantagem, String codigo) {
        var empresa = vantagem.getEmpresa();
        return List.of(
                new EmailMensagem(aluno.getEmail(), "Cupom da vantagem resgatada",
                        "Olá, " + aluno.getNome() + "!\n\nSeu resgate de " + vantagem.getNome()
                                + " foi confirmado na empresa " + empresa.getNome()
                                + ".\nCusto: " + vantagem.getCustoMoedas() + " moedas."
                                + "\nCódigo para a troca presencial: " + codigo
                                + "\nApresente este cupom à empresa para conferência."),
                new EmailMensagem(empresa.getEmail(), "Novo resgate de vantagem",
                        "Olá, " + empresa.getNome() + "!\n\nA vantagem " + vantagem.getNome()
                                + " foi resgatada por " + aluno.getNome()
                                + ".\nCusto: " + vantagem.getCustoMoedas() + " moedas."
                                + "\nCódigo para conferência: " + codigo
                                + "\nConfira este código com o cupom apresentado pelo aluno na troca presencial."));
    }
}
