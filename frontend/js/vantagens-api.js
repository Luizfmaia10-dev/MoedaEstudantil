/**
 * SISTEMA DE MOEDA ESTUDANTIL - SERVIÇO DE VANTAGENS (RF-008)
 * CRUD de vantagens da empresa parceira, com upload da foto do produto (multipart).
 */

const VANTAGENS_API_ORIGIN = 'http://localhost:8080';
const VANTAGENS_API_BASE_URL = `${VANTAGENS_API_ORIGIN}/api/empresas`;

class VantagemService {
  async _request(url, options) {
    try {
      return await AppHttp.request(url, options);
    } catch (error) {
      if (error.status === 404 && error.message === 'Não foi possível concluir a operação.') {
        throw new Error('Registro não encontrado.');
      }
      if (error.status === 413) throw new Error('A imagem excede o tamanho máximo de 5 MB.');
      throw error;
    }
  }

  _formData({ nome, descricao, custoMoedas, foto }) {
    const fd = new FormData();
    fd.append('nome', nome);
    fd.append('descricao', descricao);
    fd.append('custoMoedas', custoMoedas);
    if (foto) fd.append('foto', foto);
    return fd;
  }

  // URLs relativas (/uploads/...) são servidas pelo backend
  urlFoto(fotoUrl) {
    if (!fotoUrl) return null;
    return fotoUrl.startsWith('http') ? fotoUrl : `${VANTAGENS_API_ORIGIN}${fotoUrl}`;
  }

  async listar(empresaId) {
    return this._request(`${VANTAGENS_API_BASE_URL}/${empresaId}/vantagens`);
  }

  async criar(empresaId, dados) {
    return this._request(`${VANTAGENS_API_BASE_URL}/${empresaId}/vantagens`, {
      method: 'POST',
      body: this._formData(dados)
    });
  }

  async atualizar(empresaId, id, dados) {
    return this._request(`${VANTAGENS_API_BASE_URL}/${empresaId}/vantagens/${id}`, {
      method: 'PUT',
      body: this._formData(dados)
    });
  }

  async remover(empresaId, id) {
    await this._request(`${VANTAGENS_API_BASE_URL}/${empresaId}/vantagens/${id}`, { method: 'DELETE' });
  }
}

window.vantagemService = new VantagemService();
