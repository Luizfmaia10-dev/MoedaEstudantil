const test = require('node:test');
const assert = require('node:assert/strict');
const { createTransferenciaService } = require('./transferencia-api.js');

test('interpretação envia mensagem e rascunho ao endpoint do aluno', async () => {
  let captured;
  const request = async (url, options) => {
    captured = { url, options };
    return { estado: 'PRECISA_PROFESSOR' };
  };
  const service = createTransferenciaService(request);
  const rascunho = { destinatarioId: 6, destinatarioNome: 'Ana', quantidade: 20 };

  const response = await service.interpretar(4, 'pelo professor Carlos', rascunho);

  assert.equal(captured.url, 'http://localhost:8080/api/alunos/4/transferencias/assistente/interpretar');
  assert.deepEqual(captured.options, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ mensagem: 'pelo professor Carlos', rascunho })
  });
  assert.deepEqual(response, { estado: 'PRECISA_PROFESSOR' });
});

test('solicitação persistente mantém o contrato existente', async () => {
  let captured;
  const service = createTransferenciaService(async (url, options) => {
    captured = { url, options };
    return { id: 10, status: 'PENDENTE' };
  });
  const payload = { destinatarioId: 6, avaliadorId: 2, quantidade: 20, justificativa: '' };

  await service.solicitar(4, payload);

  assert.equal(captured.url, 'http://localhost:8080/api/alunos/4/transferencias');
  assert.equal(captured.options.method, 'POST');
  assert.equal(captured.options.body, JSON.stringify(payload));
});
