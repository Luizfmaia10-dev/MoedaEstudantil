# Task 3 — Sessão, cliente de autenticação e catálogo de rotas

## Implementação

- `AppSession` persiste somente `id`, `nome`, `email` e `perfil` em `sessionStorage`; sanitiza gravação/leitura e remove JSON inválido ou corrompido.
- `AppRoutes.dashboard()` aponta para `painel.html`. `cardsFor(session)` gera cartões isolados por perfil (ALUNO, PROFESSOR, EMPRESA), com cópias dos objetos e o ID recebido nos URLs.
- `AuthApi.login(email, senha, perfil)` encaminha as credenciais ao `POST /api/auth/login`, aparando o email e propagando o resultado/erro do cliente HTTP.
- Os três módulos expõem API compatível com navegador e CommonJS e podem ser testados sem DOM.

## Arquivos

- `frontend/js/core/session.js` e `session.test.js`
- `frontend/js/core/routes.js` e `routes.test.js`
- `frontend/js/auth-api.js` e `auth-api.test.js`

## Testes e evidência RED/GREEN

- RED: `node --test frontend/js/core/session.test.js frontend/js/core/routes.test.js frontend/js/auth-api.test.js` falhou nos três arquivos por `MODULE_NOT_FOUND` dos módulos ainda não criados, conforme esperado.
- GREEN: após implementar os módulos, a mesma execução passou: 14 testes, 0 falhas.
- Suite completa: `node --test` passou: 20 testes, 0 falhas.
- `git diff --check` não reportou problemas de whitespace.

## Auto-revisão

- A sessão cria novo objeto com exatamente os quatro campos permitidos, sem persistir senha, CPF ou outros campos extras.
- A leitura invalida perfis fora da lista aceita e dados malformados; `clear()` remove a chave versionada.
- O catálogo não compartilha objetos mutáveis e recusa perfil/ID inválidos.
- O cliente não captura nem transforma erros, preservando `AppHttp.HttpError`.
- Nenhum portal ou página de landing foi alterado.

## Preocupações

- A URL base do backend permanece literal `http://localhost:8080`, como indicado no contrato fornecido.
- Os cartões de professor e empresa refletem ações ativas já listadas em `frontend/dev-menu.html`; recursos marcados como passivos foram omitidos.
