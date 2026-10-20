/**
 * SISTEMA DE MOEDA ESTUDANTIL - SERVIÇO DE TRANSFERÊNCIA ENTRE ALUNOS (RF-012 e RF-013)
 * Atende o aluno (solicitar/cancelar) e o professor avaliador (listar/aprovar/recusar).
 */

const TRANSFERENCIA_API_BASE_URL = 'http://localhost:8080/api';

class TransferenciaService {
  /* ---------- Aluno (RF-012) ---------- */

  async buscarSaldoDisponivel(alunoId) {
    return AppHttp.request(`${TRANSFERENCIA_API_BASE_URL}/alunos/${encodeURIComponent(alunoId)}/saldo-disponivel`);
  }

  async listarColegas(alunoId) {
    return AppHttp.request(`${TRANSFERENCIA_API_BASE_URL}/alunos/${encodeURIComponent(alunoId)}/colegas`);
  }

  async listarProfessoresComCriterios(alunoId) {
    return AppHttp.request(`${TRANSFERENCIA_API_BASE_URL}/alunos/${encodeURIComponent(alunoId)}/criterios-professores`);
  }

  async listarSolicitacoes(alunoId) {
    return AppHttp.request(`${TRANSFERENCIA_API_BASE_URL}/alunos/${encodeURIComponent(alunoId)}/transferencias`);
  }

  async solicitar(alunoId, { destinatarioId, avaliadorId, quantidade, justificativa }) {
    return AppHttp.request(`${TRANSFERENCIA_API_BASE_URL}/alunos/${encodeURIComponent(alunoId)}/transferencias`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ destinatarioId, avaliadorId, quantidade, justificativa })
      });
  }

  async cancelar(alunoId, solicitacaoId) {
    return AppHttp.request(
        `${TRANSFERENCIA_API_BASE_URL}/alunos/${encodeURIComponent(alunoId)}/transferencias/${encodeURIComponent(solicitacaoId)}`,
        { method: 'DELETE' }
    );
  }

  /* ---------- Professor (RF-013) ---------- */

  async listarParaProfessor(professorId, status) {
    const query = status ? `?status=${encodeURIComponent(status)}` : '';
    return AppHttp.request(`${TRANSFERENCIA_API_API_PROF(professorId)}${query}`);
  }

  async aprovar(professorId, solicitacaoId, observacao) {
    return AppHttp.request(`${TRANSFERENCIA_API_API_PROF(professorId)}/${encodeURIComponent(solicitacaoId)}/aprovar`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ observacao })
      });
  }

  async recusar(professorId, solicitacaoId, observacao) {
    return AppHttp.request(`${TRANSFERENCIA_API_API_PROF(professorId)}/${encodeURIComponent(solicitacaoId)}/recusar`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ observacao })
      });
  }
}

function TRANSFERENCIA_API_API_PROF(professorId) {
  return `${TRANSFERENCIA_API_BASE_URL}/professores/${encodeURIComponent(professorId)}/transferencias`;
}

window.transferenciaService = new TransferenciaService();
