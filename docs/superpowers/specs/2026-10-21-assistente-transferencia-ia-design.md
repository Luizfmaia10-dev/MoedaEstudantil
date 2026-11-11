# Assistente de transferência com IA local

**Data:** 2026-10-21
**Status:** aprovado para preparação da implementação

## Contexto

O sistema já permite que um aluno solicite a transferência de moedas para outro aluno. A solicitação informa destinatário, quantidade, professor avaliador e justificativa, é persistida como pendente e somente movimenta saldo após a aprovação do professor.

A nova experiência adicionará um assistente conversacional inspirado no fluxo de Pix por mensagem: o aluno descreve em português o que deseja fazer, revisa os dados interpretados e confirma explicitamente a solicitação. A IA não movimentará moedas diretamente e não substituirá as regras atuais do backend.

## Objetivos

- Permitir comandos como “envie vinte moedas para Ana”.
- Identificar destinatário, quantidade, professor avaliador e justificativa quando informados.
- Perguntar apenas pelos dados obrigatórios que estiverem faltando.
- Resolver destinatários e professores somente entre os registros permitidos ao aluno.
- Exibir uma confirmação clara antes de criar a solicitação.
- Utilizar um modelo pequeno, gratuito e executado localmente.
- Preservar o formulário manual como alternativa permanente.

## Fora de escopo

- Executar transferências sem confirmação do aluno.
- Aprovar automaticamente solicitações que dependem do professor.
- Armazenar o histórico completo da conversa.
- Treinar ou ajustar um modelo próprio.
- Enviar dados para serviços externos de IA.
- Alterar as regras de saldo, instituição, aprovação ou concorrência já existentes.

## Solução escolhida

Ollama executará localmente o modelo `qwen2.5:0.5b-instruct`. O backend Spring será o único cliente do Ollama e solicitará uma resposta estruturada em JSON, com temperatura baixa. O navegador não acessará o modelo diretamente.

O modelo interpretará apenas linguagem natural. Identificadores, permissões, correspondência de nomes e regras de negócio continuarão sob responsabilidade determinística do backend.

Se o Ollama estiver parado ou o modelo não estiver disponível, o assistente mostrará uma mensagem curta e oferecerá o formulário manual sem comprometer o restante do sistema.

## Experiência da interface

### Entrada do assistente

A página de solicitação de transferência ganhará um botão principal de IA visualmente destacado:

- formato arredondado com gradiente lima e verde profundo;
- ícone de brilho/órbita e identificação “Transferir com IA”;
- halo animado discreto e partículas leves no estado ocioso;
- resposta magnética sutil ao ponteiro em dispositivos compatíveis;
- hover com elevação, brilho e expansão controlada;
- estados visíveis de foco, carregamento, sucesso e indisponibilidade;
- área mínima de toque de 44x44px;
- animações desativadas ou reduzidas com `prefers-reduced-motion`.

O botão não dependerá apenas de cor ou movimento para comunicar sua função.

### Painel conversacional

Ao acionar o botão, um painel acessível será aberto com animação GSAP de entrada. Em telas pequenas ele ocupará a largura disponível; em desktop ficará ao lado ou sobre a área do formulário sem ocultar informações essenciais.

O painel conterá:

- saudação curta com um exemplo de comando;
- mensagens do aluno e do assistente em balões distintos;
- campo de texto com envio por botão e tecla Enter;
- sugestões clicáveis para acelerar o primeiro uso;
- indicador de interpretação durante a consulta local;
- cartão de resumo com destinatário, quantidade, professor e justificativa;
- botões “Confirmar solicitação”, “Editar” e “Usar formulário manual”.

As animações de abertura e fechamento preservarão foco, teclado e leitores de tela.

## Fluxo funcional

1. O aluno abre a página e seleciona “Transferir com IA”.
2. Escreve a solicitação em linguagem natural.
3. O frontend envia a mensagem para o endpoint de interpretação.
4. O backend consulta o Ollama e recebe campos estruturados.
5. O backend valida quantidade e procura destinatário e professor dentro do contexto permitido.
6. Se um dado estiver ausente ou ambíguo, o assistente solicita esclarecimento e apresenta opções válidas.
7. Quando os dados estiverem completos, a interface mostra o cartão de confirmação.
8. Somente o botão “Confirmar solicitação” chama o endpoint existente de criação da transferência.
9. A solicitação é persistida como pendente e segue para avaliação do professor.

## Arquitetura

### View

`solicitar-transferencia.html` e seus módulos de JavaScript serão responsáveis pelo painel, mensagens, acessibilidade, animações e confirmação. O formulário atual permanecerá disponível e utilizará o mesmo serviço de transferência.

### Controller

Um endpoint específico receberá a mensagem do aluno:

```http
POST /api/alunos/{alunoId}/transferencias/assistente/interpretar
Content-Type: application/json

{
  "mensagem": "Envie vinte moedas para Ana pelo professor Carlos"
}
```

Esse endpoint apenas interpreta e valida a intenção. Ele nunca cria ou aprova uma transferência.

### Service

Um serviço de aplicação coordenará:

- chamada local ao Ollama;
- validação do JSON estruturado;
- conversão de números escritos por extenso;
- resolução determinística de aluno e professor;
- indicação de campos ausentes ou ambiguidades;
- montagem segura do resumo para confirmação.

O serviço atual de transferência continuará responsável pela criação persistida, reserva lógica de saldo, validação institucional e execução após aprovação.

### Model e persistência

Nenhuma nova entidade será criada para a conversa. Depois da confirmação, a solicitação continuará sendo persistida na entidade existente de solicitação de transferência, mantendo seu estado pendente, relacionamentos, quantidade e justificativa.

O conteúdo temporário do chat ficará somente na memória da página. Recarregar a página encerra a conversa, mas não remove solicitações já confirmadas e persistidas.

## Contrato de interpretação

O backend solicitará ao modelo uma estrutura equivalente a:

```json
{
  "destinatario": "Ana",
  "quantidade": 20,
  "professor": "Carlos",
  "justificativa": null
}
```

A resposta pública do backend incluirá o estado da conversa, campos encontrados, opções válidas quando houver ambiguidade e um resumo quando estiver pronta para confirmação. IDs nunca serão aceitos diretamente da saída do modelo sem resolução e validação no banco.

## Segurança e regras

- O prompt será tratado como dado, não como instrução administrativa.
- O modelo não terá acesso direto ao banco nem às credenciais.
- O backend não confiará em IDs, saldos ou permissões sugeridos pela IA.
- Destinatários serão limitados aos colegas permitidos pela regra atual.
- Quantidade deverá ser inteira e positiva.
- O aluno não poderá escolher a si próprio.
- A confirmação utilizará o endpoint e as validações existentes.
- Erros internos do modelo não serão expostos integralmente ao navegador.
- Ações destrutivas ou movimentações nunca serão disparadas pela interpretação.

## Configuração local

As propriedades da integração definirão URL, modelo e timeout, com valores locais padrão:

```properties
app.ai.ollama.url=http://localhost:11434
app.ai.ollama.model=qwen2.5:0.5b-instruct
```

Nenhuma chave de API será necessária. O README explicará como instalar o Ollama, baixar o modelo e verificar sua disponibilidade.

## Estados de erro

- Mensagem vazia: validação imediata no frontend.
- Pedido incompleto: pergunta objetiva sobre o campo ausente.
- Nome ambíguo: lista de opções para escolha explícita.
- Destinatário não permitido: mensagem sem revelar usuários externos à instituição.
- Quantidade inválida: solicitação de novo valor.
- Ollama indisponível: botão de tentar novamente e acesso ao formulário manual.
- Falha ao confirmar: mantém o resumo e permite nova tentativa sem duplicidade de clique.

## Responsividade e acessibilidade

- Implementação mobile-first.
- Validação visual em 375px, 768px, 1024px e 1440px.
- Sem rolagem horizontal.
- Foco visível e devolvido ao botão ao fechar o painel.
- Mensagens novas anunciadas por região `aria-live`.
- Controles acessíveis por teclado e com nomes descritivos.
- Contraste WCAG AA e alternativas para movimento reduzido.

## Estratégia de testes

### Backend

- Interpretação de número escrito por extenso.
- Resolução exata, ausente e ambígua de destinatário.
- Solicitação de professor quando o campo estiver ausente.
- Recusa de quantidade inválida e destinatário não permitido.
- Conversão segura da resposta estruturada do Ollama.
- Tratamento de timeout, JSON inválido e serviço indisponível.
- Garantia de que interpretar não persiste nem movimenta moedas.
- Confirmação continua utilizando o serviço existente.

### Frontend

- Abertura e fechamento acessíveis do painel.
- Envio da mensagem e renderização de estados.
- Seleção de opções em caso de ambiguidade.
- Cartão de confirmação somente com dados completos.
- Bloqueio de cliques duplicados durante confirmação.
- Fallback para formulário manual.
- Respeito a movimento reduzido.

### Integração

- Execução das suítes Java e JavaScript.
- Teste real com Ollama e `qwen2.5:0.5b-instruct`.
- Validação visual nos breakpoints definidos.
- Verificação de que a solicitação confirmada aparece para avaliação do professor.

## Critérios de aceite

1. O botão “Transferir com IA” é chamativo, interativo, acessível e coerente com a identidade visual do sistema.
2. O aluno consegue escrever quantidade e destinatário por extenso em português.
3. O assistente solicita dados obrigatórios ausentes sem inventá-los.
4. Nenhuma solicitação é persistida antes da confirmação explícita.
5. A confirmação reutiliza todas as regras e a persistência existentes.
6. O formulário manual continua plenamente utilizável.
7. A ausência do Ollama não interrompe outras funções da página.
8. Nenhum dado do sistema é enviado a um serviço externo de IA.
9. O fluxo funciona por teclado, em dispositivos móveis e com redução de movimento.
10. Testes automatizados e validação de integração terminam sem falhas.
