# Emails automáticos

## Requisitos implementados

O LABDESENV_4, seção 4.1 (Lab04S01), pede confirmação de envio e recebimento com um template para professor e outro para aluno. A descrição do sistema também pede cupom ao aluno e aviso ao parceiro no resgate, com o mesmo código nos dois emails.

| Evento confirmado | Destinatário | Conteúdo |
| --- | --- | --- |
| Distribuição de moedas | Aluno | Professor, quantidade e motivo do reconhecimento. |
| Distribuição de moedas | Professor | Aluno, quantidade e motivo do envio. |
| Resgate de vantagem | Aluno | Vantagem, empresa, custo e código do cupom para troca presencial. |
| Resgate de vantagem | Empresa | Aluno, vantagem, custo e o mesmo código para conferência. |

## Configuração Resend

Na pasta `demo (2)/demo`, copie `.env.example` para `.env` e preencha o banco e as credenciais de email:

```properties
MAIL_PROVIDER=resend
RESEND_API_KEY=sua_chave
MAIL_FROM=onboarding@resend.dev
MAIL_DELIVERY_ENABLED=true
```

O `.env` é ignorado pelo Git. A chave fica apenas no backend e não aparece no JavaScript, nos commits ou nos logs do processador. Não copie a chave para `.env.example`.

Para testes, `onboarding@resend.dev` é o remetente disponibilizado pelo Resend. Nesse modo, o envio a pessoas fica restrito ao endereço da conta; use os destinatários de simulação oficiais, como `delivered@resend.dev`, para testes técnicos. Para enviar aos alunos, professores e empresas cadastrados, verifique seu domínio no Resend e configure `MAIL_FROM` com um endereço desse domínio. Os destinatários são os emails cadastrados no sistema; o aplicativo não os redireciona automaticamente para uma caixa de testes.

Referências: [envio pela API](https://resend.com/docs/api-reference/emails/send-email), [restrição do domínio de testes](https://resend.com/docs/knowledge-base/403-error-resend-dev-domain), [endereços de simulação](https://resend.com/docs/dashboard/emails/send-test-emails) e [idempotência](https://resend.com/docs/dashboard/emails/idempotency-keys).

Para iniciar com Java 21, execute na pasta do backend:

```powershell
.\mvnw.cmd spring-boot:run
```

Não é necessário clicar em um botão adicional de envio: distribuir moedas ou resgatar uma vantagem já agenda os emails. Reinicie o backend depois de alterar o `.env`.

## Fila e tratamento de falhas

`NotificacaoTemplates` monta as mensagens e `NotificacaoService` grava duas linhas em `email_pendente` dentro da mesma transação da distribuição ou do resgate. Se houver rollback, as mensagens também são descartadas. Não há chamada ao provedor enquanto o saldo é atualizado.

`EmailOutboxScheduler` inicia após cinco segundos e chama `EmailOutboxProcessor`, que busca uma mensagem elegível por rodada com bloqueio pessimista. O intervalo padrão é de um segundo após terminar a rodada; a entrega usa uma transação separada. A fila permanece no MySQL após reiniciar a aplicação.

As falhas têm até cinco tentativas no total, com intervalos de 30, 60, 120 e 240 segundos. Depois disso, a mensagem fica em `FALHOU` para análise. Uma falha de entrega não desfaz o saldo já confirmado nem impede o envio da outra mensagem. Falhas ao gravar a própria fila revertem a operação de negócio para evitar confirmar uma movimentação sem agendar sua notificação.

O estado `ENVIADO` significa que o provedor aceitou a mensagem; a entrega final à caixa postal deve ser conferida no painel Resend. Cada mensagem usa seu UUID persistido como `Idempotency-Key` em todas as tentativas. O Resend mantém essa proteção por 24 horas. SMTP não oferece essa garantia: uma interrupção entre o aceite do servidor e o commit pode gerar duplicidade numa nova tentativa. Reinícios prolongados também exigem conferência do painel antes de reprocessar mensagens antigas.

O timeout padrão do Resend é de dez segundos. SMTP tem limites de dez segundos para conexão, leitura e escrita. Para interromper o processamento e preservar as pendências, configure `MAIL_DELIVERY_ENABLED=false` e reinicie a aplicação. Para ajustar a cadência, use `MAIL_POLL_INTERVAL_MS`; o padrão considera uma única instância do backend.

Consulte o estado no banco:

```sql
SELECT id, destinatario, assunto, status, tentativas,
       proxima_tentativa, enviado_em, ultimo_erro
FROM email_pendente
ORDER BY criado_em DESC;
```

Os logs mostram o UUID, a tentativa e o estado, sem conteúdo, destinatário ou chave. O campo `ultimo_erro` guarda a categoria da falha. Para HTTP 401/403, confira chave, remetente e restrições de destinatário; para 429, confira os limites da conta. Depois de corrigir a configuração, pendências ainda dentro das cinco tentativas serão retomadas automaticamente. Mensagens em `FALHOU` exigem análise antes de uma recuperação manual para evitar duplicidade.

## Alternativa SMTP local

Com Mailpit/MailHog disponível na porta 1025:

```properties
MAIL_PROVIDER=smtp
MAIL_FROM=nao-responda@moeda-estudantil.local
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_AUTH=false
MAIL_STARTTLS=false
```

## Verificação

```powershell
.\mvnw.cmd test
```

Os testes usam SMTP simulado, servidor HTTP local e H2; o processamento agendado fica desabilitado. Cobrem os quatro destinatários/templates, código compartilhado do cupom, autenticação HTTP, JSON, timeout, falhas do provedor, retentativas, idempotência, seleção de mensagens elegíveis e commit/rollback da fila. Não enviam emails reais.
