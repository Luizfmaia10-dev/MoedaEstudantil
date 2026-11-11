# Landing SaaS e autenticação persistida - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transformar a entrada do Sistema de Moeda Estudantil em uma landing SaaS futurista com login para aluno, professor e empresa, autenticando exclusivamente contra o banco de dados.

**Architecture:** O frontend permanece em HTML, CSS e JavaScript sem framework, com módulos IIFE/CommonJS testáveis para sessão, autenticação e rotas. O Spring Boot recebe um serviço de autenticação por perfil, exposto em `/api/auth/login`, e passa a codificar novas senhas com BCrypt enquanto migra credenciais legadas no primeiro login válido. A landing pública, o painel pós-login e os portais de cadastro compartilham a mesma sessão mínima em `sessionStorage`.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Spring MVC, Spring Data JPA, MySQL, H2, BCrypt, JUnit 5, Mockito, Node.js `node:test`, HTML5, CSS3 e JavaScript ES2020.

**Spec:** `docs/superpowers/specs/2026-10-20-landing-saas-autenticacao-design.md`

## Global Constraints

- Manter `frontend/` como fonte canônica; sincronizar para `demo (2)/demo/src/main/resources/static/` somente após os testes da fonte.
- MySQL/JPA é a única fonte oficial de usuários; não criar fallback de cadastro ou login em `localStorage`.
- Guardar em `sessionStorage` somente `id`, `nome`, `email` e `perfil`.
- Não expor senha, CPF, RG, CNPJ ou entidade JPA na resposta de autenticação.
- Aluno e empresa podem se cadastrar; professor é somente pré-cadastrado.
- Usar BCrypt para novas senhas e migrar texto simples legado após o primeiro login válido.
- Não ativar filtros globais do Spring Security, JWT ou OAuth nesta entrega.
- Construir a interface mobile-first, com alvos mínimos de 44x44px, contraste WCAG AA e suporte a `prefers-reduced-motion`.
- Preservar o teste Java não rastreado `demo (2)/demo/src/test/java/moedaEstudantil/service/CreditoSemestralProfessorServiceTest.java`; ele não pertence a esta implementação.

## File Structure

### Backend

- `dto/AuthPerfil.java`: enum dos três perfis aceitos pelo contrato.
- `dto/LoginRequest.java`: entrada imutável do login.
- `dto/AuthResponse.java`: resposta mínima e segura.
- `service/PasswordService.java`: BCrypt, compatibilidade e detecção de senha legada.
- `service/AuthService.java`: seleção do repositório, validação, migração e montagem da resposta.
- `controller/AuthController.java`: endpoint HTTP e mapeamento de credencial inválida para `401`.
- Repositórios de aluno, professor e empresa: busca case-insensitive por e-mail.
- Controllers de aluno e empresa: codificação de senha e rejeição de duplicidades antes de persistir.

### Frontend

- `js/core/session.js`: contrato único da sessão do navegador.
- `js/core/routes.js`: catálogo de ações e URLs por perfil.
- `js/auth-api.js`: cliente do endpoint `/api/auth/login`.
- `js/landing.js`: comportamento da landing e do diálogo de acesso.
- `js/painel.js`: renderização segura do painel conforme a sessão.
- `css/landing.css`: identidade pública isolada das telas legadas.
- `css/painel.css`: layout do painel pós-login.
- `index.html`: landing comercial.
- `aluno.html`: portal de cadastro/perfil do aluno movido da entrada antiga.
- `painel.html`: hub de funcionalidades do usuário autenticado.
- `api.js` e `empresa-api.js`: CRUD e login exclusivamente via backend.

---

### Task 1: Serviço de senha e autenticação por perfil

**Files:**
- Modify: `demo (2)/demo/pom.xml`
- Create: `demo (2)/demo/src/main/java/moedaEstudantil/dto/AuthPerfil.java`
- Create: `demo (2)/demo/src/main/java/moedaEstudantil/dto/LoginRequest.java`
- Create: `demo (2)/demo/src/main/java/moedaEstudantil/dto/AuthResponse.java`
- Create: `demo (2)/demo/src/main/java/moedaEstudantil/service/PasswordService.java`
- Create: `demo (2)/demo/src/main/java/moedaEstudantil/service/AuthService.java`
- Modify: `demo (2)/demo/src/main/java/moedaEstudantil/repository/AlunoRepository.java`
- Modify: `demo (2)/demo/src/main/java/moedaEstudantil/repository/ProfessorRepository.java`
- Modify: `demo (2)/demo/src/main/java/moedaEstudantil/repository/EmpresaParceiraRepository.java`
- Test: `demo (2)/demo/src/test/java/moedaEstudantil/service/AuthServiceTest.java`

**Interfaces:**
- Consumes: entidades `Aluno`, `Professor` e `EmpresaParceira` e seus repositórios JPA.
- Produces: `AuthService.autenticar(LoginRequest): AuthResponse`, `PasswordService.encode(String): String`, `PasswordService.matches(String, String): boolean` e `PasswordService.isLegacy(String): boolean`.

- [ ] **Step 1: Escrever os testes falhos do serviço**

Criar testes Mockito que nomeiem as quebras reais: perfil errado não pode autenticar, senha errada não pode autenticar, a resposta não carrega senha e senha legada precisa ser migrada.

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock AlunoRepository alunoRepository;
    @Mock ProfessorRepository professorRepository;
    @Mock EmpresaParceiraRepository empresaRepository;

    private PasswordService passwordService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordService();
        authService = new AuthService(
                alunoRepository, professorRepository, empresaRepository, passwordService);
    }

    @Test
    void autenticaAlunoSemExporCredenciais() {
        Aluno aluno = new Aluno();
        aluno.setId(4L);
        aluno.setNome("Aluno Teste");
        aluno.setEmail("aluno@puc.br");
        aluno.setSenha(passwordService.encode("senha123"));
        when(alunoRepository.findByEmailIgnoreCase("aluno@puc.br"))
                .thenReturn(Optional.of(aluno));

        AuthResponse response = authService.autenticar(
                new LoginRequest(" aluno@puc.br ", "senha123", AuthPerfil.ALUNO));

        assertEquals(4L, response.id());
        assertEquals("Aluno Teste", response.nome());
        assertEquals("aluno@puc.br", response.email());
        assertEquals(AuthPerfil.ALUNO, response.perfil());
    }

    @Test
    void rejeitaSenhaIncorreta() {
        Aluno aluno = new Aluno();
        aluno.setSenha(passwordService.encode("correta"));
        when(alunoRepository.findByEmailIgnoreCase("aluno@puc.br"))
                .thenReturn(Optional.of(aluno));

        assertThrows(AuthService.CredenciaisInvalidasException.class,
                () -> authService.autenticar(
                        new LoginRequest("aluno@puc.br", "errada", AuthPerfil.ALUNO)));
    }

    @Test
    void rejeitaRequisicaoSemPerfilComoEntradaInvalida() {
        assertThrows(AuthService.RequisicaoInvalidaException.class,
                () -> authService.autenticar(
                        new LoginRequest("aluno@puc.br", "senha123", null)));
        verifyNoInteractions(alunoRepository, professorRepository, empresaRepository);
    }

    @Test
    void naoProcuraAlunoQuandoPerfilInformadoForProfessor() {
        when(professorRepository.findByEmailIgnoreCase("aluno@puc.br"))
                .thenReturn(Optional.empty());

        assertThrows(AuthService.CredenciaisInvalidasException.class,
                () -> authService.autenticar(
                        new LoginRequest("aluno@puc.br", "senha123", AuthPerfil.PROFESSOR)));
        verifyNoInteractions(alunoRepository);
    }

    @Test
    void migraSenhaLegadaAposLoginValido() {
        Professor professor = new Professor();
        professor.setId(1L);
        professor.setNome("Glender Brás");
        professor.setEmail("professor@puc.br");
        professor.setSenha("senha123");
        when(professorRepository.findByEmailIgnoreCase("professor@puc.br"))
                .thenReturn(Optional.of(professor));

        authService.autenticar(
                new LoginRequest("professor@puc.br", "senha123", AuthPerfil.PROFESSOR));

        assertTrue(professor.getSenha().startsWith("$2"));
        verify(professorRepository).save(professor);
    }

    @Test
    void autenticaEmpresaNoRepositorioDoPerfil() {
        EmpresaParceira empresa = new EmpresaParceira();
        empresa.setId(5L);
        empresa.setNome("Cantina da PUC");
        empresa.setEmail("contato@cantina.com.br");
        empresa.setSenha(passwordService.encode("senha123"));
        when(empresaRepository.findByEmailIgnoreCase("contato@cantina.com.br"))
                .thenReturn(Optional.of(empresa));

        AuthResponse response = authService.autenticar(
                new LoginRequest("contato@cantina.com.br", "senha123", AuthPerfil.EMPRESA));

        assertEquals(AuthPerfil.EMPRESA, response.perfil());
        verifyNoInteractions(alunoRepository, professorRepository);
    }
}
```

- [ ] **Step 2: Executar o teste e confirmar a falha esperada**

Run:

```powershell
Set-Location 'demo (2)/demo'
.\mvnw.cmd -Dtest=AuthServiceTest test
```

Expected: `COMPILATION ERROR` porque os DTOs, `PasswordService`, `AuthService` e métodos de repositório ainda não existem.

- [ ] **Step 3: Adicionar BCrypt e os contratos de autenticação**

Adicionar ao `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

Criar os contratos:

```java
public enum AuthPerfil { ALUNO, PROFESSOR, EMPRESA }

public record LoginRequest(String email, String senha, AuthPerfil perfil) {}

public record AuthResponse(Long id, String nome, String email, AuthPerfil perfil) {}
```

Criar `PasswordService` como bean sem filtros web:

```java
@Service
public class PasswordService {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String encode(String raw) {
        return encoder.encode(raw);
    }

    public boolean matches(String raw, String stored) {
        if (raw == null || stored == null) return false;
        return isLegacy(stored) ? MessageDigest.isEqual(
                raw.getBytes(StandardCharsets.UTF_8),
                stored.getBytes(StandardCharsets.UTF_8))
                : encoder.matches(raw, stored);
    }

    public boolean isLegacy(String stored) {
        return stored != null && !stored.matches("^\\$2[aby]\\$.*");
    }
}
```

- [ ] **Step 4: Implementar consultas e serviço mínimo**

Adicionar `Optional<T> findByEmailIgnoreCase(String email)` aos três repositórios. Implementar `AuthService` com `switch` explícito e mensagem única:

```java
@Service
public class AuthService {
    public static final class CredenciaisInvalidasException extends RuntimeException {
        public CredenciaisInvalidasException() {
            super("E-mail, senha ou perfil inválido.");
        }
    }

    public static final class RequisicaoInvalidaException extends RuntimeException {
        public RequisicaoInvalidaException() {
            super("Informe e-mail, senha e perfil.");
        }
    }

    public AuthResponse autenticar(LoginRequest request) {
        if (request == null || request.perfil() == null
                || request.email() == null || request.email().isBlank()
                || request.senha() == null || request.senha().isBlank()) {
            throw new RequisicaoInvalidaException();
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Usuario usuario = buscar(email, request.perfil())
                .orElseThrow(CredenciaisInvalidasException::new);
        if (!passwordService.matches(request.senha(), usuario.getSenha())) {
            throw new CredenciaisInvalidasException();
        }
        if (passwordService.isLegacy(usuario.getSenha())) {
            usuario.setSenha(passwordService.encode(request.senha()));
            salvar(usuario, request.perfil());
        }
        return new AuthResponse(usuario.getId(), nome(usuario), usuario.getEmail(), request.perfil());
    }
}
```

Os helpers `buscar`, `salvar` e `nome` devem usar somente os três tipos conhecidos e lançar `CredenciaisInvalidasException` para qualquer combinação impossível.

- [ ] **Step 5: Executar os testes e confirmar sucesso**

Run:

```powershell
Set-Location 'demo (2)/demo'
.\mvnw.cmd -Dtest=AuthServiceTest test
```

Expected: `Tests run: 6, Failures: 0, Errors: 0`.

- [ ] **Step 6: Commit**

```powershell
git add -- 'demo (2)/demo/pom.xml' 'demo (2)/demo/src/main/java/moedaEstudantil/dto' 'demo (2)/demo/src/main/java/moedaEstudantil/service/AuthService.java' 'demo (2)/demo/src/main/java/moedaEstudantil/service/PasswordService.java' 'demo (2)/demo/src/main/java/moedaEstudantil/repository' 'demo (2)/demo/src/test/java/moedaEstudantil/service/AuthServiceTest.java'
git commit -m "feat(auth): validar usuarios persistidos por perfil"
```

---

### Task 2: Endpoint HTTP e persistência segura de novos cadastros

**Files:**
- Create: `demo (2)/demo/src/main/java/moedaEstudantil/controller/AuthController.java`
- Modify: `demo (2)/demo/src/main/java/moedaEstudantil/controller/AlunoController.java`
- Modify: `demo (2)/demo/src/main/java/moedaEstudantil/controller/EmpresaParceiraController.java`
- Test: `demo (2)/demo/src/test/java/moedaEstudantil/controller/AuthControllerTest.java`
- Test: `demo (2)/demo/src/test/java/moedaEstudantil/controller/CadastroSenhaTest.java`

**Interfaces:**
- Consumes: `AuthService.autenticar(LoginRequest)` e `PasswordService.encode(String)` da Task 1.
- Produces: `POST /api/auth/login`, cadastro de aluno/empresa com senha BCrypt e erros `409` para duplicidade.

- [ ] **Step 1: Escrever o teste falho do contrato HTTP**

```java
@WebMvcTest(AuthController.class)
class AuthControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AuthService authService;

    @Test
    void devolveRespostaSeguraAoAutenticar() throws Exception {
        when(authService.autenticar(any())).thenReturn(
                new AuthResponse(4L, "Aluno Teste", "aluno@puc.br", AuthPerfil.ALUNO));

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"aluno@puc.br","senha":"senha123","perfil":"ALUNO"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.perfil").value("ALUNO"))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void devolve401ComMensagemGenericaParaCredenciaisInvalidas() throws Exception {
        when(authService.autenticar(any()))
                .thenThrow(new AuthService.CredenciaisInvalidasException());

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"desconhecido@puc.br","senha":"x","perfil":"ALUNO"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail, senha ou perfil inválido."));
    }

    @Test
    void devolve400QuandoFaltamCamposObrigatorios() throws Exception {
        when(authService.autenticar(any()))
                .thenThrow(new AuthService.RequisicaoInvalidaException());

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"aluno@puc.br\",\"senha\":\"senha123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Informe e-mail, senha e perfil."));
    }

    @Test
    void devolve503QuandoPersistenciaEstaIndisponivel() throws Exception {
        when(authService.autenticar(any()))
                .thenThrow(new DataAccessResourceFailureException("banco offline"));

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"aluno@puc.br","senha":"senha123","perfil":"ALUNO"}
                        """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Serviço temporariamente indisponível."));
    }
}
```

- [ ] **Step 2: Executar o teste HTTP e confirmar a falha**

Run:

```powershell
Set-Location 'demo (2)/demo'
.\mvnw.cmd -Dtest=AuthControllerTest test
```

Expected: FAIL porque `AuthController` não existe.

- [ ] **Step 3: Implementar o endpoint e o erro estável**

```java
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.autenticar(request);
    }

    @ExceptionHandler(AuthService.CredenciaisInvalidasException.class)
    public ResponseEntity<Map<String, String>> credenciaisInvalidas(
            AuthService.CredenciaisInvalidasException error) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", error.getMessage()));
    }

    @ExceptionHandler(AuthService.RequisicaoInvalidaException.class)
    public ResponseEntity<Map<String, String>> requisicaoInvalida(
            AuthService.RequisicaoInvalidaException error) {
        return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> persistenciaIndisponivel(DataAccessException error) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Serviço temporariamente indisponível."));
    }
}
```

- [ ] **Step 4: Escrever testes falhos para hash e duplicidade no cadastro**

Instanciar os controllers com repositórios Mockito e `PasswordService` real. Verificar o estado da entidade entregue ao `save`, não o mock:

```java
@Test
void cadastroDeAlunoPersisteHashEmVezDaSenhaRecebida() {
    AlunoDTO dto = alunoValido();
    dto.setSenha("senha123");
    when(alunoRepository.findByEmailIgnoreCase(dto.getEmail())).thenReturn(Optional.empty());
    when(alunoRepository.findByCpf(dto.getCpf())).thenReturn(Optional.empty());
    when(alunoRepository.save(any())).thenAnswer(invocation -> {
        Aluno aluno = invocation.getArgument(0);
        aluno.setId(10L);
        return aluno;
    });

    alunoController.criar(dto);

    ArgumentCaptor<Aluno> captor = ArgumentCaptor.forClass(Aluno.class);
    verify(alunoRepository).save(captor.capture());
    assertNotEquals("senha123", captor.getValue().getSenha());
    assertTrue(passwordService.matches("senha123", captor.getValue().getSenha()));
}

@Test
void cadastroDeEmpresaRecusaEmailDuplicado() {
    when(empresaRepository.findByEmailIgnoreCase("contato@empresa.com"))
            .thenReturn(Optional.of(new EmpresaParceira()));

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> empresaController.cadastrar(empresaValida()));

    assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    verify(empresaRepository, never()).save(any());
}

@Test
void atualizacaoDeEmpresaCodificaNovaSenha() {
    EmpresaParceira existente = new EmpresaParceira();
    existente.setId(5L);
    existente.setSenha(passwordService.encode("senha-antiga"));
    when(empresaRepository.findById(5L)).thenReturn(Optional.of(existente));
    when(empresaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    EmpresaParceiraDTO dto = empresaValida();
    dto.setSenha("senha-nova");

    empresaController.atualizar(5L, dto);

    assertTrue(passwordService.matches("senha-nova", existente.getSenha()));
    assertNotEquals("senha-nova", existente.getSenha());
}
```

Como os controllers possuem implementações independentes, incluir também:

```java
@Test
void atualizacaoDeAlunoCodificaNovaSenha() {
    Aluno existente = new Aluno();
    existente.setId(4L);
    existente.setSenha(passwordService.encode("senha-antiga"));
    when(alunoRepository.findById(4L)).thenReturn(Optional.of(existente));
    when(alunoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    AlunoDTO dto = alunoValido();
    dto.setSenha("senha-nova");

    alunoController.atualizar(4L, dto);

    assertTrue(passwordService.matches("senha-nova", existente.getSenha()));
    assertNotEquals("senha-nova", existente.getSenha());
}
```

- [ ] **Step 5: Executar os testes de cadastro e confirmar a falha**

Run:

```powershell
Set-Location 'demo (2)/demo'
.\mvnw.cmd -Dtest=CadastroSenhaTest test
```

Expected: FAIL porque os controllers ainda persistem a senha em texto simples e não rejeitam duplicidades.

- [ ] **Step 6: Codificar senhas e validar duplicidades nos controllers**

Injetar `PasswordService` nos construtores. Em criação, validar e codificar:

```java
if (alunoRepository.findByEmailIgnoreCase(dto.getEmail().trim()).isPresent()) {
    throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
}
if (alunoRepository.findByCpf(dto.getCpf()).isPresent()) {
    throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado.");
}
aluno.setEmail(dto.getEmail().trim().toLowerCase(Locale.ROOT));
aluno.setSenha(passwordService.encode(dto.getSenha()));
```

Aplicar a mesma regra a e-mail/CNPJ de empresa. Em atualização, codificar somente quando uma senha não vazia for recebida. Não adicionar senha aos métodos `toDTO`.

- [ ] **Step 7: Executar os testes HTTP e de cadastro**

Run:

```powershell
Set-Location 'demo (2)/demo'
.\mvnw.cmd -Dtest=AuthControllerTest,CadastroSenhaTest test
```

Expected: todos os testes selecionados passam sem falhas.

- [ ] **Step 8: Commit**

```powershell
git add -- 'demo (2)/demo/src/main/java/moedaEstudantil/controller' 'demo (2)/demo/src/test/java/moedaEstudantil/controller'
git commit -m "feat(auth): expor login e proteger senhas cadastradas"
```

---

### Task 3: Sessão, cliente de autenticação e catálogo de rotas do frontend

**Files:**
- Create: `frontend/js/core/session.test.js`
- Create: `frontend/js/core/session.js`
- Create: `frontend/js/core/routes.test.js`
- Create: `frontend/js/core/routes.js`
- Create: `frontend/js/auth-api.test.js`
- Create: `frontend/js/auth-api.js`

**Interfaces:**
- Consumes: `window.AppHttp.request` existente e `POST /api/auth/login` da Task 2.
- Produces: `AppSession.save/read/clear`, `AppRoutes.dashboard/cardsFor`, e `AuthApi.login`.

- [ ] **Step 1: Escrever testes falhos para sessão mínima**

```javascript
const test = require('node:test');
const assert = require('node:assert/strict');
const { createSessionStore } = require('./session.js');

test('sessão persiste somente identidade segura', () => {
  const values = new Map();
  const storage = {
    getItem: key => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: key => values.delete(key)
  };
  const session = createSessionStore(storage);

  session.save({
    id: 4, nome: 'Aluno Teste', email: 'aluno@puc.br', perfil: 'ALUNO',
    senha: 'segredo', cpf: '000.000.000-00'
  });

  assert.deepEqual(session.read(), {
    id: 4, nome: 'Aluno Teste', email: 'aluno@puc.br', perfil: 'ALUNO'
  });
  assert.doesNotMatch(values.values().next().value, /segredo|000\.000/);
});

test('sessão inválida é descartada', () => {
  const values = new Map([['moeda_estudantil_sessao_v1', '{"id":4,"perfil":"ADMIN"}']]);
  const storage = {
    getItem: key => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: key => values.delete(key)
  };
  const session = createSessionStore(storage);
  assert.equal(session.read(), null);
});
```

- [ ] **Step 2: Escrever testes falhos para rotas e cliente HTTP**

```javascript
test('cardsFor entrega somente ações do aluno com seu id', () => {
  assert.deepEqual(cardsFor({ id: 4, perfil: 'ALUNO' }).map(card => card.href), [
    'resgatar-vantagens.html?alunoId=4',
    'extrato.html?usuarioId=4',
    'solicitar-transferencia.html?alunoId=4',
    'criterios-transferencia.html?alunoId=4'
  ]);
});

test('cardsFor recusa perfil não reconhecido', () => {
  assert.throws(() => cardsFor({ id: 1, perfil: 'ADMIN' }), /Perfil inválido/);
});

test('login envia perfil junto às credenciais', async () => {
  let captured;
  const request = async (url, options) => {
    captured = { url, options };
    return { id: 1, nome: 'Glender', email: 'prof@puc.br', perfil: 'PROFESSOR' };
  };

  await createAuthApi(request).login('prof@puc.br', 'senha123', 'PROFESSOR');

  assert.equal(captured.url, 'http://localhost:8080/api/auth/login');
  assert.deepEqual(JSON.parse(captured.options.body), {
    email: 'prof@puc.br', senha: 'senha123', perfil: 'PROFESSOR'
  });
});
```

- [ ] **Step 3: Executar os testes e confirmar as falhas**

Run:

```powershell
node --test frontend/js/core/session.test.js frontend/js/core/routes.test.js frontend/js/auth-api.test.js
```

Expected: FAIL com `MODULE_NOT_FOUND` para os três módulos novos.

- [ ] **Step 4: Implementar `session.js`**

Usar IIFE compatível com navegador e CommonJS, validar o perfil e construir um novo objeto para impedir campos extras:

```javascript
(function initializeSession(root) {
  const KEY = 'moeda_estudantil_sessao_v1';
  const PERFIS = new Set(['ALUNO', 'PROFESSOR', 'EMPRESA']);

  function createSessionStore(storage) {
    function sanitize(value) {
      if (!value || !Number.isFinite(Number(value.id)) || !PERFIS.has(value.perfil)) return null;
      return {
        id: Number(value.id),
        nome: String(value.nome || ''),
        email: String(value.email || ''),
        perfil: value.perfil
      };
    }
    return {
      save(value) {
        const safe = sanitize(value);
        if (!safe) throw new Error('Sessão inválida.');
        storage.setItem(KEY, JSON.stringify(safe));
        return safe;
      },
      read() {
        try {
          const safe = sanitize(JSON.parse(storage.getItem(KEY)));
          if (!safe) storage.removeItem(KEY);
          return safe;
        } catch {
          storage.removeItem(KEY);
          return null;
        }
      },
      clear() { storage.removeItem(KEY); }
    };
  }
  const api = { createSessionStore };
  if (root.sessionStorage) api.current = createSessionStore(root.sessionStorage);
  root.AppSession = api;
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
})(typeof window !== 'undefined' ? window : globalThis);
```

- [ ] **Step 5: Implementar `routes.js` e `auth-api.js`**

`cardsFor` deve retornar cópias de objetos com `title`, `description`, `eyebrow` e `href`. O catálogo fixo usa os URLs literais testados. `dashboard()` retorna `painel.html`.

```javascript
function createAuthApi(request) {
  return {
    login(email, senha, perfil) {
      return request('http://localhost:8080/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email.trim(), senha, perfil })
      });
    }
  };
}
```

No navegador, instanciar `window.AuthApi = createAuthApi(window.AppHttp.request)`; em Node, exportar `createAuthApi`.

- [ ] **Step 6: Executar todos os testes frontend**

Run:

```powershell
node --test
```

Expected: todos os testes JavaScript passam.

- [ ] **Step 7: Commit**

```powershell
git add -- frontend/js/core/session.js frontend/js/core/session.test.js frontend/js/core/routes.js frontend/js/core/routes.test.js frontend/js/auth-api.js frontend/js/auth-api.test.js
git commit -m "feat(frontend): centralizar sessao autenticada e rotas"
```

---

### Task 4: Portais de cadastro sem armazenamento local

**Files:**
- Rename: `frontend/index.html` to `frontend/aluno.html`
- Modify: `frontend/aluno.html`
- Modify: `frontend/empresa.html`
- Modify: `frontend/js/api.js`
- Modify: `frontend/js/empresa-api.js`
- Modify: `frontend/js/app.js`
- Modify: `frontend/js/empresa.js`
- Create: `frontend/js/api.test.js`
- Create: `frontend/js/empresa-api.test.js`

**Interfaces:**
- Consumes: `AppHttp.request`, `AuthApi.login` e `AppSession.current` da Task 3.
- Produces: `AlunoAuthService` e `EmpresaAuthService` sem `localStorage`, com cadastro/CRUD remoto e sessão unificada.

- [ ] **Step 1: Escrever testes falhos que proíbem fallback local**

Extrair factories que recebem dependências, mantendo a instância global no navegador:

```javascript
test('cadastro de aluno propaga indisponibilidade sem gravar cópia local', async () => {
  const request = async () => { throw Object.assign(new Error('Servidor indisponível.'), { status: 503 }); };
  const service = createAlunoService({ request, authApi: {}, session: {} });

  await assert.rejects(service.cadastrar(alunoValido), /Servidor indisponível/);
});

test('login de empresa usa autenticação central e salva sessão segura', async () => {
  let saved;
  const service = createEmpresaService({
    request: async () => null,
    authApi: { login: async () => ({ id: 5, nome: 'Cantina', email: 'c@x.br', perfil: 'EMPRESA' }) },
    session: { save: value => { saved = value; return value; }, read: () => null, clear() {} }
  });

  await service.login('c@x.br', 'senha123');
  assert.equal(saved.perfil, 'EMPRESA');
});
```

- [ ] **Step 2: Executar e confirmar a falha**

Run:

```powershell
node --test frontend/js/api.test.js frontend/js/empresa-api.test.js
```

Expected: FAIL porque os arquivos atuais não exportam factories e usam `localStorage`.

- [ ] **Step 3: Reescrever os serviços como clientes exclusivos da API**

`createAlunoService` deve expor:

```javascript
return {
  getInstituicoes: () => request(`${API_BASE_URL}/instituicoes`),
  cadastrar: dados => request(`${API_BASE_URL}/alunos`, jsonPost(dados))
    .then(aluno => session.save({ ...aluno, perfil: 'ALUNO' })),
  login: (email, senha) => authApi.login(email, senha, 'ALUNO')
    .then(session.save),
  getAlunoLogado: session.read,
  atualizarMeusDados: dados => {
    const atual = requireSession('ALUNO');
    return request(`${API_BASE_URL}/alunos/${atual.id}`, jsonPut(dados))
      .then(aluno => session.save({ ...aluno, perfil: 'ALUNO' }));
  },
  excluirMinhaConta: async () => {
    const atual = requireSession('ALUNO');
    await request(`${API_BASE_URL}/alunos/${atual.id}`, { method: 'DELETE' });
    session.clear();
  },
  logout: session.clear
};
```

Criar versão equivalente para empresa usando `/api/empresas`. Remover chaves `STORAGE_*`, dados iniciais, `initStorage`, `checkBackend` e qualquer captura de erro que grave localmente.

- [ ] **Step 4: Mover o portal do aluno e ligar os módulos compartilhados**

Executar o rename e ajustar scripts no final de `aluno.html` e `empresa.html`:

```html
<script src="js/core/http.js"></script>
<script src="js/core/session.js"></script>
<script src="js/core/routes.js"></script>
<script src="js/auth-api.js"></script>
<script src="js/api.js"></script>
<script src="js/app.js"></script>
```

Trocar links antigos para `index.html` por `aluno.html` somente quando o destino for o portal do aluno; a marca volta para a nova landing em `index.html`.

- [ ] **Step 5: Atualizar os controladores das páginas**

Remover `checkBackend()` e login rápido com credenciais locais. Tornar `init()` assíncrona e trocar a leitura síncrona de instituições por `const instituicoes = await window.alunoAuthService.getInstituicoes()`. Em sucesso de login, redirecionar para o painel:

```javascript
const usuario = await window.alunoAuthService.login(email, senha);
window.location.assign(window.AppRoutes.dashboard(usuario));
```

Após cadastro, manter a tela de perfil já existente; o objeto persistido é salvo pela sessão unificada.

- [ ] **Step 6: Executar todos os testes frontend**

Run:

```powershell
node --test
```

Expected: todos os testes passam e nenhuma execução usa `localStorage`.

- [ ] **Step 7: Commit**

```powershell
git add -A -- frontend/index.html frontend/aluno.html frontend/empresa.html frontend/js/api.js frontend/js/api.test.js frontend/js/empresa-api.js frontend/js/empresa-api.test.js frontend/js/app.js frontend/js/empresa.js
git commit -m "refactor(frontend): remover banco local de usuarios"
```

---

### Task 5: Painel pós-login orientado ao perfil

**Files:**
- Create: `frontend/painel.html`
- Create: `frontend/css/painel.css`
- Create: `frontend/js/painel.js`
- Create: `frontend/js/painel.test.js`

**Interfaces:**
- Consumes: `AppSession.current.read/clear` e `AppRoutes.cardsFor` da Task 3.
- Produces: hub autenticado que renderiza somente ações do perfil corrente e encerra a sessão.

- [ ] **Step 1: Escrever teste falho do modelo do painel**

Exportar `buildDashboardModel(session, cardsFor)` de `painel.js` para teste sem DOM:

```javascript
test('painel deriva saudação e ações somente da sessão autenticada', () => {
  const model = buildDashboardModel(
    { id: 1, nome: 'Glender Brás', email: 'prof@puc.br', perfil: 'PROFESSOR' },
    () => [{ title: 'Distribuir moedas', href: 'distribuir-moedas.html?professorId=1' }]
  );

  assert.equal(model.firstName, 'Glender');
  assert.equal(model.profileLabel, 'Professor');
  assert.equal(model.cards.length, 1);
});

test('painel recusa ausência de sessão', () => {
  assert.throws(() => buildDashboardModel(null, () => []), /Sessão necessária/);
});
```

- [ ] **Step 2: Executar e confirmar a falha**

Run:

```powershell
node --test frontend/js/painel.test.js
```

Expected: FAIL com `MODULE_NOT_FOUND`.

- [ ] **Step 3: Implementar modelo e renderização segura**

`buildDashboardModel` mapeia `ALUNO`, `PROFESSOR` e `EMPRESA` para rótulos. A renderização usa `document.createElement`, `textContent` e `setAttribute`, sem concatenar dados do usuário em HTML.

```javascript
function buildDashboardModel(session, cardsFor) {
  if (!session) throw new Error('Sessão necessária.');
  const labels = { ALUNO: 'Aluno', PROFESSOR: 'Professor', EMPRESA: 'Empresa parceira' };
  if (!labels[session.perfil]) throw new Error('Perfil inválido.');
  return {
    firstName: session.nome.trim().split(/\s+/)[0] || labels[session.perfil],
    profileLabel: labels[session.perfil],
    cards: cardsFor(session)
  };
}
```

Sem sessão, `painel.js` redireciona para `index.html?entrar=1`. Logout limpa sessão e retorna à landing.

- [ ] **Step 4: Criar HTML semântico do painel**

Estrutura mínima:

```html
<body class="dashboard-shell">
  <header class="dashboard-nav">
    <a class="brand" href="index.html" aria-label="Moeda Estudantil - início">ME.</a>
    <button id="logoutButton" class="button button--ghost">Sair</button>
  </header>
  <main>
    <section class="dashboard-hero" aria-labelledby="dashboardTitle">
      <p id="profileLabel" class="eyebrow"></p>
      <h1 id="dashboardTitle"></h1>
      <p>Escolha o que você quer fazer agora.</p>
    </section>
    <section aria-label="Funcionalidades disponíveis">
      <div id="actionGrid" class="action-grid"></div>
    </section>
  </main>
  <script src="js/core/session.js"></script>
  <script src="js/core/routes.js"></script>
  <script src="js/painel.js"></script>
</body>
```

- [ ] **Step 5: Criar CSS mobile-first**

Usar tokens locais e grid fluido:

```css
:root {
  --forest: #163300;
  --lime: #9fe870;
  --mist: #e2f6d5;
  --paper: #fff;
  --ink: #0e0f0c;
}
.action-grid { display: grid; grid-template-columns: 1fr; gap: 1rem; }
.action-card { min-height: 11rem; border-radius: 1.75rem; padding: 1.5rem; }
.button { min-height: 2.75rem; }
@media (min-width: 48rem) {
  .action-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (min-width: 64rem) {
  .action-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
```

- [ ] **Step 6: Executar teste do painel e testes frontend completos**

Run:

```powershell
node --test
```

Expected: todos os testes passam.

- [ ] **Step 7: Commit**

```powershell
git add -- frontend/painel.html frontend/css/painel.css frontend/js/painel.js frontend/js/painel.test.js
git commit -m "feat(frontend): adicionar painel por perfil"
```

---

### Task 6: Landing SaaS futurista e diálogo de login

**Files:**
- Create: `frontend/index.html`
- Create: `frontend/css/landing.css`
- Create: `frontend/js/landing.js`
- Create: `frontend/js/landing.test.js`

**Interfaces:**
- Consumes: `AuthApi.login`, `AppSession.current.save` e `AppRoutes.dashboard` da Task 3.
- Produces: entrada pública, seletor dos três perfis, login acessível, navegação responsiva e componentes comerciais interativos.

- [ ] **Step 1: Escrever testes falhos do estado do login**

Separar estado puro da manipulação DOM:

```javascript
test('professor nunca recebe link de cadastro', () => {
  assert.deepEqual(createLoginState('PROFESSOR'), {
    perfil: 'PROFESSOR',
    label: 'Professor',
    cadastroHref: null
  });
});

test('aluno recebe o destino correto de cadastro', () => {
  assert.equal(createLoginState('ALUNO').cadastroHref, 'aluno.html?tab=cadastro');
});

test('perfil desconhecido é recusado', () => {
  assert.throws(() => createLoginState('ADMIN'), /Perfil inválido/);
});
```

- [ ] **Step 2: Executar o teste e confirmar a falha**

Run:

```powershell
node --test frontend/js/landing.test.js
```

Expected: FAIL com `MODULE_NOT_FOUND`.

- [ ] **Step 3: Criar a estrutura semântica da landing**

`index.html` deve conter, nesta ordem e com estes IDs:

```html
<header id="siteHeader" class="site-header">
  <a class="brand" href="#produto" aria-label="Moeda Estudantil - início">ME.</a>
  <button id="menuToggle" class="menu-toggle" aria-expanded="false" aria-controls="mainNav">Menu</button>
  <nav id="mainNav" class="main-nav" aria-label="Navegação principal">
    <a href="#produto">Produto</a>
    <a href="#como-funciona">Como funciona</a>
    <a href="#beneficios">Benefícios</a>
    <a href="#parceiros">Parceiros</a>
  </nav>
  <button class="button button--outline" data-open-login>Entrar</button>
</header>
<main>
  <section id="produto" class="hero" aria-labelledby="heroTitle">
    <div class="hero__content"></div>
    <div class="coin-stage" aria-hidden="true"></div>
  </section>
  <section id="como-funciona" class="flow-section">
    <h2>Uma moeda. Três forças.</h2>
    <ol class="flow-grid"><li>Professor reconhece</li><li>Aluno conquista</li><li>Parceiro recompensa</li></ol>
  </section>
  <section id="beneficios" class="audience-section">
    <h2>Valor para toda a comunidade</h2>
    <div class="audience-grid" aria-label="Benefícios por perfil"></div>
  </section>
  <section class="proof-section" aria-labelledby="proofTitle">
    <h2 id="proofTitle">Do reconhecimento ao benefício, sem perder o ritmo.</h2>
    <dl class="proof-grid"><div><dt>3</dt><dd>perfis conectados</dd></div><div><dt>1</dt><dd>ciclo integrado</dd></div></dl>
  </section>
  <section id="parceiros" class="partner-section">
    <h2>Faça parte da rede que reconhece talento.</h2>
    <button class="button button--primary" data-open-login data-profile="EMPRESA">Acessar como parceiro</button>
  </section>
  <section class="faq-section" aria-labelledby="faqTitle">
    <h2 id="faqTitle">Perguntas frequentes</h2>
    <div class="faq-list"></div>
  </section>
</main>
<footer class="site-footer"><a href="#produto">Moeda Estudantil</a><p>Reconhecimento que circula.</p></footer>
<dialog id="loginDialog" class="login-dialog" aria-labelledby="loginTitle">
  <button id="closeLogin" class="dialog-close" aria-label="Fechar acesso">×</button>
  <p class="eyebrow">Acesso seguro</p>
  <h2 id="loginTitle">Entre na plataforma</h2>
  <div class="profile-switcher" aria-label="Escolha seu perfil">
    <button type="button" data-profile="ALUNO" aria-pressed="true">Aluno</button>
    <button type="button" data-profile="PROFESSOR" aria-pressed="false">Professor</button>
    <button type="button" data-profile="EMPRESA" aria-pressed="false">Empresa</button>
  </div>
  <form id="loginForm">
    <label for="loginEmail">E-mail</label><input id="loginEmail" type="email" autocomplete="username" required>
    <label for="loginPassword">Senha</label><input id="loginPassword" type="password" autocomplete="current-password" required>
    <p id="loginError" role="alert"></p>
    <button id="loginSubmit" class="button button--primary" type="submit">Entrar</button>
  </form>
  <a id="signupLink" class="text-link" href="aluno.html?tab=cadastro">Criar conta</a>
</dialog>
```

O hero usa a copy aprovada no contexto do produto:

```html
<p class="eyebrow">Reconhecimento que circula</p>
<h1 id="heroTitle">Mérito vira movimento.</h1>
<p class="hero__copy">Professores reconhecem. Alunos conquistam. Parceiros transformam moedas em experiências reais.</p>
<button class="button button--primary" data-open-login>Entrar na plataforma</button>
<a class="text-link" href="#como-funciona">Ver como funciona</a>
```

Criar a moeda 3D somente com elementos decorativos `aria-hidden="true"`: `coin-stage`, `coin`, `coin__edge`, `coin__face`, três `orbit-dot` e um cartão flutuante de saldo. Não usar canvas ou dependência externa.

- [ ] **Step 4: Criar os tokens e layout mobile-first**

O topo de `landing.css` deve estabelecer os tokens e bases:

```css
:root {
  --forest: #163300;
  --lime: #9fe870;
  --spruce: #054d28;
  --mist: #e2f6d5;
  --fog: #e8ebe6;
  --paper: #ffffff;
  --ink: #0e0f0c;
  --charcoal: #454745;
  --danger: #9f1d2c;
  --radius-pill: 9999px;
  --radius-card: 1.75rem;
  --page: 75rem;
}
* { box-sizing: border-box; }
html { scroll-behavior: smooth; }
body { margin: 0; min-width: 20rem; overflow-x: hidden; font-family: Inter, ui-sans-serif, system-ui, sans-serif; }
button, input, a { -webkit-tap-highlight-color: transparent; }
:focus-visible { outline: 3px solid var(--lime); outline-offset: 3px; }
.button { min-height: 2.75rem; border-radius: var(--radius-pill); }
.hero h1 { font-size: clamp(2.75rem, 12vw, 6.5rem); line-height: .86; letter-spacing: -.045em; }
```

Usar uma coluna até `48rem`, duas colunas para hero e seções a partir de `64rem`, e limitar conteúdo a `var(--page)`. Nenhum texto de corpo fica abaixo de `1rem`.

- [ ] **Step 5: Criar profundidade 3D e movimento reduzível**

```css
.coin-stage { perspective: 70rem; position: relative; min-height: 22rem; }
.coin { position: absolute; inset: 50% auto auto 50%; width: min(68vw, 20rem); aspect-ratio: 1; transform: translate(-50%, -50%) rotateX(62deg) rotateZ(-18deg); transform-style: preserve-3d; animation: coin-float 7s ease-in-out infinite; }
.coin__face { border-radius: 50%; background: radial-gradient(circle at 34% 28%, #ecffc9 0 6%, var(--lime) 20%, #63b83c 72%, #2f6e1d 100%); box-shadow: inset -1.4rem -1.1rem 2.2rem rgb(5 77 40 / .45), 0 2.5rem 4rem rgb(0 0 0 / .28); }
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { scroll-behavior: auto !important; animation-duration: .01ms !important; animation-iteration-count: 1 !important; transition-duration: .01ms !important; }
}
```

O gradiente é restrito à ilustração 3D para simular material; superfícies e botões permanecem planos conforme a referência.

- [ ] **Step 6: Implementar estado e interações da landing**

`createLoginState` retorna dados literais dos três perfis. No navegador:

- abrir `dialog.showModal()` por `[data-open-login]`;
- fechar por botão, tecla Esc nativa e clique no backdrop;
- alternar `aria-pressed` nos seletores de perfil;
- mostrar link de cadastro apenas para aluno/empresa;
- no submit, desabilitar o botão, chamar `AuthApi.login`, salvar a sessão e navegar para `painel.html`;
- exibir erros em um elemento `role="alert"` sem limpar e-mail/senha;
- alternar menu móvel com `aria-expanded`;
- accordions usam botões com `aria-expanded` e regiões nomeadas;
- aplicar tilt somente quando `(hover: hover)` e movimento não estiver reduzido.

Trecho central:

```javascript
form.addEventListener('submit', async event => {
  event.preventDefault();
  submitButton.disabled = true;
  errorBox.textContent = '';
  try {
    const user = await root.AuthApi.login(email.value, password.value, state.perfil);
    root.AppSession.current.save(user);
    root.location.assign(root.AppRoutes.dashboard(user));
  } catch (error) {
    errorBox.textContent = error.status === 401
      ? 'E-mail, senha ou perfil inválido.'
      : 'A plataforma está indisponível agora. Tente novamente.';
  } finally {
    submitButton.disabled = false;
  }
});
```

- [ ] **Step 7: Executar os testes frontend**

Run:

```powershell
node --test
```

Expected: todos os testes passam.

- [ ] **Step 8: Commit**

```powershell
git add -- frontend/index.html frontend/css/landing.css frontend/js/landing.js frontend/js/landing.test.js
git commit -m "feat(frontend): criar landing SaaS e login por perfil"
```

---

### Task 7: Sincronização, documentação e verificação integral

**Files:**
- Modify: `README.md`
- Modify generated mirror: `demo (2)/demo/src/main/resources/static/**`

**Interfaces:**
- Consumes: todos os entregáveis das Tasks 1-6.
- Produces: árvore estática sincronizada, documentação de execução e evidência final de testes/visual.

- [ ] **Step 1: Atualizar o README**

Substituir a lista de entradas por:

```markdown
* **Landing e acesso por perfil:** `http://localhost:8080/index.html`
* **Painel após autenticação:** `http://localhost:8080/painel.html`
* **Cadastro e perfil do aluno:** `http://localhost:8080/aluno.html`
* **Cadastro e perfil da empresa:** `http://localhost:8080/empresa.html`

Contas de demonstração persistidas:

| Perfil | E-mail | Senha |
| --- | --- | --- |
| Aluno | `aluno.teste@sga.pucminas.br` | `senha123` |
| Professor | `glender.bras@pucminas.br` | `senha123` |
| Empresa | `contato@cantinadapuc.com.br` | `senha123` |
```

Explicar em uma frase que o primeiro login migra senhas legadas para BCrypt.

- [ ] **Step 2: Executar a suíte frontend antes da cópia**

Run:

```powershell
node --test
```

Expected: zero falhas.

- [ ] **Step 3: Executar a suíte Maven completa**

Run:

```powershell
Set-Location 'demo (2)/demo'
.\mvnw.cmd test
```

Expected: `BUILD SUCCESS`, zero failures e zero errors. Se o teste não rastreado do usuário falhar, relatar separadamente e não editar esse arquivo sem autorização.

- [ ] **Step 4: Sincronizar o frontend e provar igualdade**

Run a partir da raiz:

```powershell
.\scripts\sync-frontend.ps1
.\scripts\test-frontend-sync.ps1
```

Expected: `Frontend sincronizado` com a mesma contagem nos dois comandos.

- [ ] **Step 5: Iniciar a aplicação com ambiente de demonstração**

Usar as variáveis MySQL já configuradas pelo usuário; não imprimir segredos. Iniciar:

```powershell
Set-Location 'demo (2)/demo'
.\mvnw.cmd spring-boot:run
```

Expected: aplicação responde em `http://localhost:8080/index.html` e `/api/instituicoes`.

- [ ] **Step 6: Fazer verificação visual e funcional no navegador**

Em 375px, 414px, 768px, 1024px e 1440px, verificar:

- nenhuma rolagem horizontal;
- navegação móvel abre, fecha e restaura `aria-expanded`;
- hero e moeda 3D não se sobrepõem ao texto;
- login abre por mouse e teclado;
- seletor muda entre os três perfis e professor não mostra cadastro;
- credencial inválida produz mensagem genérica;
- login válido de cada perfil abre o painel com os cartões corretos;
- logout remove a sessão e retorna à landing;
- `prefers-reduced-motion` elimina movimento contínuo;
- console do navegador sem erros.

- [ ] **Step 7: Verificar o diff final**

Run:

```powershell
git diff --check
git status --short
git diff --stat
```

Expected: nenhum erro de whitespace; apenas arquivos desta implementação e o teste não rastreado já existente aparecem no status.

- [ ] **Step 8: Commit final**

```powershell
git add -- README.md frontend 'demo (2)/demo/src/main/resources/static'
git commit -m "docs: atualizar acesso e sincronizar frontend"
```

- [ ] **Step 9: Reexecutar os gates após o commit**

Run:

```powershell
node --test
Set-Location 'demo (2)/demo'
.\mvnw.cmd test
Set-Location ../../..
.\scripts\test-frontend-sync.ps1
git status --short
```

Expected: testes frontend passam, Maven mostra `BUILD SUCCESS`, frontend está sincronizado e o único item não rastreado restante é `CreditoSemestralProfessorServiceTest.java` se ele ainda não tiver sido tratado pelo usuário.
