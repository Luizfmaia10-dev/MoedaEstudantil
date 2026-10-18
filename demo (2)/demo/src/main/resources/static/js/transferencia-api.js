/**
 * SISTEMA DE MOEDA ESTUDANTIL - SERVIÇO DE TRANSFERÊNCIA ENTRE ALUNOS (RF-012 e RF-013)
 * Atende o aluno (solicitar/cancelar) e o professor avaliador (listar/aprovar/recusar).
 */

const TRANSFERENCIA_API_BASE_URL = 'http://localhost:8080/api';

class TransferenciaService {
  async _tratar(res) {
    if (res.ok) return res;
    let msg = 'Não foi possível concluir a operação.';
    try {
      const corpo = await res.json();
      if (corpo && corpo.message) msg = corpo.message;
    } catch { /* corpo sem JSON */ }
    throw new Error(msg);
  }

  /* ---------- Aluno (RF-012) ---------- */

  async buscarSaldoDisponivel(alunoId) {
    const res = await this._tratar(
      await fetch(`${TRANSFERENCIA_API_BASE_URL}/alunos/${encodeURIComponent(alunoId)}/saldo-disponivel`)
    );
    return res.json();
  }

  async listarColegas(alunoId) {
    const res = await this._tratar(
      await fetch(`${TRANSFERENCIA_API_BASE_URL}/alunos/${encodeURIComponent(alunoId)}/colegas`)
    );
    return res.json();
  }

  async listarProfessoresComCriterios(alunoId) {
    const res = await this._tratar(
      await fetch(`${TRANSFERENCIA_API_BASE_URL}/alunos/${encodeURIComponent(alunoId)}/criterios-professores`)
    );
    return res.json();
  }

  async listarSolicitacoes(alunoId) {
    const res = await this._tratar(
      await fetch(`${TRANSFERENCIA_API_BASE_URL}/alunos/${encodeURIComponent(alunoId)}/transferencias`)
    );
    return res.json();
  }

  async solicitar(alunoId, { destinatarioId, avaliadorId, quantidade, justificativa }) {
    const res = await this._tratar(
      await fetch(`${TRANSFERENCIA_API_BASE_URL}/alunos/${encodeURIComponent(alunoId)}/transferencias`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ destinatarioId, avaliadorId, quantidade, justificativa })
      })
    );
    return res.json();
  }

  async cancelar(alunoId, solicitacaoId) {
    const res = await this._tratar(
      await fetch(
        `${TRANSFERENCIA_API_BASE_URL}/alunos/${encodeURIComponent(alunoId)}/transferencias/${encodeURIComponent(solicitacaoId)}`,
        { method: 'DELETE' }
      )
    );
    return res.json();
  }

  /* ---------- Professor (RF-013) ---------- */

  async listarParaProfessor(professorId, status) {
    const query = status ? `?status=${encodeURIComponent(status)}` : '';
    const res = await this._tratar(
      await fetch(`${TRANSFERENCIA_API_API_PROF(professorId)}${query}`)
    );
    return res.json();
  }

  async aprovar(professorId, solicitacaoId, observacao) {
    const res = await this._tratar(
      await fetch(`${TRANSFERENCIA_API_API_PROF(professorId)}/${encodeURIComponent(solicitacaoId)}/aprovar`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ observacao })
      })
    );
    return res.json();
  }

  async recusar(professorId, solicitacaoId, observacao) {
    const res = await this._tratar(
      await fetch(`${TRANSFERENCIA_API_API_PROF(professorId)}/${encodeURIComponent(solicitacaoId)}/recusar`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ observacao })
      })
    );
    return res.json();
  }
}

function TRANSFERENCIA_API_API_PROF(professorId) {
  return `${TRANSFERENCIA_API_BASE_URL}/professores/${encodeURIComponent(professorId)}/transferencias`;
}

window.transferenciaService = new TransferenciaService();
