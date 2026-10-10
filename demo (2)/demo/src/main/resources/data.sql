-- 1. Carga de Instituições
INSERT INTO instituicao (id, nome) VALUES (1, 'PUC Minas');
INSERT INTO instituicao (id, nome) VALUES (2, 'Universidade Federal de Minas Gerais (UFMG)');

-- 2. Carga de Usuários (Base para os Professores)
-- Nota: Em um ambiente real, as senhas estariam criptografadas (BCrypt). 
INSERT INTO usuario (id, email, senha, tipo_usuario) 
VALUES (1, 'glender.bras@pucminas.br', 'senha123', 'PROFESSOR');

INSERT INTO usuario (id, email, senha, tipo_usuario) 
VALUES (2, 'helena.souza@pucminas.br', 'senha123', 'PROFESSOR');

INSERT INTO usuario (id, email, senha, tipo_usuario) 
VALUES (3, 'carlos.teatro@ufmg.br', 'senha123', 'PROFESSOR');

-- 3. Carga de Professores (Especialização do Usuário)
INSERT INTO professor (usuario_id, nome, cpf, departamento, criterios_transferencia, instituicao_id) 
VALUES (1, 'Glender Brás', '111.111.111-11', 'Engenharia de Software', 'Aprovação para alunos com nota acima de 80 e boa participação.', 1);

INSERT INTO professor (usuario_id, nome, cpf, departamento, criterios_transferencia, instituicao_id) 
VALUES (2, 'Helena Souza', '222.222.222-22', 'Engenharia de Software', 'Aprovo se o aluno entregar todos os laboratórios no prazo.', 1);

INSERT INTO professor (usuario_id, nome, cpf, departamento, criterios_transferencia, instituicao_id) 
VALUES (3, 'Carlos Silva', '333.333.333-33', 'Escola de Belas Artes - Teatro', 'Aprovo se o aluno ajudar na organização técnica ou ensaios.', 2);

-- 4. Carga Inicial de Contas para os Professores
-- A Regra de Negócio exige que os professores recebam 1000 moedas no semestre.
INSERT INTO conta (id, saldo_corrente, usuario_id) VALUES (1, 1000, 1);
INSERT INTO conta (id, saldo_corrente, usuario_id) VALUES (2, 1000, 2);
INSERT INTO conta (id, saldo_corrente, usuario_id) VALUES (3, 1000, 3);