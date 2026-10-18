/**
 * SISTEMA DE MOEDA ESTUDANTIL - SERVIÇO DE VANTAGENS (RF-008)
 * CRUD de vantagens da empresa parceira, com upload da foto do produto (multipart).
 */

const VANTAGENS_API_ORIGIN = 'http://localhost:8080';
const VANTAGENS_API_BASE_URL = `${VANTAGENS_API_ORIGIN}/api/empresas`;

class VantagemService {
  async _tratar(res) {
    if (res.ok) return res;
    let msg = 'Não foi possível concluir a operação.';
    try {
      const corpo = await res.json();
      if (corpo && corpo.message) msg = corpo.message;
    } catch { /* corpo sem JSON */ }
    if (res.status === 404 && msg === 'Não foi possível concluir a operação.') msg = 'Registro não encontrado.';
    if (res.status === 413) msg = 'A imagem excede o tamanho máximo de 5 MB.';
    throw new Error(msg);
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
    const res = await this._tratar(await fetch(`${VANTAGENS_API_BASE_URL}/${empresaId}/vantagens`));
    return res.json();
  }

  async criar(empresaId, dados) {
    const res = await this._tratar(await fetch(`${VANTAGENS_API_BASE_URL}/${empresaId}/vantagens`, {
      method: 'POST',
      body: this._formData(dados)
    }));
    return res.json();
  }

  async atualizar(empresaId, id, dados) {
    const res = await this._tratar(await fetch(`${VANTAGENS_API_BASE_URL}/${empresaId}/vantagens/${id}`, {
      method: 'PUT',
      body: this._formData(dados)
    }));
    return res.json();
  }

  async remover(empresaId, id) {
    await this._tratar(await fetch(`${VANTAGENS_API_BASE_URL}/${empresaId}/vantagens/${id}`, { method: 'DELETE' }));
  }
}

window.vantagemService = new VantagemService();
