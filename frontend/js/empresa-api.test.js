const test = require('node:test');
const assert = require('node:assert/strict');
const { createEmpresaService } = require('./empresa-api.js');
const { createSessionStore } = require('./core/session.js');

const empresa = { id: 5, nome: 'Cantina', email: 'c@x.br', cnpj: '12.345.678/0001-90' };

function sessionStore(value) {
  const values = new Map();
  const session = createSessionStore({ getItem: key => values.get(key) ?? null, setItem: (key, data) => values.set(key, data), removeItem: key => values.delete(key) });
  if (value) session.save(value);
  return session;
}

test('cadastro de empresa propaga indisponibilidade sem criar sessão local', async () => {
  const session = sessionStore();
  const error = Object.assign(new Error('Servidor indisponível.'), { status: 503 });
  const service = createEmpresaService({ request: async () => { throw error; }, authApi: {}, session });
  await assert.rejects(service.cadastrar({ ...empresa, senha: 'segredo' }), caught => caught === error);
  assert.equal(session.read(), null);
});

test('cadastro envia POST remoto e devolve perfil completo mantendo sessão mínima', async () => {
  const session = sessionStore();
  const dados = { ...empresa, senha: 'segredo' };
  const service = createEmpresaService({ request: async (url, options) => { assert.equal(url, 'http://localhost:8080/api/empresas'); assert.deepEqual(options, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(dados) }); return dados; }, authApi: {}, session });
  assert.equal((await service.cadastrar(dados)).cnpj, '12.345.678/0001-90');
  assert.deepEqual(session.read(), { id: 5, nome: 'Cantina', email: 'c@x.br', perfil: 'EMPRESA' });
});

test('login de empresa usa autenticação central e salva sessão segura', async () => {
  const session = sessionStore();
  const service = createEmpresaService({ request: async () => assert.fail('Login não usa CRUD'), authApi: { login: async (...args) => { assert.deepEqual(args, ['c@x.br', 'senha123', 'EMPRESA']); return { ...empresa, perfil: 'EMPRESA', senha: 'segredo' }; } }, session });
  assert.deepEqual(await service.login('c@x.br', 'senha123'), { id: 5, nome: 'Cantina', email: 'c@x.br', perfil: 'EMPRESA' });
  assert.equal(service.getEmpresaLogada().perfil, 'EMPRESA');
  service.logout();
  assert.equal(session.read(), null);
});

test('perfil completo é recarregado do backend após restauração de sessão', async () => {
  const service = createEmpresaService({ request: async url => { assert.equal(url, 'http://localhost:8080/api/empresas/5'); return empresa; }, authApi: {}, session: sessionStore({ ...empresa, perfil: 'EMPRESA' }) });
  assert.deepEqual(await service.getMeusDados(), empresa);
});

test('atualização remota preserva CNPJ e atualiza sessão mínima', async () => {
  const session = sessionStore({ ...empresa, perfil: 'EMPRESA' });
  const service = createEmpresaService({ request: async (url, options) => { assert.equal(url, 'http://localhost:8080/api/empresas/5'); assert.deepEqual(options, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ nome: 'Cantina Nova' }) }); return { ...empresa, nome: 'Cantina Nova' }; }, authApi: {}, session });
  assert.equal((await service.atualizarMeusDados({ nome: 'Cantina Nova' })).cnpj, '12.345.678/0001-90');
  assert.equal(session.read().nome, 'Cantina Nova');
});

test('falha ao atualizar ou excluir empresa preserva sessão e propaga erro', async () => {
  const session = sessionStore({ ...empresa, perfil: 'EMPRESA' });
  const error = new Error('Servidor indisponível.');
  const service = createEmpresaService({ request: async () => { throw error; }, authApi: {}, session });
  await assert.rejects(service.atualizarMeusDados({ nome: 'Outro' }), caught => caught === error);
  await assert.rejects(service.excluirMinhaConta(), caught => caught === error);
  assert.equal(session.read().nome, 'Cantina');
});

test('exclusão só encerra sessão após confirmação remota', async () => {
  const session = sessionStore({ ...empresa, perfil: 'EMPRESA' });
  const service = createEmpresaService({ request: async (url, options) => { assert.equal(url, 'http://localhost:8080/api/empresas/5'); assert.deepEqual(options, { method: 'DELETE' }); assert.equal(session.read().id, 5); }, authApi: {}, session });
  await service.excluirMinhaConta();
  assert.equal(session.read(), null);
});

test('sessão ausente ou de outro perfil impede acesso remoto à empresa', async () => {
  for (const value of [null, { ...empresa, perfil: 'ALUNO' }]) {
    const service = createEmpresaService({ request: async () => assert.fail('Sessão inválida não acessa API'), authApi: {}, session: sessionStore(value) });
    assert.equal(service.getEmpresaLogada(), null);
    await assert.rejects(service.getMeusDados(), /autenticada/);
    await assert.rejects(service.atualizarMeusDados({}), /autenticada/);
    await assert.rejects(service.excluirMinhaConta(), /autenticada/);
  }
});
