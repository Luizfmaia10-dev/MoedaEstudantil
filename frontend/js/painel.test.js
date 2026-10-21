const test = require('node:test');
const assert = require('node:assert/strict');
const { buildDashboardModel, initializeDashboard } = require('./painel.js');
const { cardsFor } = require('./core/routes.js');

test('painel deriva saudação e ações somente da sessão autenticada', () => {
  const model = buildDashboardModel({ id: 1, nome: '  Glender Brás ', perfil: 'PROFESSOR' }, cardsFor);
  assert.equal(model.firstName, 'Glender');
  assert.equal(model.profileLabel, 'Professor');
  assert.equal(model.cards.length, 4);
  assert.equal(model.cards[0].href, 'distribuir-moedas.html?professorId=1');
  assert.ok(model.cards.every(card => !card.href.includes('alunoId')));
});

test('painel recusa ausência de sessão e perfil desconhecido', () => {
  assert.throws(() => buildDashboardModel(null, cardsFor), /Sessão necessária/);
  assert.throws(() => buildDashboardModel({ perfil: 'ADMIN' }, cardsFor), /Perfil inválido/);
});

test('aluno e empresa recebem apenas suas ações e nome vazio usa o perfil', () => {
  const student = buildDashboardModel({ id: 2, nome: '', perfil: 'ALUNO' }, cardsFor);
  const company = buildDashboardModel({ id: 3, nome: 'Café Escola', perfil: 'EMPRESA' }, cardsFor);
  assert.equal(student.firstName, 'Aluno');
  assert.equal(student.cards.length, 5);
  assert.equal(student.cards[0].href, 'resgatar-vantagens.html?alunoId=2');
  assert.equal(company.profileLabel, 'Empresa parceira');
  assert.equal(student.cards[4].href, 'aluno.html');
  assert.equal(company.cards.length, 2);
  assert.equal(company.cards[0].href, 'vantagens.html?empresaId=3');
  assert.equal(company.cards[1].href, 'empresa.html');
  assert.ok(student.cards.every(card => !card.href.includes('empresa')));
  assert.ok(company.cards.every(card => !card.href.includes('aluno')));
});

function documentFixture() {
  function element(tagName) {
    return {
      tagName, children: [], attributes: {}, textContent: '',
      setAttribute(name, value) { this.attributes[name] = value; },
      appendChild(child) { this.children.push(child); },
      replaceChildren() { this.children = []; },
      addEventListener(name, handler) { this[name] = handler; }
    };
  }
  const nodes = Object.fromEntries(['profileLabel', 'dashboardTitle', 'actionGrid', 'logoutButton'].map(id => [id, element('div')]));
  return { nodes, createElement: element, getElementById: id => nodes[id] };
}

test('sessão ausente redireciona para a entrada sem consultar ações', () => {
  const location = { href: '' };
  initializeDashboard({ document: documentFixture(), location, sessionStore: { read: () => null }, cardsFor });
  assert.equal(location.href, 'index.html?entrar=1');
});

test('renderiza dados como texto, ignora perfil da URL e logout encerra sessão', () => {
  const document = documentFixture();
  const location = { href: '', search: '?perfil=PROFESSOR&alunoId=999' };
  let cleared = false;
  initializeDashboard({ document, location, sessionStore: {
    read: () => ({ id: 3, nome: '<img onerror=alert(1)> Loja', perfil: 'EMPRESA' }),
    clear: () => { cleared = true; }
  }, cardsFor });
  assert.equal(document.nodes.dashboardTitle.textContent, 'Olá, <img!');
  assert.equal(document.nodes.profileLabel.textContent, 'Empresa parceira');
  const cards = document.nodes.actionGrid.children;
  assert.equal(cards.length, 2);
  assert.equal(cards[0].attributes.href, 'vantagens.html?empresaId=3');
  assert.ok(cards[0].children.some(child => child.textContent === 'Gestão de Vantagens'));
  assert.equal(cards[1].attributes.href, 'empresa.html');
  assert.ok(cards[1].children.some(child => /perfil|conta/i.test(child.textContent)));
  document.nodes.logoutButton.click();
  assert.equal(cleared, true);
  assert.equal(location.href, 'index.html');
});
