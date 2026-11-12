# Diagramas de Sequência — Sistema de Moeda Estudantil

Este documento reúne os **13 diagramas de sequência** dos casos de uso (incluindo os relacionamentos `<<include>>`) do Sistema de Moeda Estudantil, seguindo a arquitetura MVC em camadas do projeto (`View/Frontend` → `Controller` → `Service` → `Repository` → `Banco de Dados`). Emails passam por uma fila persistente e são entregues por Resend ou SMTP após o commit.

---

## 1. Solicitar transferência de moeda para outro aluno (Ator: Aluno)

```mermaid
sequenceDiagram
    autonumber
    actor A as Aluno (Remetente)
    participant V as TelaTransferencia (solicitar-transferencia.html)
    participant C as SolicitacaoTransferenciaController
    participant S as SolicitacaoTransferenciaService
    participant CR as ContaRepository
    participant SR as SolicitacaoTransferenciaRepository

    A->>V: Acessa a tela de transferência
    par Carregamento inicial da tela
        V->>C: GET /api/alunos/{alunoId}/saldo-disponivel
        C->>S: saldoDisponivel(alunoId)
        S->>CR: findByUsuarioId(alunoId)
        S->>SR: somarQuantidadePorRemetenteEStatus(alunoId, PENDENTE)
        S-->>V: SaldoDisponivelDTO (corrente, pendentes, disponivel)
    and Colegas da mesma instituição
        V->>C: GET /api/alunos/{alunoId}/colegas
        C->>S: listarColegas(alunoId)
        S-->>V: Lista de ColegaDTO (mesma instituição)
    and Professores e critérios da mesma instituição
        V->>C: GET /api/alunos/{alunoId}/criterios-professores
        C-->>V: Lista de CriterioTransferenciaDTO
    end

    A->>V: Seleciona professor avaliador
    V-->>A: Exibe os critérios cadastrados pelo professor selecionado
    A->>V: Seleciona colega destinatário, quantidade e justificativa opcional
    V->>C: POST /api/alunos/{alunoId}/transferencias
    C->>S: criar(remetenteId, destinatarioId, avaliadorId, quantidade, justificativa)

    S->>S: Valida quantidade > 0, destinatário != remetente e mesma instituição
    S->>CR: findByUsuarioIdParaAtualizacao(remetenteId) [PESSIMISTIC_WRITE]
    CR-->>S: Conta bloqueada (SELECT ... FOR UPDATE)
    S->>SR: somarQuantidadePorRemetenteEStatus(remetenteId, PENDENTE)
    SR-->>S: Total pendente

    alt Quantidade <= Saldo Disponível (saldoCorrente - pendentes)
        S->>SR: save(SolicitacaoTransferencia [status = PENDENTE])
        SR-->>S: Solicitação persistida
        S-->>C: SolicitacaoTransferenciaDTO
        C-->>V: 201 Created (SolicitacaoTransferenciaDTO)
        V-->>A: Exibe confirmação e atualiza saldo disponível
    else Quantidade > Saldo Disponível ou dados inválidos
        S-->>C: ResponseStatusException (400 Bad Request)
        C-->>V: 400 Bad Request (mensagem de erro)
        V-->>A: Exibe alerta de erro
    end
```

---

## 2. Resgatar vantagem (Ator: Aluno)

```mermaid
sequenceDiagram
    autonumber
    actor A as Aluno
    participant V as TelaResgateVantagens
    participant C as ResgateController
    participant S as ResgateService
    participant VR as VantagemRepository
    participant CR as ContaRepository
    participant TR as TransacaoRepository
    participant EM as NotificacaoService

    A->>V: Consulta catálogo de vantagens ativas
    V->>C: GET /api/vantagens
    C->>S: listarVantagensAtivas()
    S->>VR: findAllAtivas()
    VR-->>S: Lista de Vantagens
    S-->>C: Lista de VantagemDTO
    C-->>V: 200 OK (Vantagens)
    V-->>A: Exibe cards com foto, descrição, empresa e custo

    A->>V: Clica em "Resgatar" na vantagem escolhida
    V->>C: POST /api/alunos/{alunoId}/resgates (vantagemId)
    C->>S: resgatar(alunoId, vantagemId)
    S->>VR: findById(vantagemId) [verifica se está ativa]
    S->>CR: findByUsuarioIdParaAtualizacao(alunoId) [PESSIMISTIC_WRITE]
    CR-->>S: Conta do aluno

    alt Saldo Corrente >= Custo da Vantagem
        S->>S: Debita custoMoedas do saldoCorrente e gera codigoCupom único
        S->>CR: save(contaAluno)
        S->>TR: save(ResgateVantagem [tipo = SAIDA, codigoCupom])
        Note over S,EM: <<include>> Enviar e-mail às partes
        S->>EM: notificarResgate(aluno, vantagem, codigoCupom)
        EM-->>S: Dois emails agendados na mesma transação
        S-->>C: ResgateDTO (codigoCupom, vantagem, saldoRestante)
        C-->>V: 201 Created (ResgateDTO)
        V-->>A: Exibe cupom gerado e novo saldo
    else Saldo Insuficiente
        S-->>C: 400 Bad Request ("Saldo insuficiente para resgatar esta vantagem")
        C-->>V: 400 Bad Request
        V-->>A: Exibe mensagem de saldo insuficiente
    end
```

---

## 3. Enviar e-mail às partes (`<<include>>` de Resgatar Vantagem)

```mermaid
sequenceDiagram
    autonumber
    participant RS as ResgateVantagemService
    participant ES as NotificacaoService
    participant DB as EmailPendenteRepository / MySQL
    participant W as EmailOutboxScheduler / Processor
    participant API as EmailSender (Resend ou SMTP)
    actor A as Caixa de E-mail (Aluno)
    actor E as Caixa de E-mail (Empresa Parceira)

    Note over RS,ES: Incluído ao confirmar o resgate de uma vantagem (RF-009 e RF-010)
    RS->>ES: notificarResgate(aluno, vantagem, codigoCupom)
    ES->>ES: Monta templates com o mesmo código do cupom
    ES->>DB: saveAll(emailAluno, emailEmpresa)
    ES-->>RS: Agendamento concluído
    Note over RS,DB: Commit confirma resgate e emails juntos; rollback descarta ambos
    loop Uma mensagem elegível por rodada após commit
        W->>DB: buscarProntos(PENDENTE, agora, limite 1) com bloqueio
        W->>API: enviar(mensagem, chaveIdempotencia)
        alt Provedor aceitou
            W->>DB: ENVIADO e data do aceite
            API-->>A: Cupom do aluno (quando destinatário)
            API-->>E: Código de conferência (quando destinatário)
        else Falha
            W->>DB: Reagendar com atraso ou FALHOU após 5 tentativas
        end
    end
```

---

## 4. Realizar cadastro (Ator: Aluno)

```mermaid
sequenceDiagram
    autonumber
    actor A as Aluno
    participant V as TelaCadastroAluno (index.html)
    participant C as AlunoController
    participant IR as InstituicaoRepository
    participant AR as AlunoRepository

    A->>V: Acessa a aba "Cadastrar-se"
    Note over A,IR: <<include>> Selecionar instituição
    V->>C: GET /api/instituicoes
    C->>IR: findAll()
    IR-->>C: Lista de Instituições
    C-->>V: 200 OK (Instituições)
    V-->>A: Preenche seletor de Instituições de Ensino

    A->>V: Seleciona a instituição e preenche nome, CPF, RG, e-mail, senha, curso e endereço
    V->>C: POST /api/alunos (AlunoDTO)
    C->>IR: findById(dto.instituicaoId)

    alt Instituição encontrada e dados válidos
        C->>C: Instancia Aluno e cria Conta inicial (saldoCorrente = 0)
        C->>AR: save(aluno [cascade salva Conta])
        AR-->>C: Aluno persistido
        C-->>V: 201 Created (AlunoDTO)
        V-->>A: Inicia sessão e redireciona para o perfil do aluno
    else Instituição inexistente ou e-mail/CPF duplicado
        C-->>V: 400 Bad Request
        V-->>A: Exibe mensagem de erro no formulário
    end
```

---

## 5. Selecionar instituição (`<<include>>` de Realizar Cadastro)

```mermaid
sequenceDiagram
    autonumber
    actor A as Aluno
    participant V as TelaCadastroAluno (index.html)
    participant C as AlunoController
    participant IR as InstituicaoRepository

    Note over A,IR: Executado durante o caso de uso "Realizar cadastro" (RF-002)
    V->>C: GET /api/instituicoes
    C->>IR: findAll()
    IR-->>C: Lista de Instituicao (pré-cadastradas no sistema)
    C-->>V: 200 OK (JSON com id e nome das instituições)
    V->>V: Renderiza opções no campo <select id="instituicao">
    A->>V: Escolhe sua instituição de ensino na lista
    V->>V: Vincula instituicaoId aos dados do formulário de cadastro
```

---

## 6. Consultar extrato (Atores: Professor e Aluno)

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuário (Aluno ou Professor)
    participant V as TelaExtrato (extrato.html)
    participant C as ExtratoController
    participant S as ExtratoService
    participant CR as ContaRepository
    participant TR as TransacaoRepository

    U->>V: Abre a tela de extrato (opcionalmente escolhe filtro por categoria)
    V->>C: GET /api/contas/{usuarioId}/extrato?categoria={filtro}
    C->>S: gerarExtrato(usuarioId, categoria)
    S->>CR: findByUsuarioId(usuarioId)

    alt Conta encontrada
        CR-->>S: Conta (saldoCorrente, Usuario titular)
        S->>TR: findByContaIdOrderByDataHoraDescIdDesc(contaId)
        TR-->>S: Lista de TransacaoGeral (Envio, ResgateVantagem, TransferenciaEntreAlunos)
        loop Para cada transação (filtrada por categoria, se informada)
            S->>S: Formata descrição sob a ótica do titular (Professor ou Aluno)
        end
        S-->>C: Optional<ExtratoDTO> (titularNome, titularTipo, saldoAtual, transacoes)
        C-->>V: 200 OK (ExtratoDTO)
        V-->>U: Exibe saldo atual e histórico ordenado por data/hora
    else Usuário sem conta / inexistente
        CR-->>S: Optional.empty()
        S-->>C: Optional.empty()
        C-->>V: 404 Not Found
        V-->>U: Exibe mensagem "Conta não encontrada"
    end
```

---

## 7. Distribuir moedas (Ator: Professor)

```mermaid
sequenceDiagram
    autonumber
    actor P as Professor
    participant V as TelaDistribuicaoMoedas
    participant C as EnvioController
    participant S as EnvioService
    participant CR as ContaRepository
    participant TR as TransacaoRepository
    participant EM as NotificacaoService

    P->>V: Seleciona aluno da instituição e informa quantidade de moedas
    Note over P,S: <<include>> Informar motivo
    P->>V: Preenche o motivo obrigatório da distribuição
    V->>C: POST /api/professores/{professorId}/envios (alunoId, quantidade, motivo)
    C->>S: distribuirMoedas(professorId, alunoId, quantidade, motivo)

    S->>S: Valida quantidade > 0 e motivo não vazio
    S->>CR: findByUsuarioIdParaAtualizacao(professorId) [PESSIMISTIC_WRITE]
    S->>CR: findByUsuarioIdParaAtualizacao(alunoId) [PESSIMISTIC_WRITE]
    CR-->>S: Contas do professor e do aluno

    alt Saldo do Professor >= Quantidade e Motivo preenchido
        S->>S: Debita saldo da conta do Professor e credita na conta do Aluno
        S->>CR: save(contaProfessor) e save(contaAluno)
        S->>TR: save(Envio [SAIDA na conta do Professor, motivo])
        S->>TR: save(Envio [ENTRADA na conta do Aluno, motivo])
        Note over S,EM: <<include>> Confirmar envio ao professor e recebimento ao aluno
        S->>EM: notificarRecebimento(aluno, professor, quantidade, motivo)
        EM-->>S: Dois emails agendados na mesma transação
        S-->>C: EnvioDTO
        C-->>V: 201 Created (EnvioDTO)
        V-->>P: Exibe confirmação e atualiza saldo do professor
    else Saldo insuficiente ou motivo em branco
        S-->>C: 400 Bad Request
        C-->>V: 400 Bad Request (mensagem de validação)
        V-->>P: Exibe erro na tela
    end
```

---

## 8. Informar motivo (`<<include>>` de Distribuir moedas)

```mermaid
sequenceDiagram
    autonumber
    actor P as Professor
    participant V as TelaDistribuicaoMoedas
    participant C as EnvioController
    participant S as EnvioService

    Note over P,S: Passo obrigatório incluído em "Distribuir moedas" (RF-004)
    P->>V: Digita o motivo do envio (bom comportamento / participação em aula)
    V->>V: Valida preenchimento no cliente (campo obrigatório)

    alt Motivo vazio no cliente
        V-->>P: Destaca campo obrigatório e impede submissão
    else Motivo preenchido no cliente
        V->>C: Envia requisição contendo o campo "motivo"
        C->>S: validarMotivo(motivo)
        alt Motivo nulo ou apenas espaços no servidor
            S-->>C: 400 Bad Request ("O motivo do envio é obrigatório.")
            C-->>V: 400 Bad Request
            V-->>P: Exibe mensagem de erro
        else Motivo válido
            S-->>S: Prossegue com o débito/crédito das moedas
        end
    end
```

---

## 9. Confirmar envio e recebimento por email (`<<include>>` de Distribuir moedas)

```mermaid
sequenceDiagram
    autonumber
    participant ES as DistribuicaoMoedasService
    participant EM as NotificacaoService
    participant DB as EmailPendenteRepository / MySQL
    participant W as EmailOutboxScheduler / Processor
    participant API as EmailSender (Resend ou SMTP)
    actor A as Caixa de E-mail (Aluno)
    actor P as Caixa de E-mail (Professor)

    Note over ES,EM: RF-005 e Lab04S01: um template por perfil
    ES->>EM: notificarRecebimento(aluno, professor, quantidade, motivo)
    EM->>EM: Monta templates com participantes, quantidade e motivo
    EM->>DB: saveAll(emailAluno, emailProfessor)
    EM-->>ES: Agendamento concluído
    Note over ES,DB: Commit confirma saldos e emails juntos; rollback descarta ambos
    loop Uma mensagem elegível por rodada após commit
        W->>DB: buscarProntos(PENDENTE, agora, limite 1) com bloqueio
        W->>API: enviar(mensagem, chaveIdempotencia)
        alt Provedor aceitou
            API-->>A: Confirmação de recebimento (quando destinatário)
            API-->>P: Confirmação de envio (quando destinatário)
            W->>DB: ENVIADO e data do aceite
        else Falha
            W->>DB: Reagendar com atraso ou FALHOU após 5 tentativas
        end
    end
```

---

## 10. Cadastrar critérios de transferência (Ator: Professor)

```mermaid
sequenceDiagram
    autonumber
    actor P as Professor
    participant V as TelaCriteriosProf (criterios-professor.html)
    participant C as CriterioTransferenciaController
    participant S as CriterioTransferenciaService
    participant PR as ProfessorRepository

    P->>V: Acessa a tela de critérios de transferência
    V->>C: GET /api/professores/{professorId}/criterios
    C->>S: buscar(professorId)
    S->>PR: findById(professorId)
    PR-->>S: Professor
    S-->>C: CriterioTransferenciaDTO
    C-->>V: 200 OK (texto atual dos critérios)
    V-->>P: Exibe textarea com os critérios atuais e contador (0/1000)

    P->>V: Edita o texto dos critérios e clica em "Salvar"
    V->>C: PUT /api/professores/{professorId}/criterios (CriterioRequest)
    C->>S: salvar(professorId, texto)

    alt Texto com até 1000 caracteres
        S->>PR: findById(professorId)
        PR-->>S: Professor
        S->>S: Aplica trim() (ou define null se vazio)
        S->>PR: save(professor)
        PR-->>S: Professor atualizado
        S-->>C: CriterioTransferenciaDTO
        C-->>V: 200 OK (CriterioTransferenciaDTO)
        V-->>P: Exibe confirmação "Critérios salvos com sucesso."
    else Texto excede 1000 caracteres
        S-->>C: 400 Bad Request ("Os critérios devem ter no máximo 1000 caracteres.")
        C-->>V: 400 Bad Request
        V-->>P: Exibe mensagem de erro
    end
```

---

## 11. Avaliar solicitações de transferência (Ator: Professor)

```mermaid
sequenceDiagram
    autonumber
    actor P as Professor (Avaliador)
    participant V as TelaAvaliacao (avaliar-transferencias.html)
    participant C as SolicitacaoTransferenciaController
    participant S as SolicitacaoTransferenciaService
    participant SR as SolicitacaoTransferenciaRepository
    participant CR as ContaRepository
    participant TR as TransacaoRepository

    P->>V: Abre a tela de gestão de transferências (filtro = PENDENTE)
    V->>C: GET /api/professores/{professorId}/transferencias?status=PENDENTE
    C->>S: listarParaProfessor(professorId, "PENDENTE")
    S->>SR: findByAvaliadorIdOrderByDataSolicitacaoDescIdDesc(professorId)
    SR-->>S: Solicitações atribuídas ao professor
    S-->>C: Lista de SolicitacaoTransferenciaDTO
    C-->>V: 200 OK
    V-->>P: Exibe solicitações pendentes (remetente, destinatário, valor, justificativa)

    P->>V: Informa explicação opcional e clica em "Aprovar" ou "Recusar"
    alt Professor escolhe APROVAR
        V->>C: POST /api/professores/{professorId}/transferencias/{id}/aprovar (observacao)
        C->>S: aprovar(professorId, solicitacaoId, observacao)
        S->>SR: findByIdParaAtualizacao(solicitacaoId) [PESSIMISTIC_WRITE]
        S->>S: Verifica se pertence ao professor e se status == PENDENTE
        S->>CR: findByUsuarioIdParaAtualizacao(menorId) e (maiorId) [evita deadlock]
        alt Saldo corrente do remetente >= quantidade
            S->>CR: Debita contaRemetente e credita contaDestinatario
            S->>TR: save(TransferenciaEntreAlunos [SAIDA - remetente])
            S->>TR: save(TransferenciaEntreAlunos [ENTRADA - destinatário])
            S->>SR: save(solicitacao [status = APROVADA, observacaoAvaliador, dataAvaliacao])
            S-->>C: SolicitacaoTransferenciaDTO (APROVADA)
            C-->>V: 200 OK
            V-->>P: Exibe "Transferência aprovada e moedas creditadas!"
        else Saldo corrente do remetente insuficiente
            S-->>C: 400 Bad Request
            C-->>V: 400 Bad Request
            V-->>P: Exibe erro de saldo insuficiente do remetente
        end
    else Professor escolhe RECUSAR
        V->>C: POST /api/professores/{professorId}/transferencias/{id}/recusar (observacao)
        C->>S: recusar(professorId, solicitacaoId, observacao)
        S->>SR: findByIdParaAtualizacao(solicitacaoId) [PESSIMISTIC_WRITE]
        S->>CR: findByUsuarioIdParaAtualizacao(remetenteId)
        S->>SR: save(solicitacao [status = RECUSADA, observacaoAvaliador, dataAvaliacao])
        S-->>C: SolicitacaoTransferenciaDTO (RECUSADA)
        C-->>V: 200 OK
        V-->>P: Exibe "Solicitação recusada." (saldo reservado volta a ficar disponível)
    end
```

---

## 12. Cadastrar no sistema (Ator: Empresa Parceira)

```mermaid
sequenceDiagram
    autonumber
    actor E as Empresa Parceira
    participant V as TelaEmpresa (empresa.html)
    participant C as EmpresaParceiraController
    participant ER as EmpresaParceiraRepository

    E->>V: Acessa o Portal de Empresas Parceiras e preenche Nome, CNPJ, E-mail e Senha
    V->>C: POST /api/empresas (EmpresaParceiraDTO)
    C->>ER: findByCnpj(dto.cnpj) / findByEmail(dto.email)

    alt CNPJ e E-mail ainda não cadastrados
        C->>C: Instancia EmpresaParceira (tipo_usuario = EMPRESA)
        C->>ER: save(empresa)
        ER-->>C: EmpresaParceira persistida
        C-->>V: 201 Created (EmpresaParceiraDTO)
        V-->>E: Salva sessão e exibe área de gerenciamento da empresa
    else CNPJ ou E-mail já existente
        C-->>V: 400 Bad Request ("Empresa já cadastrada com este CNPJ ou e-mail")
        V-->>E: Exibe mensagem de erro no formulário
    end
```

---

## 13. Gerenciar vantagens (Ator: Empresa Parceira)

```mermaid
sequenceDiagram
    autonumber
    actor E as Empresa Parceira
    participant V as TelaVantagens (vantagens.html)
    participant C as VantagemController
    participant S as VantagemService
    participant AS as ArquivoStorageService
    participant VR as VantagemRepository

    E->>V: Abre a tela "Minhas Vantagens"
    V->>C: GET /api/empresas/{empresaId}/vantagens
    C->>S: listar(empresaId)
    S->>VR: findAtivasByEmpresaId(empresaId)
    VR-->>S: Lista de Vantagens ativas
    S-->>C: Lista de VantagemDTO
    C-->>V: 200 OK
    V-->>E: Renderiza cards das vantagens ativas

    alt Cadastrar nova vantagem
        E->>V: Preenche nome, descrição, custo (> 0), seleciona foto e clica em "Salvar"
        V->>C: POST /api/empresas/{empresaId}/vantagens (multipart/form-data)
        C->>S: criar(empresaId, nome, descricao, custoMoedas, foto)
        S->>S: Valida campos obrigatórios e custoMoedas > 0
        S->>AS: salvarImagem(foto) [valida MIME JPG/PNG/WEBP/GIF e gera nome UUID]
        AS-->>S: URL pública (/uploads/{uuid}.ext)
        S->>VR: save(Vantagem [ativa = true, fotoUrl])
        VR-->>S: Vantagem criada
        S-->>C: VantagemDTO
        C-->>V: 201 Created (VantagemDTO)
        V-->>E: Fecha modal e atualiza grade de vantagens
    else Editar vantagem existente
        E->>V: Altera dados (e opcionalmente envia nova foto) e clica em "Salvar"
        V->>C: PUT /api/empresas/{empresaId}/vantagens/{id} (multipart/form-data)
        C->>S: atualizar(empresaId, id, nome, descricao, custoMoedas, novaFoto)
        S->>VR: findById(id) [verifica se pertence à empresa e está ativa]
        opt Nova foto enviada
            S->>AS: salvarImagem(novaFoto)
            S->>AS: remover(fotoUrlAntiga)
        end
        S->>VR: save(vantagem)
        S-->>C: 200 OK (VantagemDTO)
        C-->>V: 200 OK
        V-->>E: Atualiza card da vantagem
    else Remover vantagem (Exclusão Lógica)
        E->>V: Clica em "Remover" e confirma
        V->>C: DELETE /api/empresas/{empresaId}/vantagens/{id}
        C->>S: remover(empresaId, id)
        S->>VR: findById(id) [verifica posse da empresa]
        S->>VR: save(vantagem [ativa = false])
        S-->>C: 204 No Content
        C-->>V: 204 No Content
        V-->>E: Remove card da listagem (preservando histórico de resgates anteriores)
    end
```
