(function initializeSession(root) {
  const KEY = 'moeda_estudantil_sessao_v1';
  const PERFIS = new Set(['ALUNO', 'PROFESSOR', 'EMPRESA']);

  function createSessionStore(storage) {
    if (!storage) throw new Error('Armazenamento de sessão indisponível.');

    function sanitize(value) {
      if (!value || !Number.isFinite(Number(value.id)) || !PERFIS.has(value.perfil)) return null;
      return {
        id: Number(value.id),
        nome: String(value.nome || ''),
        email: String(value.email || ''),
        perfil: value.perfil
      };
    }

    return {
      save(value) {
        const safe = sanitize(value);
        if (!safe) throw new Error('Sessão inválida.');
        storage.setItem(KEY, JSON.stringify(safe));
        return safe;
      },
      read() {
        try {
          const safe = sanitize(JSON.parse(storage.getItem(KEY)));
          if (!safe) storage.removeItem(KEY);
          return safe;
        } catch {
          storage.removeItem(KEY);
          return null;
        }
      },
      clear() {
        storage.removeItem(KEY);
      }
    };
  }

  const api = { createSessionStore };
  if (root.sessionStorage) api.current = createSessionStore(root.sessionStorage);
  root.AppSession = api;
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
})(typeof window !== 'undefined' ? window : globalThis);
