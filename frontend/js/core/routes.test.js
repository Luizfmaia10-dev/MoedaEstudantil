const test = require('node:test');
const assert = require('node:assert/strict');
const { dashboard, cardsFor } = require('./routes.js');

test('dashboard aponta para o painel autenticado', () => {
  assert.equal(dashboard(), 'painel.html');
});

test('cardsFor entrega somente ações do aluno com seu id', () => {
  assert.deepEqual(cardsFor({ id: 4, perfil: 'ALUNO' }).map(card => card.href), [
    'resgatar-vantagens.html?alunoId=4',
    'extrato.html?usuarioId=4',
    'solicitar-transferencia.html?alunoId=4',
    'criterios-transferencia.html?alunoId=4'
  ]);
});

test('cardsFor entrega somente ações do professor com seu id', () => {
  assert.deepEqual(cardsFor({ id: 9, perfil: 'PROFESSOR' }).map(card => card.href), [
    'distribuir-moedas.html?professorId=9',
    'extrato.html?usuarioId=9',
    'criterios-professor.html?professorId=9',
    'avaliar-transferencias.html?professorId=9'
  ]);
});

test('cardsFor entrega somente ações da empresa com seu id', () => {
  assert.deepEqual(cardsFor({ id: 5, perfil: 'EMPRESA' }).map(card => card.href), [
    'vantagens.html?empresaId=5'
  ]);
});

test('cardsFor devolve cópias dos cartões', () => {
  const first = cardsFor({ id: 4, perfil: 'ALUNO' });
  first[0].title = 'alterado';
  assert.notEqual(cardsFor({ id: 4, perfil: 'ALUNO' })[0].title, 'alterado');
});

test('cardsFor recusa perfil ou identidade inválidos', () => {
  assert.throws(() => cardsFor({ id: 1, perfil: 'ADMIN' }), /Perfil inválido/);
  assert.throws(() => cardsFor({ perfil: 'ALUNO' }), /Perfil inválido/);
});

test('todos os cartões possuem conteúdo descritivo', () => {
  for (const perfil of ['ALUNO', 'PROFESSOR', 'EMPRESA']) {
    for (const card of cardsFor({ id: 1, perfil })) {
      assert.ok(card.title);
      assert.ok(card.description);
      assert.ok(card.eyebrow);
      assert.ok(card.href);
    }
  }
});
