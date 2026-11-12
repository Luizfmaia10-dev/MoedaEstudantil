# Catálogo de vantagens e automação por IA — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refinar o catálogo de vantagens do aluno e permitir que cada professor automatize decisões de transferência com IA, sem decidir quando a confiança for inferior a 80% ou o Ollama falhar.

**Architecture:** A solicitação continua sendo persistida como pendente e publica um evento processado após o commit. Um serviço de automação monta um contexto mínimo, chama um avaliador Ollama fora de bloqueios e reutiliza as operações transacionais existentes para aprovar ou recusar com auditoria. O frontend amplia as telas existentes e reutiliza o `star-border`, a paleta e os padrões responsivos do projeto.

**Tech Stack:** Java 21, Spring Boot 4.1, Spring Web MVC, Spring Data JPA, Jackson, Ollama, JUnit 5, Mockito, HTML5, CSS3, JavaScript, Node Test Runner e Chromium.

**Spec:** `docs/superpowers/specs/2026-11-11-catalogo-vantagens-automacao-ia-design.md`

## Global Constraints

- Trabalhar e commitar diretamente na branch `main`, com mensagens em português.
- Manter o limiar fixo de confiança em `0.80` e não expor configuração na interface.
- Falha, timeout, resposta inválida, decisão `PENDENTE` ou baixa confiança não alteram a solicitação.
- Não adicionar dependências; reutilizar o cliente HTTP, Jackson, componentes e tokens visuais existentes.
- Não manter transação ou bloqueio de saldo durante a chamada ao Ollama.
- Preservar avaliação manual, resgate, cupom e APIs atuais.
- Implementar layout mobile-first, alvos de toque de pelo menos 44 px e movimento reduzido.
- Sincronizar `frontend/` com `demo (2)/demo/src/main/resources/static/` somente após concluir as fontes do frontend.

---

## Estrutura de arquivos

- `model/Professor.java`: persiste se a automação está ativa.
- `model/SolicitacaoTransferencia.java`: persiste auditoria da decisão automática.
- `dto/SolicitacaoTransferenciaDTO.java`: expõe auditoria para a interface.
- `dto/AutomacaoTransferenciaDTO.java`: expõe configuração e quantidade pendente.
- `service/AvaliadorTransferenciaIA.java`: contrato pequeno e testável da decisão da IA.
- `service/OllamaAvaliadorTransferenciaIA.java`: adaptador Ollama sem fallback decisório.
- `service/AutomacaoTransferenciaService.java`: configuração, lote e orquestração sem segurar bloqueios durante a IA.
- `service/SolicitacaoTransferenciaCriadaEvent.java`: evento com o identificador recém-criado.
- `service/AutomacaoTransferenciaEventListener.java`: inicia a análise somente após commit.
- `controller/SolicitacaoTransferenciaController.java`: endpoints de configuração e publicação do evento existente.
- `frontend/avaliar-transferencias.html`, `frontend/js/avaliar-transferencias.js` e `frontend/js/transferencia-api.js`: painel da automação e selo de auditoria.
- `frontend/resgatar-vantagens.html`, `frontend/js/resgatar-vantagens.js` e `frontend/css/styles.css`: busca, filtro e refinamento do catálogo.

### Task 1: Persistir e expor a auditoria da decisão

**Files:**
- Modify: `demo (2)/demo/src/main/java/moedaEstudantil/model/Professor.java`
- Modify: `demo (2)/demo/src/main/java/moedaEstudantil/model/SolicitacaoTransferencia.java`
- Modify: `demo (2)/demo/src/main/java/moedaEstudantil/dto/SolicitacaoTransferenciaDTO.java`
- Modify: `demo (2)/demo/src/main/java/moedaEstudantil/service/SolicitacaoTransferenciaService.java`
- Test: `demo (2)/demo/src/test/java/moedaEstudantil/service/SolicitacaoTransferenciaServiceTest.java`

**Interfaces:**
- Produces: `boolean Professor.isAutomacaoTransferenciasAtiva()`.
- Produces: `SolicitacaoTransferenciaDTO` com `Boolean avaliacaoAutomatica` e `Double confiancaAvaliacaoAutomatica`.
- Produces: `aprovarAutomaticamente(Long, Long, String, double)` e `recusarAutomaticamente(Long, Long, String, double)`.

- [ ] **Step 1: Escrever testes de auditoria que falham**

Adicionar testes que chamem os novos métodos e verifiquem os campos sem alterar o comportamento manual:

```java
@Test
void aprovacaoAutomaticaRegistraOrigemEConfianca() {
    solicitacaoPendente(25);
    SolicitacaoTransferenciaDTO dto = service.aprovarAutomaticamente(
            1L, 10L, "Critérios atendidos pela IA.", 0.91);
    assertTrue(dto.avaliacaoAutomatica());
    assertEquals(0.91, dto.confiancaAvaliacaoAutomatica());
}

@Test
void avaliacaoManualNaoEhMarcadaComoAutomatica() {
    solicitacaoPendente(25);
    SolicitacaoTransferenciaDTO dto = service.recusar(1L, 10L, "Análise manual");
    assertFalse(dto.avaliacaoAutomatica());
    assertNull(dto.confiancaAvaliacaoAutomatica());
}
```

- [ ] **Step 2: Executar o teste para confirmar a falha**

Run: `cd "demo (2)/demo"; .\mvnw.cmd -Dtest=SolicitacaoTransferenciaServiceTest test`

Expected: falha de compilação porque os métodos e componentes do DTO ainda não existem.

- [ ] **Step 3: Implementar os campos e centralizar a avaliação**

Adicionar os campos JPA com defaults seguros e getters/setters. No serviço, manter os métodos públicos atuais e delegar a um núcleo comum:

```java
public SolicitacaoTransferenciaDTO aprovar(Long professorId, Long solicitacaoId, String observacao) {
    return aprovar(professorId, solicitacaoId, observacao, false, null);
}

public SolicitacaoTransferenciaDTO aprovarAutomaticamente(Long professorId, Long solicitacaoId,
                                                           String observacao, double confianca) {
    return aprovar(professorId, solicitacaoId, observacao, true, validarConfianca(confianca));
}
```

Aplicar o mesmo padrão à recusa e gravar `avaliacaoAutomatica` e `confiancaAvaliacaoAutomatica` no mesmo commit transacional da decisão. Acrescentar os dois valores ao final do record `SolicitacaoTransferenciaDTO` e ao método `toDTO`.

- [ ] **Step 4: Executar os testes do serviço**

Run: `cd "demo (2)/demo"; .\mvnw.cmd -Dtest=SolicitacaoTransferenciaServiceTest test`

Expected: todos os testes passam.

- [ ] **Step 5: Commitar**

```powershell
git add -- 'demo (2)/demo/src/main/java/moedaEstudantil/model/Professor.java' 'demo (2)/demo/src/main/java/moedaEstudantil/model/SolicitacaoTransferencia.java' 'demo (2)/demo/src/main/java/moedaEstudantil/dto/SolicitacaoTransferenciaDTO.java' 'demo (2)/demo/src/main/java/moedaEstudantil/service/SolicitacaoTransferenciaService.java' 'demo (2)/demo/src/test/java/moedaEstudantil/service/SolicitacaoTransferenciaServiceTest.java'
git commit -m "feat: registrar auditoria das decisões automáticas"
```

### Task 2: Criar o contrato de avaliação e o adaptador Ollama

**Files:**
- Create: `demo (2)/demo/src/main/java/moedaEstudantil/service/AvaliadorTransferenciaIA.java`
- Create: `demo (2)/demo/src/main/java/moedaEstudantil/service/OllamaAvaliadorTransferenciaIA.java`
- Create: `demo (2)/demo/src/test/java/moedaEstudantil/service/OllamaAvaliadorTransferenciaIATest.java`

**Interfaces:**
- Produces: `AvaliadorTransferenciaIA.avaliar(Contexto contexto)`.
- Produces: records `Contexto(String criterios, int quantidade, String justificativa, String remetente, String destinatario)` e `Resultado(Decisao decisao, double confianca, String explicacao)`.
- Produces: enum `Decisao { APROVAR, RECUSAR, PENDENTE }`.
- Throws: `IaLocalIndisponivelException` quando a chamada ou o JSON falhar.

- [ ] **Step 1: Escrever testes HTTP do adaptador**

Usar `HttpServer` local como em `OllamaInterpretadorTransferenciaIATest` e cobrir resposta válida e envelope inválido:

```java
@Test
void interpretaDecisaoEstruturada() {
    servidorResponde(200, """
        {"message":{"content":"{\"decisao\":\"APROVAR\",\"confianca\":0.93,\"explicacao\":\"Critério atendido.\"}"}}
        """);
    var resultado = cliente().avaliar(new Contexto(
            "Aprovar colaboração", 20, "Ajuda no projeto", "Ana", "Bia"));
    assertEquals(Decisao.APROVAR, resultado.decisao());
    assertEquals(0.93, resultado.confianca());
}

@Test
void naoUsaFallbackQuandoOllamaFalha() {
    servidorResponde(503, "indisponível");
    assertThrows(IaLocalIndisponivelException.class, () -> cliente().avaliar(contexto()));
}
```

- [ ] **Step 2: Executar os testes para confirmar a falha**

Run: `cd "demo (2)/demo"; .\mvnw.cmd -Dtest=OllamaAvaliadorTransferenciaIATest test`

Expected: falha de compilação porque contrato e implementação ainda não existem.

- [ ] **Step 3: Implementar contrato e adaptador mínimo**

O prompt deve exigir somente o schema abaixo, temperatura zero e explicação com no máximo 300 caracteres:

```java
return Map.of(
    "type", "object",
    "properties", Map.of(
        "decisao", Map.of("type", "string", "enum", List.of("APROVAR", "RECUSAR", "PENDENTE")),
        "confianca", Map.of("type", "number", "minimum", 0, "maximum", 1),
        "explicacao", Map.of("type", "string", "maxLength", 300)),
    "required", List.of("decisao", "confianca", "explicacao"),
    "additionalProperties", false);
```

Validar confiança finita entre 0 e 1 e explicação não vazia. Converter toda falha de rede, status, envelope ou conteúdo em `IaLocalIndisponivelException`, preservando interrupção da thread.

- [ ] **Step 4: Executar os testes do adaptador**

Run: `cd "demo (2)/demo"; .\mvnw.cmd -Dtest=OllamaAvaliadorTransferenciaIATest test`

Expected: todos os testes passam.

- [ ] **Step 5: Commitar**

```powershell
git add -- 'demo (2)/demo/src/main/java/moedaEstudantil/service/AvaliadorTransferenciaIA.java' 'demo (2)/demo/src/main/java/moedaEstudantil/service/OllamaAvaliadorTransferenciaIA.java' 'demo (2)/demo/src/test/java/moedaEstudantil/service/OllamaAvaliadorTransferenciaIATest.java'
git commit -m "feat: integrar avaliador de transferências ao Ollama"
```

### Task 3: Implementar configuração e processamento em lote

**Files:**
- Create: `demo (2)/demo/src/main/java/moedaEstudantil/dto/AutomacaoTransferenciaDTO.java`
- Create: `demo (2)/demo/src/main/java/moedaEstudantil/service/AutomacaoTransferenciaService.java`
- Modify: `demo (2)/demo/src/main/java/moedaEstudantil/repository/SolicitacaoTransferenciaRepository.java`
- Test: `demo (2)/demo/src/test/java/moedaEstudantil/service/AutomacaoTransferenciaServiceTest.java`

**Interfaces:**
- Produces: `AutomacaoTransferenciaDTO(boolean ativa, boolean possuiCriterios, String criterios, long pendentes)`.
- Produces: `consultar(Long professorId)`, `configurar(Long professorId, boolean ativa)` e `avaliarSeAtiva(Long solicitacaoId)`.
- Consumes: `AvaliadorTransferenciaIA` e os métodos automáticos da Task 1.

- [ ] **Step 1: Escrever testes de configuração e decisão**

Cobrir ativação sem critérios, baixa confiança, aprovação, recusa e parada do lote por indisponibilidade:

```java
@Test
void baixaConfiancaMantemPendente() {
    prepararProfessorAtivoComCriterios();
    prepararSolicitacaoPendente();
    when(avaliador.avaliar(any())).thenReturn(
            new Resultado(Decisao.APROVAR, 0.79, "Confiança insuficiente."));
    service.avaliarSeAtiva(10L);
    verifyNoInteractions(serviceSolicitacao);
}

@Test
void decisaoConfiavelReutilizaAprovacaoTransacional() {
    prepararProfessorAtivoComCriterios();
    prepararSolicitacaoPendente();
    when(avaliador.avaliar(any())).thenReturn(
            new Resultado(Decisao.APROVAR, 0.90, "Critérios atendidos."));
    service.avaliarSeAtiva(10L);
    verify(serviceSolicitacao).aprovarAutomaticamente(1L, 10L, "Critérios atendidos.", 0.90);
}
```

- [ ] **Step 2: Executar os testes para confirmar a falha**

Run: `cd "demo (2)/demo"; .\mvnw.cmd -Dtest=AutomacaoTransferenciaServiceTest test`

Expected: falha de compilação porque serviço, DTO e consulta ainda não existem.

- [ ] **Step 3: Implementar o serviço sem I/O dentro de bloqueios**

Adicionar ao repositório:

```java
List<SolicitacaoTransferencia> findByAvaliadorIdAndStatusOrderByDataSolicitacaoAscIdAsc(
        Long avaliadorId, StatusSolicitacaoTransferencia status);
long countByAvaliadorIdAndStatus(Long avaliadorId, StatusSolicitacaoTransferencia status);
```

`configurar` valida os critérios, salva a flag e, ao ativar, copia apenas os IDs pendentes antes de chamar `avaliarSeAtiva` um por um. `avaliarSeAtiva` lê um contexto imutável, encerra a leitura, chama a IA e somente depois invoca aprovação/recusa. Capturar `IaLocalIndisponivelException` no lote para interromper; baixa confiança e `PENDENTE` apenas retornam sem alteração.

- [ ] **Step 4: Executar os testes da automação**

Run: `cd "demo (2)/demo"; .\mvnw.cmd -Dtest=AutomacaoTransferenciaServiceTest test`

Expected: todos os testes passam.

- [ ] **Step 5: Commitar**

```powershell
git add -- 'demo (2)/demo/src/main/java/moedaEstudantil/dto/AutomacaoTransferenciaDTO.java' 'demo (2)/demo/src/main/java/moedaEstudantil/service/AutomacaoTransferenciaService.java' 'demo (2)/demo/src/main/java/moedaEstudantil/repository/SolicitacaoTransferenciaRepository.java' 'demo (2)/demo/src/test/java/moedaEstudantil/service/AutomacaoTransferenciaServiceTest.java'
git commit -m "feat: automatizar decisões conforme critérios do professor"
```

### Task 4: Disparar a avaliação somente após o commit

**Files:**
- Create: `demo (2)/demo/src/main/java/moedaEstudantil/service/SolicitacaoTransferenciaCriadaEvent.java`
- Create: `demo (2)/demo/src/main/java/moedaEstudantil/service/AutomacaoTransferenciaEventListener.java`
- Modify: `demo (2)/demo/src/main/java/moedaEstudantil/service/SolicitacaoTransferenciaService.java`
- Test: `demo (2)/demo/src/test/java/moedaEstudantil/service/AutomacaoTransferenciaEventListenerTest.java`
- Test: `demo (2)/demo/src/test/java/moedaEstudantil/service/SolicitacaoTransferenciaServiceTest.java`

**Interfaces:**
- Produces: `record SolicitacaoTransferenciaCriadaEvent(Long solicitacaoId)`.
- Consumes: `ApplicationEventPublisher.publishEvent(...)` no final de `criar`.
- Consumes: `AutomacaoTransferenciaService.avaliarSeAtiva(Long)` em `@TransactionalEventListener(phase = AFTER_COMMIT)`.

- [ ] **Step 1: Escrever testes do evento e ouvinte**

No teste do serviço, injetar um mock de `ApplicationEventPublisher` e capturar o ID. No teste do ouvinte:

```java
@Test
void delegaSolicitacaoCriadaParaAutomacao() {
    listener.analisar(new SolicitacaoTransferenciaCriadaEvent(10L));
    verify(automacao).avaliarSeAtiva(10L);
}
```

- [ ] **Step 2: Executar os testes para confirmar a falha**

Run: `cd "demo (2)/demo"; .\mvnw.cmd -Dtest=SolicitacaoTransferenciaServiceTest,AutomacaoTransferenciaEventListenerTest test`

Expected: falha porque evento, construtor atualizado e ouvinte ainda não existem.

- [ ] **Step 3: Publicar e ouvir o evento**

Publicar somente depois de `solicitacaoRepository.save(s)` devolver um ID. O listener deve capturar `IaLocalIndisponivelException` e `ResponseStatusException` para preservar a resposta da criação e registrar a ocorrência sem expor detalhes ao aluno.

- [ ] **Step 4: Executar os testes do fluxo de evento**

Run: `cd "demo (2)/demo"; .\mvnw.cmd -Dtest=SolicitacaoTransferenciaServiceTest,AutomacaoTransferenciaEventListenerTest test`

Expected: todos os testes passam.

- [ ] **Step 5: Commitar**

```powershell
git add -- 'demo (2)/demo/src/main/java/moedaEstudantil/service/SolicitacaoTransferenciaCriadaEvent.java' 'demo (2)/demo/src/main/java/moedaEstudantil/service/AutomacaoTransferenciaEventListener.java' 'demo (2)/demo/src/main/java/moedaEstudantil/service/SolicitacaoTransferenciaService.java' 'demo (2)/demo/src/test/java/moedaEstudantil/service/AutomacaoTransferenciaEventListenerTest.java' 'demo (2)/demo/src/test/java/moedaEstudantil/service/SolicitacaoTransferenciaServiceTest.java'
git commit -m "feat: avaliar novas solicitações após confirmação no banco"
```

### Task 5: Expor a configuração na API do professor

**Files:**
- Modify: `demo (2)/demo/src/main/java/moedaEstudantil/controller/SolicitacaoTransferenciaController.java`
- Create: `demo (2)/demo/src/test/java/moedaEstudantil/controller/SolicitacaoTransferenciaControllerTest.java`

**Interfaces:**
- Produces: `GET /api/professores/{professorId}/transferencias/automacao`.
- Produces: `PUT /api/professores/{professorId}/transferencias/automacao` com `{"ativa":true|false}`.

- [ ] **Step 1: Escrever testes MockMvc dos endpoints**

```java
@Test
void ativaAutomacaoDoProfessor() throws Exception {
    when(automacao.configurar(1L, true)).thenReturn(
            new AutomacaoTransferenciaDTO(true, true, "Aprovar colaboração", 0));
    mvc.perform(put("/api/professores/1/transferencias/automacao")
            .contentType(MediaType.APPLICATION_JSON).content("{\"ativa\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ativa").value(true));
}
```

Também testar corpo ausente e erro de critérios como HTTP 400.

- [ ] **Step 2: Executar o teste para confirmar a falha**

Run: `cd "demo (2)/demo"; .\mvnw.cmd -Dtest=SolicitacaoTransferenciaControllerTest test`

Expected: 404 nos novos endpoints.

- [ ] **Step 3: Implementar endpoints mínimos**

Injetar `AutomacaoTransferenciaService`, criar `record ConfigurarAutomacaoRequest(Boolean ativa)` e rejeitar `null` com mensagem “Informe se a automação deve ficar ativa.”.

- [ ] **Step 4: Executar testes do controller e serviços relacionados**

Run: `cd "demo (2)/demo"; .\mvnw.cmd -Dtest=SolicitacaoTransferenciaControllerTest,AutomacaoTransferenciaServiceTest,SolicitacaoTransferenciaServiceTest test`

Expected: todos os testes passam.

- [ ] **Step 5: Commitar**

```powershell
git add -- 'demo (2)/demo/src/main/java/moedaEstudantil/controller/SolicitacaoTransferenciaController.java' 'demo (2)/demo/src/test/java/moedaEstudantil/controller/SolicitacaoTransferenciaControllerTest.java'
git commit -m "feat: disponibilizar configuração da automação na API"
```

### Task 6: Integrar automação e identidade de IA à tela do professor

**Files:**
- Modify: `frontend/avaliar-transferencias.html`
- Modify: `frontend/css/assistente-transferencia.css`
- Modify: `frontend/js/transferencia-api.js`
- Modify: `frontend/js/avaliar-transferencias.js`
- Modify: `frontend/js/core/routes.js`
- Modify: `frontend/js/transferencia-api.test.js`
- Create: `frontend/js/avaliar-transferencias.test.js`
- Modify: `frontend/js/painel.test.js`

**Interfaces:**
- Produces: `transferenciaService.consultarAutomacao(professorId)`.
- Produces: `transferenciaService.configurarAutomacao(professorId, ativa)`.
- Produces: função pura `automationViewModel(configuracao)` exportada em Node para teste.

- [ ] **Step 1: Escrever testes de API e apresentação**

```javascript
test('configura automação no endpoint do professor', async () => {
  let captured;
  const service = createTransferenciaService(async (url, options) => { captured = { url, options }; return { ativa: true }; });
  await service.configurarAutomacao(1, true);
  assert.equal(captured.url, 'http://localhost:8080/api/professores/1/transferencias/automacao');
  assert.equal(captured.options.method, 'PUT');
  assert.equal(captured.options.body, JSON.stringify({ ativa: true }));
});

test('modelo visual diferencia automação ativa e critérios ausentes', () => {
  assert.equal(automationViewModel({ ativa: true, possuiCriterios: true }).action, 'Desativar automação');
  assert.equal(automationViewModel({ ativa: false, possuiCriterios: false }).disabled, true);
});
```

- [ ] **Step 2: Executar os testes para confirmar a falha**

Run: `node --test frontend/js/transferencia-api.test.js frontend/js/avaliar-transferencias.test.js frontend/js/painel.test.js`

Expected: falha porque métodos e modelo visual ainda não existem.

- [ ] **Step 3: Implementar o painel com componentes existentes**

Adicionar a faixa `.ai-entry automation-entry` antes da lista, com estado, critérios, contador e botão:

```html
<button type="button" id="alternarAutomacao" class="star-border-button">
  <canvas class="star-border-light" aria-hidden="true"></canvas>
  <span class="ai-spark-icon" aria-hidden="true">✦</span>
  <span class="ai-button-copy"><strong id="automacaoAcao">Ativar automação</strong><small>Decisões com confiança mínima de 80%</small></span>
</button>
```

Carregar `css/assistente-transferencia.css` e `js/star-border.js`, manter o botão com 44 px ou mais, usar `aria-live` no feedback e renderizar selo “Avaliada pela IA” quando `avaliacaoAutomatica` for verdadeiro. Atualizar o card do painel para “Transferências e Automação”.

- [ ] **Step 4: Executar os testes do frontend do professor**

Run: `node --test frontend/js/transferencia-api.test.js frontend/js/avaliar-transferencias.test.js frontend/js/painel.test.js frontend/js/star-border.test.js`

Expected: todos os testes passam.

- [ ] **Step 5: Commitar**

```powershell
git add -- frontend/avaliar-transferencias.html frontend/css/assistente-transferencia.css frontend/js/transferencia-api.js frontend/js/avaliar-transferencias.js frontend/js/core/routes.js frontend/js/transferencia-api.test.js frontend/js/avaliar-transferencias.test.js frontend/js/painel.test.js
git commit -m "feat: adicionar painel visual de automação ao professor"
```

### Task 7: Refinar o catálogo de vantagens do aluno

**Files:**
- Modify: `frontend/resgatar-vantagens.html`
- Modify: `frontend/css/styles.css`
- Modify: `frontend/js/resgatar-vantagens.js`
- Create: `frontend/js/resgatar-vantagens.test.js`

**Interfaces:**
- Produces: `filtrarVantagens(vantagens, termo, somenteDisponiveis, saldo)`.
- Produces: `catalogViewModel(vantagem, saldo)` para disponibilidade, texto e estado do botão.

- [ ] **Step 1: Escrever testes da busca e disponibilidade**

```javascript
test('busca por nome, descrição ou empresa sem diferenciar acentos', () => {
  const itens = [{ nome: 'Café grátis', descricao: 'Bebida', empresaNome: 'Cantina' }];
  assert.equal(filtrarVantagens(itens, 'cafe', false, 0).length, 1);
  assert.equal(filtrarVantagens(itens, 'cantina', false, 0).length, 1);
});

test('filtro de saldo mantém somente vantagens resgatáveis', () => {
  const itens = [{ custoMoedas: 20 }, { custoMoedas: 80 }];
  assert.deepEqual(filtrarVantagens(itens, '', true, 50), [itens[0]]);
  assert.equal(catalogViewModel(itens[1], 50).disabled, true);
});
```

- [ ] **Step 2: Executar os testes para confirmar a falha**

Run: `node --test frontend/js/resgatar-vantagens.test.js`

Expected: falha porque funções exportáveis ainda não existem.

- [ ] **Step 3: Implementar filtros e refinamento mobile-first**

Adicionar campo de busca com label visível, checkbox “Cabe no meu saldo” e contador de resultados entre o hero e a grade. Normalizar texto com `normalize('NFD').replace(/[\u0300-\u036f]/g, '')`. Reaproveitar o catálogo em memória, sem chamadas adicionais à API. Aplicar `loading="lazy"` às imagens, limitar descrições sem truncar informações essenciais, exibir “Faltam N moedas” no cartão e manter o botão desabilitado com explicação acessível.

No CSS, manter uma coluna por padrão, duas em 640 px e três em 1024 px; usar unidades relativas, imagem com proporção estável, foco visível e controles com altura mínima de 44 px.

- [ ] **Step 4: Executar testes do catálogo e contratos centrais**

Run: `node --test frontend/js/resgatar-vantagens.test.js frontend/js/modal-gsap.test.js frontend/js/navigation.test.js`

Expected: todos os testes passam.

- [ ] **Step 5: Commitar**

```powershell
git add -- frontend/resgatar-vantagens.html frontend/css/styles.css frontend/js/resgatar-vantagens.js frontend/js/resgatar-vantagens.test.js
git commit -m "feat: refinar catálogo de vantagens do aluno"
```

### Task 8: Sincronizar, documentar e verificar a entrega completa

**Files:**
- Modify: `README.md`
- Modify: `demo (2)/demo/src/main/resources/static/**` por meio de `scripts/sync-frontend.ps1`

**Interfaces:**
- Consumes: todas as fontes frontend concluídas nas Tasks 6 e 7.
- Produces: cópia estática idêntica e instruções de uso da automação.

- [ ] **Step 1: Atualizar a documentação operacional**

Documentar que o professor precisa cadastrar critérios, pode ativar a automação na tela de transferências, que somente decisões com confiança mínima de 80% são executadas e que Ollama indisponível mantém pendências manuais.

- [ ] **Step 2: Sincronizar o frontend canônico**

Run: `.\scripts\sync-frontend.ps1`

Expected: `Frontend sincronizado (N arquivos).`

- [ ] **Step 3: Executar toda a suíte backend**

Run: `cd "demo (2)/demo"; .\mvnw.cmd test`

Expected: `BUILD SUCCESS`, zero falhas e zero erros.

- [ ] **Step 4: Executar toda a suíte frontend e checar sincronização**

Run: `node --test frontend/js/*.test.js frontend/js/core/*.test.js`

Expected: todos os testes passam.

Run: `.\scripts\test-frontend-sync.ps1`

Expected: frontend sincronizado.

- [ ] **Step 5: Executar verificações estáticas e visuais**

Run: `git diff --check`

Expected: nenhuma saída e código zero.

Iniciar a aplicação com `cd "demo (2)/demo"; .\mvnw.cmd spring-boot:run`, autenticar com as contas de demonstração e verificar `resgatar-vantagens.html?alunoId=4` e `avaliar-transferencias.html?professorId=1` em 375, 768, 1024 e 1440 px. Confirmar ausência de rolagem horizontal, foco visível, alvos de 44 px, filtro funcional, botão `star-border`, estado ativo/inativo e selo de auditoria.

- [ ] **Step 6: Commitar sincronização e documentação**

```powershell
git add -- README.md 'demo (2)/demo/src/main/resources/static'
git commit -m "docs: explicar automação e sincronizar interfaces"
```

- [ ] **Step 7: Confirmar histórico modular e árvore limpa**

Run: `git status --short; git log -10 --oneline`

Expected: árvore limpa e commits separados para especificação, plano, modelo, Ollama, automação, evento, API, interfaces e documentação.
