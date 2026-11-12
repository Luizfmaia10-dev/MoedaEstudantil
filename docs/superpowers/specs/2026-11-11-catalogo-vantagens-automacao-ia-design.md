# Catálogo de vantagens e automação de transferências por IA

## Objetivo

Refinar a listagem de vantagens do aluno e permitir que cada professor ative uma automação que aprove ou recuse solicitações de transferência com base em seus próprios critérios. Falhas, respostas ambíguas e decisões com confiança inferior a 80% devem preservar a solicitação pendente para avaliação manual.

## Escopo

### Catálogo do aluno

- Refinar a tela existente `resgatar-vantagens.html`, sem criar um segundo catálogo.
- Preservar o saldo, o fluxo de resgate e a exibição do cupom existentes.
- Melhorar a hierarquia visual dos cartões, a consistência das imagens e os estados de carregamento, vazio e erro.
- Adicionar busca por nome, descrição ou empresa.
- Adicionar filtro para exibir somente vantagens que cabem no saldo atual.
- Indicar claramente vantagens indisponíveis por saldo insuficiente.
- Manter a identidade verde e amarela, os componentes existentes e uma composição responsiva mobile-first.

### Automação do professor

- Integrar a automação ao topo da tela existente de avaliação de transferências.
- Permitir ativação e desativação individual por professor.
- Exigir critérios de transferência cadastrados antes da ativação.
- Ao ativar, analisar solicitações pendentes já existentes.
- Após a ativação, analisar cada nova solicitação destinada ao professor.
- Manter os controles manuais disponíveis para toda solicitação que permanecer pendente.
- Identificar decisões automáticas no histórico e na interface.

## Experiência visual

A área de automação reutilizará a faixa escura da IA, a paleta da aplicação e o componente `star-border` já empregado no assistente de transferência. O botão principal manterá o canvas luminoso, os estados de foco e movimento reduzido. O painel mostrará o estado da automação, um resumo dos critérios e feedback do processamento das pendências.

Solicitações decididas automaticamente receberão o selo “Avaliada pela IA”. Pendências continuarão com os botões manuais atuais. O painel do professor continuará sendo a entrada principal e sua ação de transferências passará a mencionar a automação.

## Modelo de dados

`Professor` receberá um indicador persistido `automacaoTransferenciasAtiva`, inicialmente falso.

`SolicitacaoTransferencia` receberá os campos de auditoria:

- `avaliacaoAutomatica`: identifica uma decisão executada pela automação;
- `confiancaAvaliacaoAutomatica`: confiança validada da decisão.

A explicação continuará em `observacaoAvaliador`, e a data continuará em `dataAvaliacao`. Decisões manuais manterão `avaliacaoAutomatica` falsa e confiança nula.

## Contrato da IA

Um contrato específico de avaliação receberá um contexto imutável com critérios, quantidade, justificativa e dados textuais mínimos dos participantes. O resultado terá decisão `APROVAR`, `RECUSAR` ou `PENDENTE`, confiança entre 0 e 1 e uma explicação curta.

A implementação Ollama reutilizará a URL, o modelo, o timeout e o cliente HTTP já adotados pelo projeto. A resposta será JSON e passará por validação estrita. Não haverá fallback determinístico para decisões automáticas.

Uma decisão só poderá alterar a solicitação quando:

- a resposta for válida;
- a decisão for `APROVAR` ou `RECUSAR`;
- a confiança for igual ou superior a 0,80;
- a automação do professor ainda estiver ativa;
- a solicitação ainda estiver pendente e atribuída ao mesmo professor.

Qualquer outra condição mantém a solicitação pendente.

## Fluxo de processamento

1. A solicitação é validada e salva como pendente pela regra atual.
2. Um evento é publicado dentro da transação.
3. Um ouvinte executado após o commit solicita a análise automática.
4. O contexto é lido sem manter transação ou bloqueio durante a chamada ao Ollama.
5. Se houver decisão confiável, a automação chama o fluxo transacional de aprovação ou recusa.
6. O fluxo transacional volta a bloquear e conferir a solicitação antes de movimentar saldo ou gravar a decisão.

Ao ativar a automação, as pendências são processadas em ordem. Se a IA ficar indisponível, o lote é interrompido e o restante permanece pendente. Conflitos com cancelamento ou avaliação manual são tratados como item já resolvido, sem sobrescrever a decisão existente.

## API

Na rota do professor serão adicionadas operações para:

- consultar estado, critérios disponíveis e resumo da automação;
- ativar ou desativar a automação.

A ativação sem critérios retorna erro de validação. A API de avaliação continuará sendo a única responsável por alterar status e saldos; o navegador não poderá enviar uma decisão como se fosse produzida pela IA.

O DTO de solicitação incluirá os campos de auditoria necessários para o selo e a confiança. O contrato atual permanecerá compatível por adição de campos.

## Tratamento de erros

- Ollama indisponível, timeout, JSON inválido ou baixa confiança: manter pendente.
- Critérios ausentes: impedir ativação com mensagem clara.
- Saldo insuficiente no momento de aprovar: manter a proteção transacional atual e não produzir movimentação parcial.
- Solicitação já resolvida: ignorar a decisão automática atrasada.
- Falha ao processar pendências: informar o professor e preservar os itens não processados.

## Testes

### Backend

- configuração individual da automação e exigência de critérios;
- interpretação válida e inválida da resposta Ollama;
- aprovação e recusa automáticas com auditoria;
- baixa confiança, decisão pendente e indisponibilidade sem alteração;
- processamento das pendências ao ativar;
- disparo após o commit de uma nova solicitação;
- conflito com decisão manual ou cancelamento;
- endpoints e serialização dos novos campos.

### Frontend

- busca e filtro do catálogo;
- renderização de saldo insuficiente;
- estados da automação e comandos de ativação/desativação;
- selo de decisão automática;
- presença e inicialização do `star-border` no botão de IA;
- sincronização entre `frontend/` e os estáticos do Spring.

### Verificação visual

Verificar as duas telas em 375, 768, 1024 e 1440 pixels, incluindo ausência de rolagem horizontal, alvos de toque adequados, foco visível e comportamento com movimento reduzido.

## Estratégia de commits

O desenvolvimento será dividido em commits pequenos e independentes, com mensagens em português: modelo persistente, contrato da IA, integração Ollama, regras da automação, API, interface do professor, catálogo do aluno, sincronização/documentação e correções encontradas pela verificação.

## Fora do escopo

- Escolha de modelo ou limiar de confiança pela interface.
- Regras determinísticas alternativas quando a IA falhar.
- Reversão automática de decisões já executadas.
- Novo provedor externo de IA ou nova dependência.
- Reestruturação das telas ou serviços que não participam desses fluxos.
