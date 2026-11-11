# Design da modularização do sistema

## Objetivo

Reduzir arquivos com responsabilidades misturadas e duplicação acidental sem alterar endpoints HTTP, formatos JSON, URLs das telas ou regras de negócio existentes.

## Estratégia

A mudança será incremental e organizada em commits atômicos. O backend continuará em MVC, mas regras auxiliares hoje concentradas em serviços grandes serão extraídas para componentes com uma responsabilidade. O frontend continuará sem etapa obrigatória de build, com módulos globais pequenos e explícitos para HTTP, sessão, apresentação e formulários.

Os arquivos de `frontend/` permanecem como fonte editável para uso com Live Server. A cópia servida pelo Spring em `src/main/resources/static` será atualizada por um script determinístico, evitando edições manuais divergentes.

## Módulos

### Frontend

- `core/http.js`: requisições HTTP, leitura segura da resposta e mensagens de erro.
- `core/session.js`: leitura, escrita e limpeza do usuário autenticado.
- `core/format.js`: formatação de moeda, data e escape de HTML.
- `core/dom.js`: seletores e atualização de mensagens de interface.
- APIs por domínio consomem `core/http.js` e deixam de duplicar `fetch` e tratamento de erro.
- Controladores de página consomem sessão e formatação compartilhadas.
- CSS será separado em base, componentes e páginas, mantendo um agregador compatível em `styles.css`.

### Backend

- Transferências: separar consulta, validação/transição de estado e movimentação de saldo.
- Distribuição e resgate: compartilhar operações financeiras pequenas somente quando houver semântica idêntica; regras específicas permanecem no serviço de aplicação.
- Controllers continuam responsáveis apenas pelo protocolo HTTP.
- Repositórios e entidades preservam nomes e mapeamentos para evitar migração de banco desnecessária.

### Ferramentas

- Um script PowerShell sincroniza `frontend/` com os recursos estáticos do Spring.
- Um teste automatizado verifica que as duas árvores permanecem idênticas.
- A documentação aponta a fonte canônica e o comando de sincronização.

## Compatibilidade

- Nenhuma URL pública será alterada.
- Nenhum campo de request ou response será renomeado.
- O comportamento visual e as animações GSAP existentes serão preservados.
- O projeto continua executável com Maven e Java 21, sem bundler JavaScript.

## Verificação

Cada extração deve manter a suíte Maven verde. Módulos frontend puros terão testes com `node:test` quando houver lógica observável; a sincronização terá verificação própria. Antes do envio, serão executados testes completos, build e conferência de árvore Git limpa.

## Estratégia de commits

Cada commit deve entregar uma fronteira compreensível e reversível: documentação, infraestrutura de sincronização, cada módulo core, migração de um grupo de APIs, extrações backend e CSS. Não serão criados commits vazios ou divisões artificiais de uma mesma mudança inseparável.
