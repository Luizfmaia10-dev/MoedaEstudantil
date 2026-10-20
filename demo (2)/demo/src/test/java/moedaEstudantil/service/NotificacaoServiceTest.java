package moedaEstudantil.service;

import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.EmpresaParceira;
import moedaEstudantil.model.Professor;
import moedaEstudantil.model.Vantagem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotificacaoServiceTest {

    private JavaMailSender mailSender;
    private NotificacaoService service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        service = new NotificacaoService(mailSender, "nao-responda@moeda.test");
    }

    @Test
    void notificaAlunoQuandoRecebeMoedas() {
        Aluno aluno = new Aluno();
        aluno.setNome("Aluno Teste");
        aluno.setEmail("aluno@teste.com");
        Professor professor = new Professor();
        professor.setNome("Glender Bras");

        service.notificarRecebimento(aluno, professor, 25, "Participacao em aula");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage mensagem = captor.getValue();
        assertEquals("aluno@teste.com", mensagem.getTo()[0]);
        assertTrue(mensagem.getText().contains("25"));
        assertTrue(mensagem.getText().contains("Participacao em aula"));
    }

    @Test
    void enviaMesmoCodigoAoAlunoEEmpresaNoResgate() {
        Aluno aluno = new Aluno();
        aluno.setNome("Aluno Teste");
        aluno.setEmail("aluno@teste.com");
        EmpresaParceira empresa = new EmpresaParceira();
        empresa.setNome("Cantina");
        empresa.setEmail("empresa@teste.com");
        Vantagem vantagem = new Vantagem();
        vantagem.setNome("Combo Lanche");
        vantagem.setEmpresa(empresa);

        service.notificarResgate(aluno, vantagem, "CUPOM-ABC12345");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(captor.capture());
        List<SimpleMailMessage> mensagens = captor.getAllValues();
        assertEquals("aluno@teste.com", mensagens.get(0).getTo()[0]);
        assertEquals("empresa@teste.com", mensagens.get(1).getTo()[0]);
        assertTrue(mensagens.stream().allMatch(m -> m.getText().contains("CUPOM-ABC12345")));
    }
}
