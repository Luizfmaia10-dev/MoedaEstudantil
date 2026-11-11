# Landing SaaS e autenticação persistida

**Data:** 2026-10-20  
**Status:** aprovado para preparação da implementação

## Contexto

O Sistema de Moeda Estudantil já possui funcionalidades separadas para aluno, professor e empresa parceira, servidas por Spring Boot com persistência JPA/MySQL. A entrada atual, porém, começa diretamente no formulário do aluno, o menu central é uma tela temporária de desenvolvimento e os logins de aluno e empresa utilizam dados mantidos no navegador. O professor não possui um fluxo de login próprio.

A reformulação deve transformar a entrada do produto em uma apresentação comercial com aparência de SaaS, comunicar valor antes de pedir credenciais e oferecer um acesso unificado aos três perfis. O banco de dados será a única fonte oficial de usuários; o navegador manterá apenas os dados mínimos da sessão corrente.

## Objetivos

- Criar uma landing page comercial, futurista e responsiva para apresentar o produto.
- Aplicar uma identidade própria inspirada na referência fornecida: verde profundo, lima elétrica, tipografia pesada, formas arredondadas e profundidade 3D.
- Oferecer login selecionável para aluno, professor e empresa parceira.
- Preservar cadastro para aluno e empresa; professores permanecem pré-cadastrados.
- Autenticar credenciais contra usuários persistidos pelo backend.
- Direcionar cada perfil às funcionalidades existentes que lhe pertencem.
- Remover o uso de `localStorage` como base alternativa de usuários.
- Manter `frontend/` como fonte canônica e sincronizar a cópia estática do Spring Boot.

## Fora de escopo

- Migrar o frontend para React, Vue ou outro framework.
- Reescrever todas as telas funcionais existentes.
- Implantar autorização completa por endpoint, JWT, OAuth ou recuperação de senha.
- Criar novos requisitos de negócio além dos descritos no projeto.
- Alterar diagramas UML que não forem diretamente afetados pela autenticação.

O login desta entrega valida credenciais no servidor e cria uma sessão de interface no navegador. A proteção integral de todas as APIs por identidade autenticada é uma evolução posterior e não será simulada como se já existisse.

## Direção visual

### Identidade

- **Forest Ink (`#163300`)**: fundo principal, texto de maior peso e superfícies invertidas.
- **Lime Voltage (`#9fe870`)**: ação principal, seleção ativa e pontos focais.
- **Spruce (`#054d28`)**: profundidade e variação tonal.
- **Linen Mist (`#e2f6d5`)** e **Fog (`#e8ebe6`)**: superfícies claras e cartões.
- **Obsidian (`#0e0f0c`)** e **Charcoal (`#454745`)**: títulos e textos.

A identidade não reproduzirá logotipos ou ativos da Wise. A referência será traduzida para o tema de mérito acadêmico por meio de moedas, órbitas, selos e conexões entre os três públicos.

### Tipografia e formas

- Inter será a família principal, com peso 900 e tracking fechado em títulos de campanha.
- Títulos usarão `clamp()` para variar de aproximadamente 2,75rem em celular até 6,5rem em desktop.
- Botões, filtros e seletores usarão formato pill.
- Cartões comerciais usarão raios maiores, bordas sutis e sombras concentradas.
- Campos manterão contraste AA, rótulos persistentes e altura mínima de 48px.

### Profundidade e movimento

- O hero conterá uma composição de moeda 3D feita em HTML/CSS, com camadas, perspectiva e reflexos.
- Pequenos elementos orbitais reforçarão a ideia de circulação da moeda.
- Cartões de perfil terão inclinação e elevação leves no hover apenas em dispositivos compatíveis.
- Animações respeitarão `prefers-reduced-motion`.
- Nenhuma informação ou ação dependerá exclusivamente de animação.

## Arquitetura de navegação

### Página pública

`index.html` passa a ser a landing page e contém:

1. Cabeçalho fixo com marca, Produto, Como funciona, Benefícios, Parceiros e Entrar.
2. Hero com proposta de valor, CTA de acesso e visualização 3D.
3. Seção demonstrando o ciclo professor → aluno → empresa.
4. Benefícios segmentados pelos três públicos.
5. Métricas e sinais de confiança apresentados como demonstração de produto, sem alegações externas não verificadas.
6. Chamada para instituições e empresas parceiras.
7. Perguntas frequentes em accordion.
8. Rodapé com atalhos do sistema.

Em telas pequenas, a navegação será recolhida em um menu com controle de foco e botão de pelo menos 44x44px.

### Entrada por perfil

O botão “Entrar” abre um painel/modal acessível com três opções:

- **Aluno:** login e acesso ao cadastro.
- **Professor:** login de usuário pré-cadastrado.
- **Empresa parceira:** login e acesso ao cadastro.

O perfil escolhido permanece visível no formulário para reduzir erros. A troca de perfil limpa mensagens de erro e mantém somente o e-mail quando apropriado.

### Destinos após login

- **Aluno:** área do aluno com atalhos para vantagens, extrato, transferência e critérios.
- **Professor:** painel com distribuição de moedas, extrato, critérios e avaliação de transferências.
- **Empresa:** área da empresa com perfil e gestão de vantagens.

Os destinos utilizarão o `id` devolvido pelo backend nos links das funcionalidades atuais. O roteamento ficará centralizado em uma função testável, em vez de espalhar regras pelos listeners da página.

## Componentes do frontend

### Novos módulos

- `frontend/js/landing.js`: navegação, menu móvel, modal, seleção de perfil, accordions e efeitos de movimento progressivos.
- `frontend/js/auth-api.js`: chamada de login, normalização de erros e armazenamento da sessão.
- `frontend/js/core/session.js`: leitura, escrita e remoção da sessão corrente em `sessionStorage`.
- `frontend/js/core/routes.js`: geração dos destinos por perfil e identificador.

### Sessão no navegador

A chave de sessão será única para todos os perfis. O valor armazenado conterá somente:

```json
{
  "id": 4,
  "nome": "Aluno Teste",
  "email": "aluno.teste@sga.pucminas.br",
  "perfil": "ALUNO"
}
```

Senha, CPF, CNPJ e demais dados sensíveis não serão armazenados na sessão.

### Compatibilidade das telas existentes

As páginas atuais continuarão aceitando seus parâmetros de consulta durante esta entrega. O novo roteador monta esses parâmetros a partir do usuário autenticado. Serviços antigos de aluno e empresa deixam de semear ou consultar bancos locais; cadastro, leitura e atualização usam somente a API.

## Backend e persistência

### Endpoint de autenticação

Será adicionado:

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "aluno.teste@sga.pucminas.br",
  "senha": "senha123",
  "perfil": "ALUNO"
}
```

Resposta de sucesso:

```json
{
  "id": 4,
  "nome": "Aluno Teste",
  "email": "aluno.teste@sga.pucminas.br",
  "perfil": "ALUNO"
}
```

Respostas de erro:

- `400`: corpo inválido ou perfil desconhecido.
- `401`: e-mail, senha ou combinação de perfil incorreta.
- `503`: falha de persistência indisponível para o serviço.

A mensagem pública de `401` será genérica para não revelar se o e-mail existe.

### Camadas

- `AuthController`: contrato HTTP e códigos de resposta.
- `AuthService`: normalização de e-mail, seleção do repositório por perfil, comparação de senha e montagem da resposta segura.
- `LoginRequest` e `AuthResponse`: DTOs sem exposição da entidade.
- Repositórios existentes recebem as consultas mínimas necessárias. O repositório de professor ganhará busca por e-mail.

### Senhas

Novos cadastros e trocas de senha devem usar hash BCrypt. Para compatibilidade com registros já existentes em texto simples, o primeiro login válido faz migração transparente do valor legado para BCrypt e persiste a atualização. A resposta da API nunca inclui o campo de senha.

Será usada apenas a biblioteca de criptografia necessária, sem ativar filtros globais do Spring Security nesta entrega.

### Fonte de verdade

- MySQL/JPA é a fonte oficial em execução normal.
- H2 permanece isolado aos testes automatizados.
- `localStorage` não contém cadastros nem credenciais.
- Falha do backend é apresentada como indisponibilidade; não cria registros offline divergentes.

## Fluxos principais

### Login

1. Usuário abre a landing e escolhe “Entrar”.
2. Seleciona o perfil.
3. Informa e-mail e senha.
4. Frontend envia o corpo ao endpoint de autenticação.
5. Backend valida no repositório correspondente.
6. Frontend salva a resposta segura em `sessionStorage`.
7. Roteador direciona para o painel do perfil.

### Cadastro

1. Aluno ou empresa escolhe “Criar conta” a partir do respectivo perfil.
2. O formulário atual é apresentado com a nova identidade visual.
3. O cadastro é enviado ao endpoint já existente.
4. Backend valida unicidade de e-mail e CPF/CNPJ, codifica a senha e persiste.
5. A interface confirma o cadastro e permite autenticação imediata.

Professor não recebe opção de autocadastro.

## Estados e tratamento de erros

- Botões exibem estado de carregamento e impedem envios duplicados.
- Erros de campo permanecem próximos ao campo e são anunciados por `aria-live`.
- Credenciais inválidas exibem mensagem genérica.
- Backend indisponível oferece tentativa novamente, sem fallback local.
- Modal devolve foco ao botão que o abriu quando fechado.
- Erros inesperados não apagam o conteúdo digitado.

## Responsividade e acessibilidade

- Construção mobile-first com breakpoints existentes ou 640px, 768px e 1024px quando não houver padrão prévio.
- Verificação visual em 375px, 414px, 768px, 1024px e 1440px.
- Conteúdo sem rolagem horizontal.
- Alvos interativos de pelo menos 44x44px.
- Navegação completa por teclado, foco visível e modal com semântica de diálogo.
- Contraste mínimo WCAG AA para texto e controles.
- Hierarquia de títulos válida e links com nomes descritivos.

## Estratégia de testes

### Backend

- Login de aluno, professor e empresa com credenciais válidas.
- Recusa de senha incorreta e de perfil incorreto.
- E-mail tratado sem diferença de maiúsculas e com espaços externos removidos.
- Resposta não expõe senha nem dados desnecessários.
- Senha legada válida é convertida para BCrypt.
- Novos cadastros persistem senha com hash.

### Frontend

- `auth-api` envia o contrato correto e traduz erros HTTP.
- Sessão guarda apenas os campos permitidos.
- Roteador gera o destino correto para cada perfil.
- Perfil inválido não produz redirecionamento inseguro.
- Interações essenciais continuam funcionando sem animação.

### Integração e visual

- Executar a suíte Maven completa.
- Executar testes JavaScript com o mecanismo já adotado pelo projeto.
- Sincronizar `frontend/` com os recursos estáticos e executar o teste de sincronização.
- Abrir a aplicação real e validar landing, modal, login e destinos nos breakpoints definidos.
- Conferir console do navegador e logs do backend sem erros inesperados.

## Arquivos previstos

### Frontend

- Reformular `frontend/index.html`.
- Atualizar `frontend/css/styles.css` ou separar estilos da landing quando isso reduzir acoplamento.
- Criar os módulos `landing.js`, `auth-api.js`, `session.js` e `routes.js`.
- Adaptar `api.js`, `empresa-api.js`, `app.js` e `empresa.js` para remover persistência local.
- Criar ou adaptar um painel de entrada por perfil usando as páginas existentes.

### Backend

- Criar DTOs, serviço e controller de autenticação.
- Atualizar os três repositórios com consultas necessárias.
- Codificar senhas nos fluxos de criação e atualização.
- Adicionar a dependência de BCrypt sem ativar autenticação automática global.
- Atualizar os dados de demonstração somente quando necessário para manter logins previsíveis.

### Testes e documentação

- Criar testes unitários e de controller para autenticação.
- Criar testes dos módulos puros do frontend.
- Atualizar o README com os acessos de demonstração e o novo ponto de entrada.

## Critérios de aceite

1. A raiz da aplicação apresenta uma landing comercial responsiva, não um formulário isolado.
2. A navegação oferece acesso claro para aluno, professor e empresa.
3. Os três perfis autenticam usando dados persistidos no banco.
4. Professor não pode se autocadastrar pela interface.
5. O navegador não usa `localStorage` como banco de usuários.
6. Senhas não aparecem em respostas nem são mantidas na sessão do navegador.
7. Cada perfil chega às funcionalidades correspondentes com o identificador correto.
8. A aplicação informa indisponibilidade do servidor sem criar dados locais alternativos.
9. A experiência funciona por teclado, respeita redução de movimento e não apresenta overflow nos breakpoints verificados.
10. Testes backend, frontend e sincronização terminam sem falhas.

