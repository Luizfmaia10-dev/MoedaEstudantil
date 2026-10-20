# Modularização do Sistema Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Modularizar backend, frontend e sincronização de recursos sem mudar os contratos públicos.

**Architecture:** O frontend ganha um núcleo reutilizável sem bundler e mantém APIs por domínio. O backend extrai responsabilidades do serviço de transferência para colaboradores Spring testáveis. `frontend/` é a fonte canônica e um script replica seu conteúdo para os recursos estáticos.

**Tech Stack:** Java 21, Spring Boot 4, JPA, JUnit 5, Mockito, JavaScript, Node.js `node:test`, CSS, PowerShell.

**Spec:** `docs/superpowers/specs/2026-10-19-modularizacao-design.md`

## Global Constraints

- Preservar URLs, payloads JSON, regras de negócio e mapeamentos JPA.
- Preservar o funcionamento direto das páginas sem bundler.
- Preservar animações GSAP dos modais.
- Cada tarefa concluída recebe um commit atômico.

---

### Task 1: Sincronização determinística do frontend

**Files:**
- Create: `scripts/sync-frontend.ps1`
- Create: `scripts/test-frontend-sync.ps1`
- Modify: `README.md`

**Interfaces:**
- Consumes: árvore canônica `frontend/`.
- Produces: cópia idêntica em `demo (2)/demo/src/main/resources/static/`.

- [ ] Criar teste que compara caminhos e hashes e executá-lo com uma alteração temporária para observar falha.
- [ ] Criar script com cópia por `Copy-Item`, remoção apenas de arquivos estáticos órfãos e validação dos caminhos absolutos.
- [ ] Executar sincronização e o teste até passar.
- [ ] Documentar os comandos e criar commit.

### Task 2: Núcleo HTTP do frontend

**Files:**
- Create: `frontend/js/core/http.js`
- Create: `frontend/js/core/http.test.js`
- Modify: `frontend/*html`

**Interfaces:**
- Produces: `window.AppHttp.request(url, options)` e `window.AppHttp.message(error)`.

- [ ] Escrever testes para JSON, texto vazio e erro HTTP; confirmar falha por módulo ausente.
- [ ] Implementar o núcleo HTTP mínimo e executar `node --test frontend/js/core/http.test.js`.
- [ ] Incluir o script antes das APIs, sincronizar e criar commit.

### Task 3: Migrar APIs de consulta

**Files:**
- Modify: `frontend/js/criterios-api.js`
- Modify: `frontend/js/extrato-api.js`
- Modify: `frontend/js/vantagens-api.js`
- Modify: `frontend/js/distribuicao-api.js`
- Modify: `frontend/js/resgate-api.js`

**Interfaces:**
- Consumes: `AppHttp.request`.
- Produces: as mesmas APIs globais e assinaturas atuais.

- [ ] Substituir `fetch` duplicado sem mudar objetos exportados.
- [ ] Executar testes do núcleo, sincronização e smoke check sintático; criar commit.

### Task 4: Migrar APIs de cadastro e transferência

**Files:**
- Modify: `frontend/js/api.js`
- Modify: `frontend/js/empresa-api.js`
- Modify: `frontend/js/transferencia-api.js`

**Interfaces:**
- Consumes: `AppHttp.request`.
- Produces: `api`, `empresaApi` e `transferenciaApi` compatíveis.

- [ ] Migrar chamadas preservando endpoints e payloads.
- [ ] Executar testes e sincronização; criar commit.

### Task 5: Sessão e apresentação compartilhadas

**Files:**
- Create: `frontend/js/core/session.js`
- Create: `frontend/js/core/format.js`
- Create: `frontend/js/core/core.test.js`
- Modify: controladores de páginas que repetem sessão/formatação.

**Interfaces:**
- Produces: `AppSession`, `AppFormat.currency`, `AppFormat.date` e `AppFormat.escapeHtml`.

- [ ] Escrever testes de leitura de sessão e escape/formatação; confirmar falha.
- [ ] Implementar módulos e migrar consumidores sem mudar a UI.
- [ ] Executar testes, sincronização e criar commits separados para sessão e formatação.

### Task 6: CSS por responsabilidade

**Files:**
- Create: `frontend/css/base.css`
- Create: `frontend/css/components.css`
- Create: `frontend/css/pages.css`
- Modify: `frontend/css/styles.css`

**Interfaces:**
- `styles.css` permanece o único link público e importa os três módulos.

- [ ] Separar regras na ordem base, componentes e páginas.
- [ ] Conferir imports, sincronizar, executar smoke visual automatizado e criar commit.

### Task 7: Serviço de domínio para movimentação de saldo

**Files:**
- Create: `demo (2)/demo/src/main/java/moedaEstudantil/service/MovimentacaoSaldoService.java`
- Create: `demo (2)/demo/src/test/java/moedaEstudantil/service/MovimentacaoSaldoServiceTest.java`
- Modify: serviços que debitam ou creditam contas.

**Interfaces:**
- Produces: métodos `debitar(Conta, BigDecimal)` e `creditar(Conta, BigDecimal)` com validação central.

- [ ] Escrever testes de saldo insuficiente, débito e crédito e confirmar falha de compilação.
- [ ] Implementar componente e migrar serviços.
- [ ] Executar testes focados e suíte Maven; criar commit.

### Task 8: Decompor solicitações de transferência

**Files:**
- Create: componentes em `demo (2)/demo/src/main/java/moedaEstudantil/service/transferencia/`.
- Create: testes correspondentes.
- Modify: `SolicitacaoTransferenciaService.java`.

**Interfaces:**
- Produces: resolvedor de participantes e executor transacional; o serviço de aplicação mantém sua API pública.

- [ ] Escrever testes focados nas novas interfaces e confirmar falha.
- [ ] Extrair resolução de entidades e execução financeira mantendo a transação externa.
- [ ] Executar testes focados e suíte Maven; criar commits por extração.

### Task 9: Validação final e entrega

**Files:**
- Modify: `README.md` somente se os comandos finais exigirem correção.

**Interfaces:**
- Produces: branch `main` validada e enviada a `origin/main`.

- [ ] Executar testes Node, teste de sincronização, `mvnw.cmd test` e `mvnw.cmd package -DskipTests`.
- [ ] Executar `git diff --check`, conferir histórico e árvore limpa.
- [ ] Enviar com `git push origin main`.
