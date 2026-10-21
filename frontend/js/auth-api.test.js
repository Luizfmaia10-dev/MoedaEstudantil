const test = require('node:test');
const assert = require('node:assert/strict');
const { createAuthApi } = require('./auth-api.js');

test('login envia perfil junto às credenciais', async () => {
  let captured;
  const request = async (url, options) => {
    captured = { url, options };
    return { id: 1, nome: 'Glender', email: 'prof@puc.br', perfil: 'PROFESSOR' };
  };

  const result = await createAuthApi(request).login(' prof@puc.br ', 'senha123', 'PROFESSOR');

  assert.equal(captured.url, 'http://localhost:8080/api/auth/login');
  assert.deepEqual(captured.options, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'prof@puc.br', senha: 'senha123', perfil: 'PROFESSOR' })
  });
  assert.deepEqual(result, { id: 1, nome: 'Glender', email: 'prof@puc.br', perfil: 'PROFESSOR' });
});

test('login propaga HttpError do cliente HTTP', async () => {
  const error = Object.assign(new Error('Credenciais inválidas.'), { name: 'HttpError', status: 401 });
  const api = createAuthApi(async () => { throw error; });

  await assert.rejects(api.login('a@puc.br', 'errada', 'ALUNO'), caught => caught === error);
});
