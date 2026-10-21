(function initializeEmpresaService(root) {
  const API_EMPRESAS_URL = 'http://localhost:8080/api/empresas';

  function createEmpresaService({ request, authApi, session }) {
    function getEmpresaLogada() {
      const atual = session.read();
      return atual && atual.perfil === 'EMPRESA' ? atual : null;
    }

    function requireSession() {
      const atual = getEmpresaLogada();
      if (!atual) throw new Error('Nenhuma empresa autenticada.');
      return atual;
    }

    function saveProfile(empresa) {
      session.save({ ...empresa, perfil: 'EMPRESA' });
      return empresa;
    }

    function json(method, dados) {
      return { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(dados) };
    }

    return {
      async cadastrar(dados) {
        return saveProfile(await request(API_EMPRESAS_URL, json('POST', dados)));
      },
      async login(email, senha) {
        return session.save(await authApi.login(email, senha, 'EMPRESA'));
      },
      getEmpresaLogada,
      async getMeusDados() {
        const atual = requireSession();
        return request(`${API_EMPRESAS_URL}/${atual.id}`);
      },
      async atualizarMeusDados(dados) {
        const atual = requireSession();
        return saveProfile(await request(`${API_EMPRESAS_URL}/${atual.id}`, json('PUT', dados)));
      },
      async excluirMinhaConta() {
        const atual = requireSession();
        await request(`${API_EMPRESAS_URL}/${atual.id}`, { method: 'DELETE' });
        session.clear();
      },
      logout: () => session.clear()
    };
  }

  if (root.AppHttp && root.AuthApi && root.AppSession && root.AppSession.current) {
    root.empresaAuthService = createEmpresaService({ request: root.AppHttp.request, authApi: root.AuthApi, session: root.AppSession.current });
  }
  if (typeof module !== 'undefined' && module.exports) module.exports = { createEmpresaService };
})(typeof window !== 'undefined' ? window : globalThis);
