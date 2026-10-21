# Task 4 — Portais exclusivamente pelo backend

## Status

Implementação concluída no worktree `landing-saas-auth`. Nenhuma cópia de arquivos estáticos do Spring foi sincronizada nesta task.

## Implementação e decisões

- `createAlunoService` e `createEmpresaService` recebem `request`, `authApi` e `session`, exportam factories CommonJS para testes e mantêm as instâncias globais de navegador.
- Cadastro, consulta do próprio perfil, edição e exclusão usam somente os endpoints HTTP. Falhas são propagadas; não há usuários iniciais, armazenamento local de usuários, detecção de backend ou fallback.
- Login usa `AuthApi.login(email, senha, perfil)` e salva a identidade em `AppSession.current`. Leituras de sessão filtram o perfil esperado antes de permitir acesso ao recurso.
- A sessão permanece estritamente mínima: `id`, `nome`, `email` e `perfil`. Para preservar CPF/RG/endereço/curso/instituição/CNPJ após reload, foi adicionado `getMeusDados()` com GET do recurso próprio. Essa decisão foi validada pelo agente coordenador.
- Cadastro e atualização retornam o recurso completo para renderização imediata, mas entregam a persistência de sessão ao sanitizador compartilhado. Os dados completos usados pela edição ficam somente na memória do controlador.
- A sessão só é removida depois do sucesso da exclusão remota. Falhas de atualização/exclusão preservam a sessão.
- Inicialização dos controladores é assíncrona. Instituições são carregadas da API e a falha é exibida sem impedir o login. O login redireciona por `AppRoutes.dashboard`.
- O portal antigo foi movido de `index.html` para `aluno.html`. As marcas apontam para `index.html`, reservado à landing da Task 6. O link da empresa para o aluno e os dois acessos do menu de desenvolvimento foram atualizados.
- Foram removidos os atalhos de login com credenciais de demonstração.

## Arquivos

- Rename `frontend/index.html` → `frontend/aluno.html`.
- Alterados `frontend/empresa.html` e `frontend/dev-menu.html`.
- Reescritos `frontend/js/api.js` e `frontend/js/empresa-api.js`.
- Alterados `frontend/js/app.js` e `frontend/js/empresa.js`.
- Criados `frontend/js/api.test.js` e `frontend/js/empresa-api.test.js`.
- Criado este relatório.

## TDD — evidências RED/GREEN

1. Antes da reescrita, `node --test frontend/js/api.test.js frontend/js/empresa-api.test.js` encerrou com código 1: os dois módulos antigos dependiam de `window` no import e não exportavam factories. Resultado inicial: 0 pass, 2 fail.
2. A ausência das factories também foi confirmada isoladamente nos arquivos originais obtidos por `git show HEAD:<arquivo>`, executados em VM com ambiente de navegador compatível: `createAlunoService` e `createEmpresaService` resultaram em `undefined`, quando o contrato exige `function`.
3. Depois da implementação dos serviços, o comando direcionado passou com 17 testes, 0 falhas.
4. Foram adicionados três testes executando os controladores reais em VM com DOM controlado, antes da adaptação dos controladores: restauração/edição do perfil completo, redirecionamento de login e falha de instituições mantendo login operacional. RED: 17 pass, 3 fail, por uso síncrono de uma Promise de instituições (`insts.forEach is not a function`).
5. Depois da adaptação assíncrona dos controladores, o comando direcionado passou com 20 testes, 0 falhas.
6. Verificação completa: `node --test` passou com **41 testes, 41 pass, 0 fail**.
7. `git diff --check` não apontou erros de whitespace. O Git apenas informou a política existente de conversão LF/CRLF no Windows.
8. Busca nos seis arquivos de produção envolvidos não encontrou `localStorage`, `STORAGE_*`, `checkBackend`, `initStorage` ou `btnQuickLogin`.

Os testes exercitam serviços reais, `createSessionStore` real e controladores reais. As dependências HTTP são substituídas na fronteira externa; o DOM controlado permite validar conteúdo exibido, campos de edição, eventos e navegação sem navegador real.

## Auto-revisão

- Verificados métodos HTTP, URLs, corpos JSON e propagação do erro original.
- Verificado que sessão inválida/de outro perfil não dispara consulta, edição ou exclusão remota.
- Verificado que nenhum dado extra ou senha entra na sessão sanitizada.
- Verificada restauração dos detalhes pelo backend e edição dos campos que a sessão mínima não contém.
- Verificada preservação das telas de cadastro, perfil, edição e exclusão; login segue a nova navegação para o painel.
- Conferida a ordem dos scripts compartilhados nos dois portais e os destinos dos links do portal do aluno.
- Mantido o escopo em `frontend/` e neste relatório; nenhuma alteração no backend ou nos demais fluxos funcionais.

## Preocupações e limites

- `frontend/index.html` fica ausente até a Task 6 criar a landing, conforme solicitado. O painel também será integrado em task posterior; o redirecionamento já consome o contrato compartilhado.
- A URL base `http://localhost:8080/api` segue o contrato existente e o brief.
- Esta task não realizou teste visual em navegador nem integração contra um backend em execução; cobertura automatizada usa factories e execução dos controladores em VM.
- Chaves antigas eventualmente existentes em navegadores não são lidas nem atualizadas; esta task não executa migração/limpeza destrutiva de dados antigos.
