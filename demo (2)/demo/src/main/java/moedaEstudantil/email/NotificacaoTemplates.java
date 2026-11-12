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
                new EmailMensagem(aluno.getEmail(), "Voc? recebeu moedas estudantis",
                        "Ol?, " + aluno.getNome() + "!\n\nVoc? recebeu " + quantidade
                                + " moedas do professor " + professor.getNome() + ".\nMotivo: " + motivo),
                new EmailMensagem(professor.getEmail(), "Confirma??o do envio de moedas estudantis",
                        "Ol?, professor(a) " + professor.getNome() + "!\n\nSeu envio de " + quantidade
                                + " moedas para " + aluno.getNome() + " foi confirmado.\nMotivo: " + motivo));
    }

    public List<EmailMensagem> resgate(Aluno aluno, Vantagem vantagem, String codigo) {
        var empresa = vantagem.getEmpresa();
        return List.of(
                new EmailMensagem(aluno.getEmail(), "Cupom da vantagem resgatada",
                        "Ol?, " + aluno.getNome() + "!\n\nSeu resgate de " + vantagem.getNome()
                                + " foi confirmado na empresa " + empresa.getNome()
                                + ".\nCusto: " + vantagem.getCustoMoedas() + " moedas."
                                + "\nC?digo para a troca presencial: " + codigo
                                + "\nApresente este cupom ? empresa para confer?ncia."),
                new EmailMensagem(empresa.getEmail(), "Novo resgate de vantagem",
                        "Ol?, " + empresa.getNome() + "!\n\nA vantagem " + vantagem.getNome()
                                + " foi resgatada por " + aluno.getNome()
                                + ".\nCusto: " + vantagem.getCustoMoedas() + " moedas."
                                + "\nC?digo para confer?ncia: " + codigo
                                + "\nConfira este c?digo com o cupom apresentado pelo aluno na troca presencial."));
    }
}
