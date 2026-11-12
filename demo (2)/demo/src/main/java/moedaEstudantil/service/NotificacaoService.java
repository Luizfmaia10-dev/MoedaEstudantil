package moedaEstudantil.service;

import moedaEstudantil.email.EmailMensagem;
import moedaEstudantil.email.NotificacaoTemplates;
import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.EmailPendente;
import moedaEstudantil.model.Professor;
import moedaEstudantil.model.Vantagem;
import moedaEstudantil.repository.EmailPendenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NotificacaoService {
    private final EmailPendenteRepository repository;
    private final NotificacaoTemplates templates;

    public NotificacaoService(EmailPendenteRepository repository, NotificacaoTemplates templates) {
        this.repository = repository;
        this.templates = templates;
    }

    @Transactional
    public void notificarRecebimento(Aluno aluno, Professor professor, int quantidade, String motivo) {
        agendar(templates.distribuicao(aluno, professor, quantidade, motivo));
    }

    @Transactional
    public void notificarResgate(Aluno aluno, Vantagem vantagem, String codigo) {
        agendar(templates.resgate(aluno, vantagem, codigo));
    }

    private void agendar(List<EmailMensagem> mensagens) {
        repository.saveAll(mensagens.stream().map(EmailPendente::new).toList());
    }
}
