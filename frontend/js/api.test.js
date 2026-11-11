const test = require('node:test');
const assert = require('node:assert/strict');
const { createAlunoService } = require('./api.js');
const { createSessionStore } = require('./core/session.js');
const { readFileSync } = require('node:fs');
const vm = require('node:vm');

const aluno = { id: 7, nome: 'Ana', email: 'ana@puc.br', cpf: '123', rg: 'MG-1', curso: 'Computação', instituicaoId: 2, instituicaoNome: 'PUC Minas', endereco: 'Rua A' };

function sessionStore(value) {
  const values = new Map();
  const session = createSessionStore({ getItem: key => values.get(key) ?? null, setItem: (key, data) => values.set(key, data), removeItem: key => values.delete(key) });
  if (value) session.save(value);
  return session;
}

test('cadastro de aluno propaga indisponibilidade sem criar sessão local', async () => {
  const session = sessionStore();
  const error = Object.assign(new Error('Servidor indisponível.'), { status: 503 });
  const service = createAlunoService({ request: async () => { throw error; }, authApi: {}, session });
  await assert.rejects(service.cadastrar({ ...aluno, senha: 'segredo' }), caught => caught === error);
  assert.equal(session.read(), null);
});

test('cadastro envia POST remoto e devolve perfil completo mantendo sessão mínima', async () => {
  const session = sessionStore();
  let captured;
  const service = createAlunoService({ request: async (url, options) => { captured = { url, options }; return { ...aluno, senha: 'segredo' }; }, authApi: {}, session });
  const dados = { ...aluno, senha: 'segredo' };
  const result = await service.cadastrar(dados);
  assert.equal(captured.url, 'http://localhost:8080/api/alunos');
  assert.deepEqual(captured.options, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(dados) });
  assert.equal(result.curso, 'Computação');
  assert.deepEqual(session.read(), { id: 7, nome: 'Ana', email: 'ana@puc.br', perfil: 'ALUNO' });
});

test('instituições vêm exclusivamente do endpoint remoto', async () => {
  const service = createAlunoService({ request: async url => { assert.equal(url, 'http://localhost:8080/api/instituicoes'); return [{ id: 2, nome: 'PUC Minas' }]; }, authApi: {}, session: sessionStore() });
  assert.deepEqual(await service.getInstituicoes(), [{ id: 2, nome: 'PUC Minas' }]);
});

test('login de aluno usa autenticação central e salva sessão segura', async () => {
  const session = sessionStore();
  const service = createAlunoService({ request: async () => assert.fail('Login não usa CRUD'), authApi: { login: async (...args) => { assert.deepEqual(args, ['ana@puc.br', 'senha123', 'ALUNO']); return { ...aluno, perfil: 'ALUNO', senha: 'segredo' }; } }, session });
  assert.deepEqual(await service.login('ana@puc.br', 'senha123'), { id: 7, nome: 'Ana', email: 'ana@puc.br', perfil: 'ALUNO' });
  assert.equal(service.getAlunoLogado().perfil, 'ALUNO');
  service.logout();
  assert.equal(session.read(), null);
});

test('perfil completo é recarregado do backend após restauração de sessão', async () => {
  const service = createAlunoService({ request: async url => { assert.equal(url, 'http://localhost:8080/api/alunos/7'); return aluno; }, authApi: {}, session: sessionStore({ ...aluno, perfil: 'ALUNO' }) });
  assert.deepEqual(await service.getMeusDados(), aluno);
});

test('atualização remota preserva dados completos e atualiza sessão mínima', async () => {
  const session = sessionStore({ ...aluno, perfil: 'ALUNO' });
  const service = createAlunoService({ request: async (url, options) => { assert.equal(url, 'http://localhost:8080/api/alunos/7'); assert.deepEqual(options, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ nome: 'Ana Silva' }) }); return { ...aluno, nome: 'Ana Silva' }; }, authApi: {}, session });
  const result = await service.atualizarMeusDados({ nome: 'Ana Silva' });
  assert.equal(result.cpf, '123');
  assert.equal(session.read().nome, 'Ana Silva');
});

test('falha ao atualizar ou excluir aluno preserva sessão e propaga erro', async () => {
  const session = sessionStore({ ...aluno, perfil: 'ALUNO' });
  const error = new Error('Servidor indisponível.');
  const service = createAlunoService({ request: async () => { throw error; }, authApi: {}, session });
  await assert.rejects(service.atualizarMeusDados({ nome: 'Outro' }), caught => caught === error);
  await assert.rejects(service.excluirMinhaConta(), caught => caught === error);
  assert.equal(session.read().nome, 'Ana');
});

test('exclusão só encerra sessão após confirmação remota', async () => {
  const session = sessionStore({ ...aluno, perfil: 'ALUNO' });
  const service = createAlunoService({ request: async (url, options) => { assert.equal(url, 'http://localhost:8080/api/alunos/7'); assert.deepEqual(options, { method: 'DELETE' }); assert.equal(session.read().id, 7); }, authApi: {}, session });
  await service.excluirMinhaConta();
  assert.equal(session.read(), null);
});

test('sessão ausente ou de outro perfil impede acesso remoto ao aluno', async () => {
  for (const value of [null, { ...aluno, perfil: 'EMPRESA' }]) {
    const service = createAlunoService({ request: async () => assert.fail('Sessão inválida não acessa API'), authApi: {}, session: sessionStore(value) });
    assert.equal(service.getAlunoLogado(), null);
    await assert.rejects(service.getMeusDados(), /autenticado/);
    await assert.rejects(service.atualizarMeusDados({}), /autenticado/);
    await assert.rejects(service.excluirMinhaConta(), /autenticado/);
  }
});

async function openPortal(script, serviceName, service) {
  const nodes = new Map();
  const makeNode = () => ({ value: '', style: {}, textContent: '', innerHTML: '', children: [], listeners: {}, classList: { add() {}, remove() {} }, addEventListener(name, fn) { this.listeners[name] = fn; }, appendChild(node) { this.children.push(node); }, reset() {} });
  let ready;
  let destination;
  const window = { [serviceName]: service, AppRoutes: { dashboard: user => `dashboard-${user.perfil.toLowerCase()}.html` }, location: { assign: target => { destination = target; } } };
  const document = { addEventListener: (name, fn) => { ready = fn; }, getElementById: id => { if (!nodes.has(id)) nodes.set(id, makeNode()); return nodes.get(id); }, createElement: makeNode };
  vm.runInNewContext(readFileSync(require.resolve(script), 'utf8'), { window, document, setTimeout() {} });
  await ready();
  return { node: id => document.getElementById(id), destination: () => destination };
}

test('portais restauram detalhes remotos e preenchem edição sem depender da sessão mínima', async () => {
  const { createEmpresaService } = require('./empresa-api.js');
  const empresa = { id: 5, nome: 'Cantina', email: 'c@x.br', cnpj: '12.345.678/0001-90' };
  for (const [script, name, service, field, expected, editField] of [
    ['./app.js', 'alunoAuthService', createAlunoService({ request: async url => url.endsWith('/instituicoes') ? [{ id: 2, nome: 'PUC Minas' }] : aluno, authApi: {}, session: sessionStore({ ...aluno, perfil: 'ALUNO' }) }), 'viewCpf', '123', 'editCpf'],
    ['./empresa.js', 'empresaAuthService', createEmpresaService({ request: async () => empresa, authApi: {}, session: sessionStore({ ...empresa, perfil: 'EMPRESA' }) }), 'viewCnpj', '12.345.678/0001-90', 'editCnpj']
  ]) {
    const portal = await openPortal(script, name, service);
    assert.equal(portal.node(field).textContent, expected);
    await portal.node('btnOpenEdit').listeners.click();
    assert.equal(portal.node(editField).value, expected);
  }
});

test('login nos portais redireciona ao dashboard do perfil autenticado', async () => {
  const { createEmpresaService } = require('./empresa-api.js');
  for (const [script, name, factory, perfil] of [
    ['./app.js', 'alunoAuthService', createAlunoService, 'ALUNO'],
    ['./empresa.js', 'empresaAuthService', createEmpresaService, 'EMPRESA']
  ]) {
    const service = factory({ request: async () => [], authApi: { login: async () => ({ id: 7, nome: 'Conta', email: 'conta@x.br', perfil }) }, session: sessionStore() });
    const portal = await openPortal(script, name, service);
    portal.node('loginEmail').value = 'conta@x.br';
    portal.node('loginSenha').value = 'senha123';
    await portal.node('loginForm').listeners.submit({ preventDefault() {} });
    assert.equal(portal.destination(), `dashboard-${perfil.toLowerCase()}.html`);
  }
});

test('falha no carregamento de instituições mostra erro e mantém login funcional', async () => {
  const service = createAlunoService({ request: async () => { throw new Error('Servidor indisponível.'); }, authApi: { login: async () => ({ ...aluno, perfil: 'ALUNO' }) }, session: sessionStore() });
  const portal = await openPortal('./app.js', 'alunoAuthService', service);
  assert.equal(portal.node('toastContainer').children[0].textContent, 'Servidor indisponível.');
  portal.node('loginEmail').value = 'ana@puc.br';
  await portal.node('loginForm').listeners.submit({ preventDefault() {} });
  assert.equal(portal.destination(), 'dashboard-aluno.html');
});
