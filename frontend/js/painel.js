(function initializePanel(root) {
  'use strict';

  function buildDashboardModel(session, cardsFor) {
    if (!session) throw new Error('Sessão necessária.');
    const labels = { ALUNO: 'Aluno', PROFESSOR: 'Professor', EMPRESA: 'Empresa parceira' };
    if (!Object.hasOwn(labels, session.perfil)) throw new Error('Perfil inválido.');
    return {
      firstName: String(session.nome || '').trim().split(/\s+/)[0] || labels[session.perfil],
      profileLabel: labels[session.perfil],
      cards: cardsFor(session)
    };
  }

  function initializeDashboard({ document, location, sessionStore, cardsFor }) {
    let model;
    try {
      model = buildDashboardModel(sessionStore.read(), cardsFor);
    } catch {
      location.href = 'index.html?entrar=1';
      return;
    }

    document.getElementById('profileLabel').textContent = model.profileLabel;
    document.getElementById('dashboardTitle').textContent = `Olá, ${model.firstName}!`;
    const grid = document.getElementById('actionGrid');
    grid.replaceChildren();
    model.cards.forEach((card, index) => {
      const link = document.createElement('a');
      link.setAttribute('class', 'action-card');
      link.setAttribute('href', card.href);
      const number = document.createElement('span');
      number.setAttribute('class', 'action-number');
      number.setAttribute('aria-hidden', 'true');
      number.textContent = String(index + 1).padStart(2, '0');
      const title = document.createElement('h2');
      title.textContent = card.title;
      const description = document.createElement('p');
      description.textContent = card.description;
      const action = document.createElement('span');
      action.setAttribute('class', 'action-link');
      action.setAttribute('aria-hidden', 'true');
      action.textContent = 'Acessar →';
      [number, title, description, action].forEach(child => link.appendChild(child));
      grid.appendChild(link);
    });
    document.getElementById('logoutButton').addEventListener('click', () => {
      sessionStore.clear();
      location.href = 'index.html';
    });
  }

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = { buildDashboardModel, initializeDashboard };
  } else if (root.document) {
    initializeDashboard({
      document: root.document,
      location: root.location,
      sessionStore: root.AppSession.current,
      cardsFor: root.AppRoutes.cardsFor
    });
  }
})(typeof window !== 'undefined' ? window : globalThis);
