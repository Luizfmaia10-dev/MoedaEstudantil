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

-- 5. Carga de exemplo para testar o extrato (RF-006)
-- Aluno de teste: usuario_id 4 / conta 4. Professor Glender: usuario_id 1 / conta 1.
INSERT IGNORE INTO usuario (id, email, senha, tipo_usuario)
VALUES (4, 'aluno.teste@sga.pucminas.br', 'senha123', 'ALUNO');

INSERT IGNORE INTO aluno (usuario_id, nome, cpf, rg, endereco, curso, instituicao_id)
VALUES (4, 'Aluno Teste', '444.444.444-44', 'MG-00.000.000', 'Rua Teste, 100', 'Engenharia de Software', 1);

INSERT IGNORE INTO conta (id, saldo_corrente, usuario_id) VALUES (4, 70, 4);

-- Professor envia 100 moedas ao aluno (aparece nos dois extratos)
INSERT IGNORE INTO transacao (id, tipo_transacao, data_hora, quantidade, tipo_movimentacao, conta_id, motivo, professor_id, aluno_id)
VALUES (1, 'ENVIO', '2026-09-01 10:00:00', 100, 'SAIDA', 1, 'Excelente participacao em aula', 1, 4);
INSERT IGNORE INTO transacao (id, tipo_transacao, data_hora, quantidade, tipo_movimentacao, conta_id, motivo, professor_id, aluno_id)
VALUES (2, 'ENVIO', '2026-09-01 10:00:00', 100, 'ENTRADA', 4, 'Excelente participacao em aula', 1, 4);

-- Aluno recebe mais 30 moedas e depois resgata uma vantagem de 60 (saldo final 70)
INSERT IGNORE INTO transacao (id, tipo_transacao, data_hora, quantidade, tipo_movimentacao, conta_id, motivo, professor_id, aluno_id)
VALUES (3, 'ENVIO', '2026-09-15 14:30:00', 30, 'SAIDA', 1, 'Entrega antecipada do laboratorio', 1, 4);
INSERT IGNORE INTO transacao (id, tipo_transacao, data_hora, quantidade, tipo_movimentacao, conta_id, motivo, professor_id, aluno_id)
VALUES (4, 'ENVIO', '2026-09-15 14:30:00', 30, 'ENTRADA', 4, 'Entrega antecipada do laboratorio', 1, 4);
INSERT IGNORE INTO transacao (id, tipo_transacao, data_hora, quantidade, tipo_movimentacao, conta_id, codigo_cupom, aluno_id)
VALUES (5, 'RESGATE', '2026-10-02 09:15:00', 60, 'SAIDA', 4, 'CUPOM-ABC123', 4);
-- 6. Carga de exemplo para testar a gestao de vantagens (RF-008)
INSERT IGNORE INTO usuario (id, email, senha, tipo_usuario) VALUES (5, 'contato@cantinadapuc.com.br', 'senha123', 'EMPRESA');
INSERT IGNORE INTO empresa_parceira (usuario_id, nome, cnpj) VALUES (5, 'Cantina da PUC', '12.345.678/0001-90');
INSERT IGNORE INTO vantagem (id, nome, descricao, foto_url, custo_moedas, ativa, empresa_id) VALUES (1, 'Combo Lanche', 'Sanduiche natural + suco de 300ml', NULL, 40, true, 5);
INSERT IGNORE INTO vantagem (id, nome, descricao, foto_url, custo_moedas, ativa, empresa_id) VALUES (2, 'Desconto de 20% na copiadora', 'Valido para impressoes e copias do mes', NULL, 25, true, 5);

-- 7. RF-011: amplia a coluna de criterios para 1000 caracteres (ddl-auto=update nao altera colunas existentes)
ALTER TABLE professor MODIFY criterios_transferencia VARCHAR(1000);


-- 8. RF-012: garante que a coluna status aceite CANCELADA (caso tenha sido criada como ENUM antes)
ALTER TABLE solicitacao_transferencia MODIFY status VARCHAR(20);

-- Colegas para testar solicitacao de transferencia (RF-012)
-- Alunos 6 e 7 sao da PUC Minas (instituicao 1, mesma do Aluno 4); Aluno 8 e da UFMG (instituicao 2)
INSERT IGNORE INTO usuario (id, email, senha, tipo_usuario)
VALUES (6, 'mariana.costa@sga.pucminas.br', 'senha123', 'ALUNO');
INSERT IGNORE INTO aluno (usuario_id, nome, cpf, rg, endereco, curso, instituicao_id)
VALUES (6, 'Mariana Costa', '666.666.666-66', 'MG-66.666.666', 'Rua A, 10', 'Engenharia de Software', 1);
INSERT IGNORE INTO conta (id, saldo_corrente, usuario_id) VALUES (6, 50, 6);

INSERT IGNORE INTO usuario (id, email, senha, tipo_usuario)
VALUES (7, 'lucas.ferreira@sga.pucminas.br', 'senha123', 'ALUNO');
INSERT IGNORE INTO aluno (usuario_id, nome, cpf, rg, endereco, curso, instituicao_id)
VALUES (7, 'Lucas Ferreira', '777.777.777-77', 'MG-77.777.777', 'Rua B, 20', 'Ciencia da Computacao', 1);
INSERT IGNORE INTO conta (id, saldo_corrente, usuario_id) VALUES (7, 20, 7);

INSERT IGNORE INTO usuario (id, email, senha, tipo_usuario)
VALUES (8, 'pedro.almeida@ufmg.br', 'senha123', 'ALUNO');
INSERT IGNORE INTO aluno (usuario_id, nome, cpf, rg, endereco, curso, instituicao_id)
VALUES (8, 'Pedro Almeida', '888.888.888-88', 'MG-88.888.888', 'Av. Antonio Carlos, 6627', 'Sistemas de Informacao', 2);
INSERT IGNORE INTO conta (id, saldo_corrente, usuario_id) VALUES (8, 100, 8);
