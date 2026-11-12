package moedaEstudantil.service;

import moedaEstudantil.email.NotificacaoTemplates;
import moedaEstudantil.model.*;
import moedaEstudantil.repository.EmailPendenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificacaoServiceTest {
    private EmailPendenteRepository repository;
    private NotificacaoService service;
    private Aluno aluno;
    private Professor professor;

    @BeforeEach
    void setUp() {
        repository = mock(EmailPendenteRepository.class);
        service = new NotificacaoService(repository, new NotificacaoTemplates());
        aluno = new Aluno();
        aluno.setNome("Aluno Teste");
        aluno.setEmail("aluno@teste.com");
        professor = new Professor();
        professor.setNome("Professor Teste");
        professor.setEmail("professor@teste.com");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<EmailPendente> mensagens() {
        ArgumentCaptor<List<EmailPendente>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(repository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void agendaConfirmacaoParaProfessorEAlunoComQuantidadeEMotivo() {
        service.notificarRecebimento(aluno, professor, 25, "Participacao em aula");
        var mensagens = mensagens();
        assertEquals(List.of("aluno@teste.com", "professor@teste.com"),
                mensagens.stream().map(e -> e.mensagem().destinatario()).toList());
        assertTrue(mensagens.stream().allMatch(e -> e.mensagem().texto().contains("25")
                && e.mensagem().texto().contains("Participacao em aula")));
        assertTrue(mensagens.get(0).mensagem().texto().contains(professor.getNome()));
        assertTrue(mensagens.get(1).mensagem().texto().contains(aluno.getNome()));
        assertTrue(mensagens.stream().allMatch(e -> e.getStatus() == EmailPendente.Status.PENDENTE));
    }

    @Test
    void agendaMesmoCodigoAoAlunoEEmpresaNoResgate() {
        EmpresaParceira empresa = new EmpresaParceira();
        empresa.setNome("Cantina");
        empresa.setEmail("empresa@teste.com");
        Vantagem vantagem = new Vantagem();
        vantagem.setNome("Combo Lanche");
        vantagem.setCustoMoedas(40);
        vantagem.setEmpresa(empresa);
        service.notificarResgate(aluno, vantagem, "CUPOM-ABC12345");
        var mensagens = mensagens();
        assertEquals(List.of("aluno@teste.com", "empresa@teste.com"),
                mensagens.stream().map(e -> e.mensagem().destinatario()).toList());
        assertTrue(mensagens.stream().allMatch(e -> e.mensagem().texto().contains("CUPOM-ABC12345")
                && e.mensagem().texto().contains("Combo Lanche")));
        assertNotEquals(mensagens.get(0).getId(), mensagens.get(1).getId());
    }
}
