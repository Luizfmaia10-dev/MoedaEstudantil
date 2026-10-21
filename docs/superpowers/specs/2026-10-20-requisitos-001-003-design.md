# Design dos requisitos RF-001, RF-002 e RF-003

## Objetivo

Implementar autenticação dos três perfis, seleção restrita de instituição no cadastro do aluno e crédito cumulativo semestral de 1.000 moedas aos professores, preservando os contratos existentes e o trabalho concorrente de reformulação do menu.

## Restrições de integração

- Não modificar `frontend/dev-menu.html`, navegação global ou arquivos com alterações concorrentes não confirmadas.
- Criar interfaces novas em arquivos isolados quando a integração visual depender do menu em reformulação.
- Manter os endpoints existentes de alunos, empresas, distribuição, resgate, extrato e transferências.
- Manter `frontend/` como fonte canônica e sincronizar os recursos estáticos pelo script existente.

## RF-001 - Autenticação

Um `AutenticacaoService` localizará o usuário por e-mail entre alunos, professores e empresas e validará a senha com BCrypt. O endpoint `POST /api/auth/login` receberá e-mail e senha e devolverá somente os dados necessários à interface: identificador, nome, e-mail e perfil (`ALUNO`, `PROFESSOR` ou `EMPRESA`). Senhas nunca serão devolvidas.

Cadastros e atualizações de aluno e empresa passarão a gravar senhas BCrypt. Para preservar os usuários pré-cadastrados em texto puro, o login aceitará a senha legada uma vez e atualizará imediatamente o registro para BCrypt.

O frontend compartilhará um módulo de autenticação e sessão. Os portais existentes de aluno e empresa usarão o backend quando disponível. O professor receberá uma página própria e isolada com login e atalhos de suas funcionalidades; sua entrada no menu em reformulação ficará para integração posterior, evitando conflito.

## RF-002 - Instituições participantes

As opções do cadastro serão carregadas de `GET /api/instituicoes`. O backend exigirá `instituicaoId` e rejeitará identificadores inexistentes; o aluno nunca poderá criar uma instituição pelo fluxo público. A mesma validação será usada em cadastro e atualização.

A lógica de cadastro sairá do controller para `AlunoService`, permitindo testes unitários de instituição obrigatória, instituição inexistente, e-mail duplicado e criação da conta com saldo zero.

## RF-003 - Crédito semestral

Os semestres serão fixos: 1 para janeiro-junho e 2 para julho-dezembro. Uma entidade de auditoria registrará professor, ano, semestre, quantidade e data do crédito, com restrição única por professor/ano/semestre.

`CreditoSemestralService` processará todos os professores dentro de transação, bloqueará a conta, somará 1.000 ao saldo corrente e registrará a auditoria. Reexecuções no mesmo semestre não alterarão o saldo. Um agendador executará o processamento ao iniciar a aplicação e diariamente; créditos não usados permanecem acumulados.

## Erros e segurança

- Credenciais inválidas retornam HTTP 401 com mensagem genérica.
- Cadastro inválido, duplicado ou sem instituição retorna HTTP 400/409 conforme o caso.
- Professor sem conta não recebe registro parcial; a transação falha para aquele processamento.
- Concorrência é protegida pela restrição única e bloqueio pessimista da conta.

## Testes

- Autenticação dos três perfis, senha incorreta, e-mail inexistente e migração de senha legada.
- Cadastro de aluno com instituição válida e rejeições de instituição ausente/inexistente.
- Primeiro crédito semestral, idempotência, acúmulo entre semestres e conta inexistente.
- Testes de controller para status e payloads principais.
- Testes JavaScript do módulo compartilhado, sincronização do frontend, suíte Maven completa e build.

## Commits

Os commits serão divididos por unidade verificável: documentação, segurança de senha, autenticação, cadastro/instituições, domínio do semestre, persistência do crédito, agendamento, frontend compartilhado, portal do professor e documentação final. Nenhum commit incluirá arquivos pertencentes ao trabalho concorrente do menu.
