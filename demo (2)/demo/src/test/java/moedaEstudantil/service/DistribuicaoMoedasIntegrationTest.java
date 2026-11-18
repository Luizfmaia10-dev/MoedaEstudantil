package moedaEstudantil.service;

import moedaEstudantil.dto.DistribuicaoMoedasRequest;
import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.Conta;
import moedaEstudantil.model.Instituicao;
import moedaEstudantil.model.Professor;
import moedaEstudantil.repository.AlunoRepository;
import moedaEstudantil.repository.ContaRepository;
import moedaEstudantil.repository.EmailPendenteRepository;
import moedaEstudantil.repository.InstituicaoRepository;
import moedaEstudantil.repository.ProfessorRepository;
import moedaEstudantil.repository.TransacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "app.mail.delivery-enabled=false",
        "app.mail.provider=smtp",
        "spring.jpa.show-sql=false"
})
class DistribuicaoMoedasIntegrationTest {

    @Autowired DistribuicaoMoedasService service;
    @Autowired InstituicaoRepository instituicoes;
    @Autowired ProfessorRepository professores;
    @Autowired AlunoRepository alunos;
    @Autowired ContaRepository contas;
    @Autowired TransacaoRepository transacoes;
    @Autowired EmailPendenteRepository emails;

    private Professor professor;
    private Aluno aluno;

    @BeforeEach
    void setUp() {
        emails.deleteAll();
        transacoes.deleteAll();
        contas.deleteAll();
        alunos.deleteAll();
        professores.deleteAll();
        instituicoes.deleteAll();

        Instituicao instituicao = new Instituicao();
        instituicao.setNome("PUC Minas");
        instituicao = instituicoes.save(instituicao);

        professor = new Professor();
        professor.setNome("Professor Teste");
        professor.setEmail("luiznxmaia@gmail.com");
        professor.setSenha("senha123");
        professor.setInstituicao(instituicao);
        professor = professores.save(professor);

        aluno = new Aluno();
        aluno.setNome("Aluno Teste");
        aluno.setEmail("lf.cunhamaia@gmail.com");
        aluno.setSenha("senha123");
        aluno.setInstituicao(instituicao);
        aluno = alunos.save(aluno);

        contas.save(conta(professor, 100));
        contas.save(conta(aluno, 20));
    }

    @Test
    void distribuicaoPersisteOsDoisLancamentosEAgendaProfessorEAluno() {
        service.distribuir(professor.getId(),
                new DistribuicaoMoedasRequest(aluno.getId(), 10, "Participacao em aula"));

        assertEquals(2, transacoes.count());
        assertEquals(List.of("lf.cunhamaia@gmail.com", "luiznxmaia@gmail.com"),
                emails.findAll().stream().map(email -> email.mensagem().destinatario()).sorted().toList());
        assertEquals(90, contas.findByUsuarioId(professor.getId()).orElseThrow().getSaldoCorrente());
        assertEquals(30, contas.findByUsuarioId(aluno.getId()).orElseThrow().getSaldoCorrente());
    }

    private Conta conta(moedaEstudantil.model.Usuario usuario, int saldo) {
        Conta conta = new Conta();
        conta.setUsuario(usuario);
        conta.setSaldoCorrente(saldo);
        return conta;
    }
}
