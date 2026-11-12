package moedaEstudantil.email;

import moedaEstudantil.model.*;
import moedaEstudantil.repository.EmailPendenteRepository;
import moedaEstudantil.service.NotificacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"app.mail.delivery-enabled=false", "app.mail.provider=smtp", "spring.jpa.show-sql=false"})
class EmailOutboxIntegrationTest {
    @Autowired NotificacaoService notificacao;
    @Autowired EmailPendenteRepository repository;
    @Autowired PlatformTransactionManager transactionManager;
    private TransactionTemplate transaction;
    private Aluno aluno;
    private Professor professor;

    @BeforeEach void setUp() {
        repository.deleteAll();
        transaction = new TransactionTemplate(transactionManager);
        aluno = new Aluno(); aluno.setEmail("aluno@teste.com"); aluno.setNome("Aluno");
        professor = new Professor(); professor.setEmail("professor@teste.com"); professor.setNome("Professor");
    }

    @Test void rollbackDaOperacaoDescartaEmails() {
        transaction.executeWithoutResult(tx -> {
            notificacao.notificarRecebimento(aluno, professor, 10, "Participacao");
            assertEquals(2, repository.count());
            tx.setRollbackOnly();
        });
        assertEquals(0, repository.count());
    }

    @Test void commitDisponibilizaEmailsEEnviadosNaoVoltamParaFila() {
        transaction.executeWithoutResult(tx -> notificacao.notificarRecebimento(aluno, professor, 10, "Participacao"));
        assertEquals(2, repository.count());
        transaction.executeWithoutResult(tx -> {
            var prontos = repository.buscarProntos(EmailPendente.Status.PENDENTE, Instant.now(), PageRequest.of(0, 10));
            assertEquals(2, prontos.size());
            prontos.get(0).confirmarEnvio(Instant.now());
        });
        transaction.executeWithoutResult(tx -> assertEquals(1,
                repository.buscarProntos(EmailPendente.Status.PENDENTE, Instant.now(), PageRequest.of(0, 10)).size()));
    }

    @Test void falhaFuturaSoVoltaAposIntervalo() {
        notificacao.notificarRecebimento(aluno, professor, 10, "Participacao");
        transaction.executeWithoutResult(tx -> {
            for (var email : repository.findAll()) email.registrarFalha(Instant.now(), "Falha");
        });
        transaction.executeWithoutResult(tx -> {
            assertTrue(repository.buscarProntos(EmailPendente.Status.PENDENTE, Instant.now(), PageRequest.of(0, 10)).isEmpty());
            assertEquals(2, repository.buscarProntos(EmailPendente.Status.PENDENTE, Instant.now().plusSeconds(31), PageRequest.of(0, 10)).size());
        });
    }
}
