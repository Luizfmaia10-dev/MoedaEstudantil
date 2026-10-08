# Sistema de Moeda Estudantil

Projeto desenvolvido para a disciplina de **Laboratório de Desenvolvimento de Software** do curso de Engenharia de Software da PUC Minas.

O sistema tem o objetivo de estimular o reconhecimento do mérito estudantil através de uma moeda virtual, que pode ser distribuída por professores aos alunos e trocada por produtos e descontos em empresas parceiras.

---
## 1. Requisitos do sistema

### 1.1 Requisitos funcionais (RF):
- **RF-001:** O usuário autentica no sistema
- **RF-002:** O aluno seleciona uma instituição participante a partir de uma lista de opções. 
- **RF-003:** O sistema adiciona ao saldo corrente do professor 1000 (mil) moedas a cada semestre.
- **RF-004:** O professor distribui moedas aos alunos, indicando o motivo da transferência.
- **RF-005:** O sistema notifica ao aluno que ele recebeu moeda(s) de um professor.
- **RF-006:** O usuário consulta o extrato da sua conta.
- **RF-007:** O aluno troca moedas por uma vantagem.
- **RF-008:** A empresa parceira gerencia vantagens que deseja oferecer, bem como o custo em moedas estudantis.
- **RF-009:** O sistema envia um email  com cupom ao aluno quando ele resgata uma vantagem.
- **RF-010:** O sistema envia um email quando há uma troca à empresa parceira.
- **RF-11:** O professor cadastra critérios de trasnferencia
- **RF-12:**

### 1.2 Requisitos não funcionais (RNF):
- **RNF-001:** O sistema deve ser desenvolvido utilizando a arquitetura MVC.
- **RNF-002:** O sistema deve implementar uma estratégia de acesso ao banco de dados, como ORM ou Padrão DAO.

---
## 2. Histórias de Usuário (User Stories)

### 2.1 Histórias de Usuário - Aluno

**USER STORY 01**
* **Como um** aluno
* **eu quero** realizar um cadastro selecionando minha instituição de ensino pré-cadastrada
* **para que** eu possa ingressar no sistema de mérito.

**USER STORY 02**
* **Como um** aluno
* **eu quero** resgatar vantagens cadastradas por parceiros
* **para que** eu possa trocar minhas moedas por benefícios (como descontos e materiais) e ter o valor descontado do meu saldo.

**USER STORY 03**
* **Como um** aluno
* **eu quero** consultar o extrato de minha conta
* **para que** eu visualize as transações de recebimento ou troca que realizei.

---
### 2.2 Histórias de Usuário - Professor

**USER STORY 04**
* **Como um** professor
* **eu quero** enviar moedas aos alunos com um motivo obrigatório
* **para que** eu possa realizar o reconhecimento por bom comportamento e participação em aula.

**USER STORY 05**
* **Como um** professor
* **eu quero** consultar o meu extrato
* **para que** eu veja o total de moedas que ainda possuo e as transações de envio realizadas.

---
### 2.3 Histórias de Usuário - Empresa Parceira

**USER STORY 06**
* **Como uma** empresa parceira
* **eu quero** cadastrar vantagens contendo descrição, foto do produto e custo em moedas
* **para que** eu possa oferecer benefícios para resgate no sistema.

---
### 2.4 Histórias de Usuário Transversais (Usuário Geral / Sistema)

**USER STORY 07**
* **Como um** usuário (aluno, professor ou empresa parceira)
* **eu quero** me autenticar no sistema utilizando meu login e senha
* **para que** eu possa acessar e realizar os requisitos do sistema de forma segura.

**USER STORY 08**
* **Como o** sistema
* **eu quero** enviar notificações por email para o aluno (ao receber moedas ou cupom) e para o parceiro (com código de verificação)
* **para que** o fluxo de informação e a conferência de troca presencial sejam facilitados.

---
## 3. Modelagem e Diagramas UML

Durante o Processo de Desenvolvimento, foram definidos os seguintes artefatos de especificação e modelagem para o projeto:
* Veja o [**diagrama de casos de uso**](Artefatos/diagrama-casos-de-uso.pdf)
* Veja o [**diagrama de classes**](Artefatos/diagrama-classes-v2.pdf)
* Veja o [**diagrama de componentes**](Artefatos/diagrama-componentes.pdf)
* **Modelo ER**

---
## 🛠️ Tecnologias e Arquitetura

* **Padrão de Projeto / Arquitetura:** Arquitetura MVC.
* **Persistência de Dados:** Estratégia de acesso ao banco de dados utilizando abordagens como ORM ou Padrão DAO.
* **Estrutura:** Separação entre front-end e back-end para as operações de comunicação e CRUDs.
* **Controle de Versão:** Utilização de repositório GitHub para controle do código e modelos UML.

---
## 👥 Autores / Disciplina

* **Alunos**: Caio César Falinacio dos Santos, Luiz Fernando Cunha Maia e Pedro Henrique Nogueira
* **Disciplina:** Laboratório de Desenvolvimento de Software.
* **Professor Responsável:** Glender Brás.
