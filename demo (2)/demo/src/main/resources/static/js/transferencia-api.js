/**
 * SISTEMA DE MOEDA ESTUDANTIL - SERVIÇO DE TRANSFERÊNCIA ENTRE ALUNOS (RF-012)
 * Consulta colegas e professores da mesma instituição, saldo disponível e gerencia solicitações.
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
}

window.transferenciaService = new TransferenciaService();
