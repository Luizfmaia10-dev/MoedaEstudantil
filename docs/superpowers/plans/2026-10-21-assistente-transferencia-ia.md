# Assistente de Transferência com IA Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adicionar à página de transferência um assistente conversacional com IA local, confirmação explícita e um CTA que reproduza fielmente o efeito Star Border fornecido.

**Architecture:** A View mantém a conversa somente em memória e envia cada mensagem com o rascunho atual. Um Controller dedicado chama um Service que usa uma porta de interpretação por IA, resolve nomes somente nos registros da instituição e devolve um DTO seguro; a confirmação continua chamando o endpoint persistente já existente. A implementação Ollama usa `qwen2.5:0.5b-instruct` e saída JSON estruturada, sem expor o modelo diretamente ao navegador.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Spring MVC, Jackson, Ollama, Qwen2.5 0.5B Instruct, HTML5, CSS3, JavaScript, Canvas 2D, GSAP, Node Test Runner, JUnit 5 e Mockito.

**Spec:** `docs/superpowers/specs/2026-10-21-assistente-transferencia-ia-design.md`

## Global Constraints

- Não criar commits durante esta execução, conforme solicitação do usuário.
- Manter `frontend/` como fonte canônica e sincronizar a árvore estática do Spring somente após testes do frontend.
- A IA interpreta texto, mas não persiste nem movimenta moedas.
- A criação da solicitação continua exclusivamente em `SolicitacaoTransferenciaService.criar(...)`.
- O formulário manual deve funcionar mesmo quando o Ollama estiver indisponível.
- O CTA deve reproduzir o algoritmo Canvas do Star Border fornecido: estrela e trilha no contorno exato, volta no hover/foco e pulso no clique.
- A interface deve ser mobile-first, ter alvos de toque de no mínimo 44x44px e respeitar `prefers-reduced-motion`.
- Nenhuma chave de API ou serviço externo será usado.

---

### Task 1: Porta de IA e cliente Ollama estruturado

**Files:**
- Create: `demo (2)/demo/src/main/java/moedaEstudantil/service/InterpretadorTransferenciaIA.java`
- Create: `demo (2)/demo/src/main/java/moedaEstudantil/service/OllamaInterpretadorTransferenciaIA.java`
- Create: `demo (2)/demo/src/test/java/moedaEstudantil/service/OllamaInterpretadorTransferenciaIATest.java`
- Modify: `demo (2)/demo/src/main/resources/application.properties`

**Interfaces:**
- Produces: `InterpretadorTransferenciaIA.interpretar(String mensagem, Rascunho rascunho)` retornando `Intencao(destinatario, professor, quantidade, justificativa)`.
- Produces: propriedades `app.ai.ollama.url`, `app.ai.ollama.model` e `app.ai.ollama.timeout`.

- [ ] **Step 1: Escrever o teste falho do contrato HTTP e da resposta estruturada**

Usar um `HttpServer` local no teste para capturar o POST e responder:

```json
{"message":{"content":"{\"destinatario\":\"Ana\",\"professor\":\"Carlos\",\"quantidade\":20,\"justificativa\":null}"}}
```

O teste deve afirmar que a intenção devolvida contém `Ana`, `Carlos` e `20`, que o corpo enviado usa `stream: false`, `temperature: 0` e um schema JSON em `format`.

- [ ] **Step 2: Executar o teste e confirmar RED**

Run: `cd "demo (2)/demo"; .\mvnw.cmd -Dtest=OllamaInterpretadorTransferenciaIATest test`

Expected: FAIL porque a porta e o cliente ainda não existem.

- [ ] **Step 3: Implementar a porta e o cliente mínimo**

Definir a porta com records imutáveis:

```java
public interface InterpretadorTransferenciaIA {
    record Rascunho(String destinatario, String professor, Integer quantidade, String justificativa) {}
    record Intencao(String destinatario, String professor, Integer quantidade, String justificativa) {}
    Intencao interpretar(String mensagem, Rascunho rascunho);
}
```

O cliente deve usar `java.net.http.HttpClient`, Jackson e POST em `${app.ai.ollama.url}/api/chat`; o prompt de sistema limita a saída aos quatro campos, instrui português brasileiro e manda conservar valores já presentes no rascunho. Timeout e falhas de transporte devem virar `OllamaIndisponivelException` interna ao serviço, sem vazar corpo remoto.

- [ ] **Step 4: Executar o teste e confirmar GREEN**

Run: `cd "demo (2)/demo"; .\mvnw.cmd -Dtest=OllamaInterpretadorTransferenciaIATest test`

Expected: PASS.

---

### Task 2: Resolução determinística da intenção

**Files:**
- Create: `demo (2)/demo/src/main/java/moedaEstudantil/dto/AssistenteTransferenciaDTO.java`
- Create: `demo (2)/demo/src/main/java/moedaEstudantil/service/AssistenteTransferenciaService.java`
- Create: `demo (2)/demo/src/test/java/moedaEstudantil/service/AssistenteTransferenciaServiceTest.java`
- Modify: `demo (2)/demo/src/main/java/moedaEstudantil/repository/ProfessorRepository.java`

**Interfaces:**
- Consumes: `InterpretadorTransferenciaIA` da Task 1.
- Produces: `AssistenteTransferenciaService.interpretar(Long alunoId, String mensagem, RascunhoAssistente rascunho)`.
- Produces: `AssistenteTransferenciaDTO` com `estado`, `mensagem`, campos resolvidos e `opcoes`.

- [ ] **Step 1: Escrever testes falhos de resolução e segurança**

Cobrir, separadamente:

```java
assertEquals("PRONTO", resposta.estado());
assertEquals(6L, resposta.destinatarioId());
assertEquals(1L, resposta.avaliadorId());
assertEquals(20, resposta.quantidade());
verifyNoInteractions(solicitacaoRepository);
```

Também cobrir nome ambíguo (`PRECISA_DESTINATARIO` com opções), professor ausente (`PRECISA_PROFESSOR`), quantidade nula/zero (`PRECISA_QUANTIDADE`), colega de outra instituição não exposto e aluno inexistente com HTTP 404.

- [ ] **Step 2: Executar o teste e confirmar RED**

Run: `cd "demo (2)/demo"; .\mvnw.cmd -Dtest=AssistenteTransferenciaServiceTest test`

Expected: FAIL porque DTO e serviço não existem.

- [ ] **Step 3: Implementar resolução mínima**

Adicionar ao repositório:

```java
List<Professor> findByInstituicaoIdOrderByNome(Long instituicaoId);
```

Normalizar nomes com `Normalizer.Form.NFD`, remover marcas diacríticas e comparar em minúsculas. A ordem do estado deve ser: quantidade inválida, destinatário ausente/ambíguo, professor ausente/ambíguo e `PRONTO`. IDs já escolhidos no rascunho só podem ser aceitos se pertencerem às listas carregadas para a instituição atual.

- [ ] **Step 4: Executar o teste e confirmar GREEN**

Run: `cd "demo (2)/demo"; .\mvnw.cmd -Dtest=AssistenteTransferenciaServiceTest test`

Expected: PASS.

---

### Task 3: Endpoint de interpretação sem efeito financeiro

**Files:**
- Create: `demo (2)/demo/src/main/java/moedaEstudantil/controller/AssistenteTransferenciaController.java`
- Create: `demo (2)/demo/src/test/java/moedaEstudantil/controller/AssistenteTransferenciaControllerTest.java`

**Interfaces:**
- Consumes: `AssistenteTransferenciaService.interpretar(...)` da Task 2.
- Produces: `POST /api/alunos/{alunoId}/transferencias/assistente/interpretar`.

- [ ] **Step 1: Escrever teste MVC falho**

Enviar:

```json
{
  "mensagem":"mande vinte moedas para Ana",
  "rascunho":{"destinatarioId":null,"avaliadorId":null,"quantidade":null,"justificativa":null}
}
```

Asserções: HTTP 200, `estado` e campos do DTO; mensagem em branco retorna HTTP 400; indisponibilidade do Ollama retorna HTTP 503 com mensagem pública curta.

- [ ] **Step 2: Executar o teste e confirmar RED**

Run: `cd "demo (2)/demo"; .\mvnw.cmd -Dtest=AssistenteTransferenciaControllerTest test`

Expected: FAIL porque o endpoint ainda não existe.

- [ ] **Step 3: Implementar controller mínimo**

Usar records de request aninhados ao controller, validar `mensagem == null || mensagem.isBlank()` e delegar apenas ao serviço de interpretação. Não injetar `SolicitacaoTransferenciaService` neste controller.

- [ ] **Step 4: Executar o teste e confirmar GREEN**

Run: `cd "demo (2)/demo"; .\mvnw.cmd -Dtest=AssistenteTransferenciaControllerTest test`

Expected: PASS.

---

### Task 4: Cliente frontend e estado conversacional

**Files:**
- Create: `frontend/js/assistente-transferencia.js`
- Create: `frontend/js/assistente-transferencia.test.js`
- Modify: `frontend/js/transferencia-api.js`
- Create: `frontend/js/transferencia-api.test.js`

**Interfaces:**
- Produces: `transferenciaService.interpretar(alunoId, mensagem, rascunho)`.
- Produces: `createAssistantState()`, `mergeInterpretation(state, resposta)` e `initializeTransferAssistant(deps)`.

- [ ] **Step 1: Escrever testes JavaScript falhos**

Cobrir o payload literal do endpoint, preservação do rascunho entre mensagens, opções ambíguas, bloqueio de confirmação sem estado `PRONTO`, confirmação chamando `transferenciaService.solicitar(...)` exatamente uma vez e fallback manual após erro 503.

- [ ] **Step 2: Executar os testes e confirmar RED**

Run: `node --test frontend/js/transferencia-api.test.js frontend/js/assistente-transferencia.test.js`

Expected: FAIL por módulos/funções ausentes.

- [ ] **Step 3: Implementar API e máquina de estados mínima**

O módulo deve exportar as funções no Node e anexá-las ao navegador pelo padrão IIFE já usado no projeto. Os estados públicos serão `INICIAL`, `INTERPRETANDO`, `PRECISA_QUANTIDADE`, `PRECISA_DESTINATARIO`, `PRECISA_PROFESSOR`, `PRONTO`, `CONFIRMANDO`, `SUCESSO` e `INDISPONIVEL`.

- [ ] **Step 4: Executar os testes e confirmar GREEN**

Run: `node --test frontend/js/transferencia-api.test.js frontend/js/assistente-transferencia.test.js`

Expected: PASS.

---

### Task 5: Efeito Star Border fiel em JavaScript puro

**Files:**
- Create: `frontend/js/star-border.js`
- Create: `frontend/js/star-border.test.js`

**Interfaces:**
- Produces: `createStarBorder(root, options)` retornando `{ destroy(), pulse() }`.
- Consumes: um botão contendo `<canvas class="star-border-light" aria-hidden="true"></canvas>`.

- [ ] **Step 1: Escrever testes falhos do comportamento observável**

Usar o Chromium de suporte para verificar que o Canvas acompanha as dimensões do botão, hover inicia uma volta adicional, clique inicia dois pulsos no ponto pressionado, foco por teclado equivale ao hover e movimento reduzido mantém o contorno estático sem loop contínuo.

- [ ] **Step 2: Executar o teste e confirmar RED**

Run: `node --test frontend/js/star-border.test.js`

Expected: FAIL porque o adaptador ainda não existe.

- [ ] **Step 3: Adaptar fielmente o algoritmo fornecido**

Transportar para JavaScript puro, sem alterar as constantes funcionais:

```js
const BLEED = 48;
const LAP = 0.9;
const TAU = Math.PI * 2;
```

Preservar `outline`, `pointAt`, `project`, `drawTrail`, `drawHead`, poeira opcional, `ResizeObserver`, `IntersectionObserver`, volta no hover/foco e pulso bilateral no clique. Para o CTA usar:

```js
createStarBorder(button, {
  color: '#b8ff6a',
  trailColor: '#63e6a6',
  duration: 4,
  direction: 'clockwise',
  stars: 1,
  trailLength: 0.3,
  thickness: 1,
  radius: 18,
  glow: 0.85,
  hover: 'lap',
  sparkle: true,
  clickPulse: true,
  theme: 'dark'
});
```

- [ ] **Step 4: Executar o teste e confirmar GREEN**

Run: `node --test frontend/js/star-border.test.js`

Expected: PASS.

---

### Task 6: Painel visual, GSAP e integração com formulário

**Files:**
- Modify: `frontend/solicitar-transferencia.html`
- Create: `frontend/css/assistente-transferencia.css`
- Modify: `frontend/js/solicitar-transferencia.js`
- Modify: `frontend/js/assistente-transferencia.test.js`

**Interfaces:**
- Consumes: Tasks 4 e 5, `window.modalGsap` e `window.transferenciaService.solicitar(...)`.
- Produces: CTA `#abrirAssistenteIA`, diálogo `#assistenteDialog` e resumo `#assistenteResumo`.

- [ ] **Step 1: Escrever teste falho de interface responsiva e acessível**

No Chromium, carregar o HTML real e afirmar: CTA e Canvas presentes; tamanho mínimo 44x44px; diálogo com nome acessível; região de mensagens com `aria-live="polite"`; foco inicial no campo; ausência de overflow em 375, 768, 1024 e 1440px; botão de confirmação indisponível até o estado `PRONTO`.

- [ ] **Step 2: Executar o teste e confirmar RED**

Run: `node --test frontend/js/assistente-transferencia.test.js frontend/js/star-border.test.js`

Expected: FAIL pelos elementos ainda ausentes.

- [ ] **Step 3: Implementar marcação e estilos mobile-first**

O CTA ficará antes do formulário manual, com texto “Transferir com IA”, subtítulo “Descreva em português” e ícone de quatro pontas. O diálogo terá cabeçalho, histórico, sugestões, composer e cartão de confirmação. Usar `dialog`, labels reais, `aria-live`, foco visível, `clamp()` e media queries `min-width: 768px` e `min-width: 1024px`.

- [ ] **Step 4: Integrar comportamento e animações**

Carregar GSAP, `modal-gsap.js`, `star-border.js` e `assistente-transferencia.js` antes de `solicitar-transferencia.js`. Abrir/fechar pelo animador existente; ao confirmar, chamar o endpoint atual, atualizar saldo/lista, fechar com GSAP e anunciar sucesso. “Usar formulário manual” deve preencher todos os campos já resolvidos e focar o primeiro campo ausente.

- [ ] **Step 5: Executar testes e confirmar GREEN**

Run: `node --test frontend/js/assistente-transferencia.test.js frontend/js/star-border.test.js frontend/js/transferencia-api.test.js`

Expected: PASS.

---

### Task 7: Documentação, sincronização e instalação local

**Files:**
- Modify: `README.md`
- Modify via sync: `demo (2)/demo/src/main/resources/static/**`

**Interfaces:**
- Consumes: implementação completa das Tasks 1–6.
- Produces: instruções reproduzíveis de instalação e frontend servido pelo Spring.

- [ ] **Step 1: Atualizar README**

Adicionar Ollama aos pré-requisitos e os comandos:

```powershell
winget install --id Ollama.Ollama -e
ollama pull qwen2.5:0.5b-instruct
ollama run qwen2.5:0.5b-instruct
```

Documentar as propriedades opcionais, o endpoint somente interpretativo, a confirmação explícita e que apenas a solicitação confirmada é persistida.

- [ ] **Step 2: Instalar/verificar Ollama e baixar o modelo**

Run: `ollama --version`

Se o executável estiver ausente, executar `winget install --id Ollama.Ollama -e --accept-package-agreements --accept-source-agreements`; depois executar `ollama pull qwen2.5:0.5b-instruct`.

- [ ] **Step 3: Sincronizar frontend**

Run: `.\scripts\sync-frontend.ps1`

Expected: `Frontend sincronizado`.

- [ ] **Step 4: Verificar sincronização**

Run: `.\scripts\test-frontend-sync.ps1`

Expected: exit code 0.

---

### Task 8: Verificação completa e teste de uso

**Files:**
- Verify only: all modified files.

**Interfaces:**
- Consumes: aplicação e modelo local completos.
- Produces: evidência final sem commit.

- [ ] **Step 1: Executar toda a suíte JavaScript**

Run: `node --test frontend/js/*.test.js`

Expected: todos os testes aprovados.

- [ ] **Step 2: Executar toda a suíte Maven**

Run: `cd "demo (2)/demo"; .\mvnw.cmd test`

Expected: `BUILD SUCCESS` e zero falhas.

- [ ] **Step 3: Executar verificação estática do diff**

Run: `git diff --check`

Expected: nenhuma saída de erro.

- [ ] **Step 4: Executar teste real da integração**

Com Ollama, MySQL e Spring ativos, abrir `http://localhost:8080/solicitar-transferencia.html?alunoId=4`, enviar “mande vinte moedas para Colega PUC pelo professor Glender”, revisar o cartão e confirmar. Verificar que a nova solicitação aparece como `PENDENTE` e que o chat não é salvo após recarregar.

- [ ] **Step 5: Conferir alterações sem criar commit**

Run: `git status --short; git diff --stat`

Expected: arquivos da funcionalidade modificados e nenhum novo commit após `e2cf0f3`.
