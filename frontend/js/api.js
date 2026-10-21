(function initializeAlunoService(root) {
  const API_BASE_URL = 'http://localhost:8080/api';

  function createAlunoService({ request, authApi, session }) {
    function getAlunoLogado() {
      const atual = session.read();
      return atual && atual.perfil === 'ALUNO' ? atual : null;
    }

    function requireSession() {
      const atual = getAlunoLogado();
      if (!atual) throw new Error('Nenhum aluno autenticado.');
      return atual;
    }

    function saveProfile(aluno) {
      session.save({ ...aluno, perfil: 'ALUNO' });
      return aluno;
    }

    function json(method, dados) {
      return { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(dados) };
    }

    return {
      getInstituicoes: () => request(`${API_BASE_URL}/instituicoes`),
      async cadastrar(dados) {
        return saveProfile(await request(`${API_BASE_URL}/alunos`, json('POST', dados)));
      },
      async login(email, senha) {
        return session.save(await authApi.login(email, senha, 'ALUNO'));
      },
      getAlunoLogado,
      async getMeusDados() {
        const atual = requireSession();
        return request(`${API_BASE_URL}/alunos/${atual.id}`);
      },
      async atualizarMeusDados(dados) {
        const atual = requireSession();
        return saveProfile(await request(`${API_BASE_URL}/alunos/${atual.id}`, json('PUT', dados)));
      },
      async excluirMinhaConta() {
        const atual = requireSession();
        await request(`${API_BASE_URL}/alunos/${atual.id}`, { method: 'DELETE' });
        session.clear();
      },
      logout: () => session.clear()
    };
  }

  if (root.AppHttp && root.AuthApi && root.AppSession && root.AppSession.current) {
    root.alunoAuthService = createAlunoService({ request: root.AppHttp.request, authApi: root.AuthApi, session: root.AppSession.current });
  }
  if (typeof module !== 'undefined' && module.exports) module.exports = { createAlunoService };
})(typeof window !== 'undefined' ? window : globalThis);
