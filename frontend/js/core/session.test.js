const test = require('node:test');
const assert = require('node:assert/strict');
const { createSessionStore } = require('./session.js');

function memoryStorage(initial = []) {
  const values = new Map(initial);
  return {
    values,
    getItem: key => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: key => values.delete(key)
  };
}

test('sessão persiste somente identidade segura', () => {
  const storage = memoryStorage();
  const session = createSessionStore(storage);

  session.save({
    id: 4, nome: 'Aluno Teste', email: 'aluno@puc.br', perfil: 'ALUNO',
    senha: 'segredo', cpf: '000.000.000-00'
  });

  assert.deepEqual(session.read(), {
    id: 4, nome: 'Aluno Teste', email: 'aluno@puc.br', perfil: 'ALUNO'
  });
  assert.doesNotMatch(storage.values.values().next().value, /segredo|000\.000/);
});

test('sessão inválida é descartada', () => {
  const storage = memoryStorage([['moeda_estudantil_sessao_v1', '{"id":4,"perfil":"ADMIN"}']]);
  const session = createSessionStore(storage);

  assert.equal(session.read(), null);
  assert.equal(storage.getItem('moeda_estudantil_sessao_v1'), null);
});

test('clear remove sessão do sessionStorage', () => {
  const storage = memoryStorage();
  const session = createSessionStore(storage);
  session.save({ id: 4, nome: 'Aluno', email: 'a@puc.br', perfil: 'ALUNO' });

  session.clear();

  assert.equal(session.read(), null);
});

test('save rejeita dados e perfis inválidos', () => {
  const session = createSessionStore(memoryStorage());
  assert.throws(() => session.save({ id: 1, perfil: 'ADMIN' }), /Sessão inválida/);
  assert.throws(() => session.save({ id: 'x', perfil: 'ALUNO' }), /Sessão inválida/);
});

test('read descarta JSON corrompido', () => {
  const storage = memoryStorage([['moeda_estudantil_sessao_v1', '{']]);
  const session = createSessionStore(storage);

  assert.equal(session.read(), null);
  assert.equal(storage.getItem('moeda_estudantil_sessao_v1'), null);
});
