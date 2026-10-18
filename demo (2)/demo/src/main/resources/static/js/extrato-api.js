/**
 * SISTEMA DE MOEDA ESTUDANTIL - SERVIÇO DE EXTRATO (RF-006)
 * Consulta o extrato da conta de um aluno ou professor no backend.
 */

const EXTRATO_API_BASE_URL = 'http://localhost:8080/api';

class ExtratoService {
  async buscarExtrato(usuarioId, categoria) {
    const query = categoria ? `?categoria=${encodeURIComponent(categoria)}` : '';
    const res = await fetch(`${EXTRATO_API_BASE_URL}/contas/${encodeURIComponent(usuarioId)}/extrato${query}`);

    if (res.status === 404) {
      throw new Error('Conta não encontrada para este usuário.');
    }
    if (!res.ok) {
      throw new Error('Não foi possível carregar o extrato.');
    }
    return res.json();
  }
}

window.extratoService = new ExtratoService();
