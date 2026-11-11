const test = require('node:test');
const assert = require('node:assert/strict');

const { request, HttpError } = require('./http.js');

test('request devolve JSON de uma resposta bem-sucedida', async () => {
  const fetchImpl = async () => new Response(JSON.stringify({ id: 7 }), {
    status: 200,
    headers: { 'Content-Type': 'application/json' }
  });

  assert.deepEqual(await request('/recurso', {}, fetchImpl), { id: 7 });
});

test('request devolve null para resposta vazia', async () => {
  const fetchImpl = async () => new Response(null, { status: 204 });

  assert.equal(await request('/recurso', {}, fetchImpl), null);
});

test('request expõe status e mensagem de erro do backend', async () => {
  const fetchImpl = async () => new Response(JSON.stringify({ message: 'Saldo insuficiente.' }), {
    status: 422,
    headers: { 'Content-Type': 'application/json' }
  });

  await assert.rejects(
    request('/recurso', {}, fetchImpl),
    error => error instanceof HttpError && error.status === 422 && error.message === 'Saldo insuficiente.'
  );
});
