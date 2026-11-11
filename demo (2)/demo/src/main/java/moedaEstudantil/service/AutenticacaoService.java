package moedaEstudantil.service;

import moedaEstudantil.dto.UsuarioAutenticadoDTO;
import moedaEstudantil.model.Aluno;
import moedaEstudantil.model.EmpresaParceira;
import moedaEstudantil.model.Professor;
import moedaEstudantil.model.Usuario;
import moedaEstudantil.repository.AlunoRepository;
import moedaEstudantil.repository.EmpresaParceiraRepository;
import moedaEstudantil.repository.ProfessorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AutenticacaoService {
    private final AlunoRepository alunos;
    private final ProfessorRepository professores;
    private final EmpresaParceiraRepository empresas;
    private final SenhaService senhas;

    public AutenticacaoService(AlunoRepository alunos, ProfessorRepository professores,
                               EmpresaParceiraRepository empresas, SenhaService senhas) {
        this.alunos = alunos;
        this.professores = professores;
        this.empresas = empresas;
        this.senhas = senhas;
    }

    @Transactional
    public UsuarioAutenticadoDTO autenticar(String email, String senha, String perfil) {
        if (email == null || email.isBlank() || senha == null || senha.isBlank()
                || perfil == null || perfil.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe e-mail, senha e perfil.");
        }
        String normalizado = email.trim().toLowerCase(Locale.ROOT);
        String perfilNormalizado = perfil.trim().toUpperCase(Locale.ROOT);
        return switch (perfilNormalizado) {
            case "ALUNO" -> alunos.findByEmailIgnoreCase(normalizado)
                    .map(usuario -> validarAluno(usuario, senha)).orElseThrow(this::credenciaisInvalidas);
            case "PROFESSOR" -> professores.findByEmailIgnoreCase(normalizado)
                    .map(usuario -> validarProfessor(usuario, senha)).orElseThrow(this::credenciaisInvalidas);
            case "EMPRESA" -> empresas.findByEmailIgnoreCase(normalizado)
                    .map(usuario -> validarEmpresa(usuario, senha)).orElseThrow(this::credenciaisInvalidas);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Perfil inválido.");
        };
    }

    private UsuarioAutenticadoDTO validarAluno(Aluno usuario, String senha) {
        validarSenha(usuario, senha, () -> alunos.save(usuario));
        return new UsuarioAutenticadoDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(), "ALUNO");
    }

    private UsuarioAutenticadoDTO validarProfessor(Professor usuario, String senha) {
        validarSenha(usuario, senha, () -> professores.save(usuario));
        return new UsuarioAutenticadoDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(), "PROFESSOR");
    }

    private UsuarioAutenticadoDTO validarEmpresa(EmpresaParceira usuario, String senha) {
        validarSenha(usuario, senha, () -> empresas.save(usuario));
        return new UsuarioAutenticadoDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(), "EMPRESA");
    }

    private void validarSenha(Usuario usuario, String senha, Runnable salvar) {
        if (!senhas.corresponde(senha, usuario.getSenha())) throw credenciaisInvalidas();
        if (!senhas.estaCodificada(usuario.getSenha())) {
            usuario.setSenha(senhas.codificar(senha));
            salvar.run();
        }
    }

    private ResponseStatusException credenciaisInvalidas() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail, senha ou perfil inválido.");
    }
}
