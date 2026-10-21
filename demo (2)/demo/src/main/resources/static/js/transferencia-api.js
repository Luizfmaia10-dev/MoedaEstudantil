(function initializeTransferenciaApi(root, factory) {
  'use strict';

  const exports = factory();
  if (typeof module !== 'undefined' && module.exports) module.exports = exports;
  if (root?.AppHttp?.request) {
    root.transferenciaService = exports.createTransferenciaService(root.AppHttp.request);
  }
})(typeof window !== 'undefined' ? window : globalThis, function transferenciaApiFactory() {
  const API_BASE_URL = 'http://localhost:8080/api';

  function createTransferenciaService(request, baseUrl = API_BASE_URL) {
    const professorUrl = professorId =>
      `${baseUrl}/professores/${encodeURIComponent(professorId)}/transferencias`;

    return {
      buscarSaldoDisponivel(alunoId) {
        return request(`${baseUrl}/alunos/${encodeURIComponent(alunoId)}/saldo-disponivel`);
      },

      listarColegas(alunoId) {
        return request(`${baseUrl}/alunos/${encodeURIComponent(alunoId)}/colegas`);
      },

      listarProfessoresComCriterios(alunoId) {
        return request(`${baseUrl}/alunos/${encodeURIComponent(alunoId)}/criterios-professores`);
      },

      listarSolicitacoes(alunoId) {
        return request(`${baseUrl}/alunos/${encodeURIComponent(alunoId)}/transferencias`);
      },

      interpretar(alunoId, mensagem, rascunho) {
        return request(`${baseUrl}/alunos/${encodeURIComponent(alunoId)}/transferencias/assistente/interpretar`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ mensagem, rascunho })
        });
      },

      solicitar(alunoId, { destinatarioId, avaliadorId, quantidade, justificativa }) {
        return request(`${baseUrl}/alunos/${encodeURIComponent(alunoId)}/transferencias`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ destinatarioId, avaliadorId, quantidade, justificativa })
        });
      },

      cancelar(alunoId, solicitacaoId) {
        return request(
          `${baseUrl}/alunos/${encodeURIComponent(alunoId)}/transferencias/${encodeURIComponent(solicitacaoId)}`,
          { method: 'DELETE' }
        );
      },

      listarParaProfessor(professorId, status) {
        const query = status ? `?status=${encodeURIComponent(status)}` : '';
        return request(`${professorUrl(professorId)}${query}`);
      },

      aprovar(professorId, solicitacaoId, observacao) {
        return request(`${professorUrl(professorId)}/${encodeURIComponent(solicitacaoId)}/aprovar`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ observacao })
        });
      },

      recusar(professorId, solicitacaoId, observacao) {
        return request(`${professorUrl(professorId)}/${encodeURIComponent(solicitacaoId)}/recusar`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ observacao })
        });
      }
    };
  }

  return { API_BASE_URL, createTransferenciaService };
});
