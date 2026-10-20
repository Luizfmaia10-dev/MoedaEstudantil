/**
 * SISTEMA DE MOEDA ESTUDANTIL - SERVIÇO DE CRITÉRIOS DE TRANSFERÊNCIA (RF-011)
 * Professor edita o texto; aluno apenas consulta (somente leitura).
 */

const CRITERIOS_API_BASE_URL = 'http://localhost:8080/api';

class CriterioService {
  async buscarDoProfessor(professorId) {
    return AppHttp.request(`${CRITERIOS_API_BASE_URL}/professores/${encodeURIComponent(professorId)}/criterios`);
  }

  async salvar(professorId, criterios) {
    return AppHttp.request(`${CRITERIOS_API_BASE_URL}/professores/${encodeURIComponent(professorId)}/criterios`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ criterios })
    });
  }

  async listarParaAluno(alunoId) {
    return AppHttp.request(`${CRITERIOS_API_BASE_URL}/alunos/${encodeURIComponent(alunoId)}/criterios-professores`);
  }
}

window.criterioService = new CriterioService();
