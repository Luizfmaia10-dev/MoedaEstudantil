/**
 * SISTEMA DE MOEDA ESTUDANTIL - SERVIÇO DE CRITÉRIOS DE TRANSFERÊNCIA (RF-011)
 * Professor edita o texto; aluno apenas consulta (somente leitura).
 */

const CRITERIOS_API_BASE_URL = 'http://localhost:8080/api';

class CriterioService {
  async _tratar(res) {
    if (res.ok) return res;
    let msg = 'Não foi possível concluir a operação.';
    try {
      const corpo = await res.json();
      if (corpo && corpo.message) msg = corpo.message;
    } catch { /* corpo sem JSON */ }
    throw new Error(msg);
  }

  async buscarDoProfessor(professorId) {
    const res = await this._tratar(await fetch(`${CRITERIOS_API_BASE_URL}/professores/${encodeURIComponent(professorId)}/criterios`));
    return res.json();
  }

  async salvar(professorId, criterios) {
    const res = await this._tratar(await fetch(`${CRITERIOS_API_BASE_URL}/professores/${encodeURIComponent(professorId)}/criterios`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ criterios })
    }));
    return res.json();
  }

  async listarParaAluno(alunoId) {
    const res = await this._tratar(await fetch(`${CRITERIOS_API_BASE_URL}/alunos/${encodeURIComponent(alunoId)}/criterios-professores`));
    return res.json();
  }
}

window.criterioService = new CriterioService();
