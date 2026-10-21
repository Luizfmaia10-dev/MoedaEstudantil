const test = require('node:test');
const assert = require('node:assert/strict');
const { createLoginState, initializeLanding } = require('./landing.js');
const { createSessionStore } = require('./core/session.js');
const routes = require('./core/routes.js');

test('professor nunca recebe link de cadastro', () => {
  assert.deepEqual(createLoginState('PROFESSOR'), { perfil: 'PROFESSOR', label: 'Professor', cadastroHref: null });
});
test('aluno e empresa recebem seus destinos de cadastro', () => {
  assert.equal(createLoginState('ALUNO').cadastroHref, 'aluno.html?tab=cadastro');
  assert.equal(createLoginState('EMPRESA').cadastroHref, 'empresa.html?tab=cadastro');
});
test('perfil desconhecido e propriedades herdadas são recusados', () => {
  for (const profile of ['ADMIN', 'constructor', '__proto__', null]) {
    assert.throws(() => createLoginState(profile), /Perfil inválido/);
  }
});

function fixture({ login = async () => ({ id: 7, nome: 'Ana', email: 'ana@exemplo.com', perfil: 'ALUNO' }), search = '' } = {}) {
  function element(dataset = {}) {
    const handlers = {};
    return {
      dataset, attributes: {}, hidden: false, disabled: false, value: '', textContent: '', open: false, style: { setProperty() {}, removeProperty() {} },
      setAttribute(name, value) { this.attributes[name] = String(value); },
      getAttribute(name) { return this.attributes[name]; },
      removeAttribute(name) { delete this.attributes[name]; },
      addEventListener(name, handler) { handlers[name] = handler; },
      emit(name, event = {}) { return handlers[name]?.({ target: this, preventDefault() {}, ...event }); },
      focus() { this.focused = true; },
      showModal() { this.open = true; },
      close() { this.open = false; this.emit('close'); },
      getBoundingClientRect() { return { left: 20, top: 20, right: 400, bottom: 500, width: 380, height: 480 }; }
    };
  }
  const nodes = Object.fromEntries(['loginDialog', 'loginTitle', 'loginForm', 'loginEmail', 'loginPassword', 'loginError', 'loginSubmit', 'signupLink', 'closeLogin', 'menuToggle', 'mainNav', 'answer-1'].map(id => [id, element()]));
  nodes.menuToggle.setAttribute('aria-expanded', 'false');
  const openers = [element(), element({ profile: 'EMPRESA' })];
  const profiles = ['ALUNO', 'PROFESSOR', 'EMPRESA'].map(profile => element({ profile }));
  const faq = element();
  faq.setAttribute('aria-expanded', 'false');
  faq.setAttribute('aria-controls', 'answer-1');
  nodes['answer-1'].hidden = true;
  const navLink = element();
  const document = {
    getElementById: id => nodes[id],
    querySelector: () => null,
    querySelectorAll: selector => ({ '[data-open-login]': openers, '.profile-switcher [data-profile]': profiles, '[data-faq-toggle]': [faq], '#mainNav a': [navLink] })[selector] || []
  };
  const storage = new Map();
  const sessionStore = createSessionStore({ getItem: key => storage.get(key) ?? null, setItem: (key, value) => storage.set(key, value), removeItem: key => storage.delete(key) });
  const location = { search, assign(href) { this.destination = href; } };
  initializeLanding({ document, location, authApi: { login }, sessionStore, routes });
  return { nodes, openers, profiles, faq, navLink, sessionStore, location };
}

test('abre parceiro, alterna perfil e remove o href de cadastro do professor', () => {
  const f = fixture();
  f.openers[1].emit('click');
  assert.equal(f.nodes.loginDialog.open, true);
  assert.equal(f.profiles[2].getAttribute('aria-pressed'), 'true');
  assert.equal(f.nodes.signupLink.getAttribute('href'), 'empresa.html?tab=cadastro');
  f.profiles[1].emit('click');
  assert.equal(f.nodes.signupLink.hidden, true);
  assert.equal(f.nodes.signupLink.getAttribute('href'), undefined);
  assert.equal(f.profiles[2].getAttribute('aria-pressed'), 'false');
  f.profiles[0].emit('click');
  assert.equal(f.nodes.signupLink.hidden, false);
  assert.equal(f.nodes.signupLink.getAttribute('href'), 'aluno.html?tab=cadastro');
});

test('fecha pelo botão ou backdrop e devolve foco ao acionador', () => {
  const f = fixture();
  f.openers[0].emit('click');
  f.nodes.closeLogin.emit('click');
  assert.equal(f.nodes.loginDialog.open, false);
  assert.equal(f.openers[0].focused, true);
  f.openers[1].emit('click');
  f.nodes.loginDialog.emit('click', { clientX: 50, clientY: 50 });
  assert.equal(f.nodes.loginDialog.open, true);
  f.nodes.loginDialog.emit('click', { clientX: 5, clientY: 5 });
  assert.equal(f.nodes.loginDialog.open, false);
});

test('entrada redirecionada abre login e menu/FAQ expõem seus estados', () => {
  const f = fixture({ search: '?entrar=1' });
  assert.equal(f.nodes.loginDialog.open, true);
  f.nodes.menuToggle.emit('click');
  assert.equal(f.nodes.menuToggle.getAttribute('aria-expanded'), 'true');
  f.navLink.emit('click');
  assert.equal(f.nodes.menuToggle.getAttribute('aria-expanded'), 'false');
  f.faq.emit('click');
  assert.equal(f.faq.getAttribute('aria-expanded'), 'true');
  assert.equal(f.nodes['answer-1'].hidden, false);
  f.faq.emit('click');
  assert.equal(f.nodes['answer-1'].hidden, true);
});

test('login usa credenciais e perfil selecionado, salva sessão e só então navega', async () => {
  let credentials;
  const f = fixture({ login: async (...args) => {
    credentials = args;
    return { id: 12, nome: 'Loja', email: 'loja@exemplo.com', perfil: 'EMPRESA' };
  } });
  f.openers[1].emit('click');
  f.nodes.loginEmail.value = 'loja@exemplo.com';
  f.nodes.loginPassword.value = 'senha-exata';
  await f.nodes.loginForm.emit('submit');
  assert.deepEqual(credentials, ['loja@exemplo.com', 'senha-exata', 'EMPRESA']);
  assert.equal(f.sessionStore.read().perfil, 'EMPRESA');
  assert.equal(f.location.destination, 'painel.html');
  assert.equal(f.nodes.loginSubmit.disabled, false);
});

test('requisição pendente bloqueia submissão duplicada e mudança de perfil', async () => {
  let finish;
  let count = 0;
  const f = fixture({ login: () => { count += 1; return new Promise(resolve => { finish = resolve; }); } });
  const pending = f.nodes.loginForm.emit('submit');
  assert.equal(f.nodes.loginSubmit.disabled, true);
  assert.ok(f.profiles.every(button => button.disabled));
  await f.nodes.loginForm.emit('submit');
  assert.equal(count, 1);
  finish({ id: 7, nome: 'Ana', email: 'ana@exemplo.com', perfil: 'ALUNO' });
  await pending;
  assert.ok(f.profiles.every(button => !button.disabled));
});

for (const status of [401, 503]) {
  test(`erro ${status} preserva credenciais, não salva sessão e permite tentar novamente`, async () => {
    const f = fixture({ login: async () => { throw Object.assign(new Error('<img onerror=alert(1)>'), { status }); } });
    f.nodes.loginEmail.value = 'ana@exemplo.com';
    f.nodes.loginPassword.value = 'senha';
    await f.nodes.loginForm.emit('submit');
    assert.equal(f.nodes.loginError.textContent, status === 401 ? 'E-mail, senha ou perfil inválido.' : 'A plataforma está indisponível agora. Tente novamente.');
    assert.equal(f.nodes.loginEmail.value, 'ana@exemplo.com');
    assert.equal(f.nodes.loginPassword.value, 'senha');
    assert.equal(f.nodes.loginSubmit.disabled, false);
    assert.equal(f.sessionStore.read(), null);
    assert.equal(f.location.destination, undefined);
  });
}
