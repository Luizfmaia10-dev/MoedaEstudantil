/**
 * SISTEMA DE MOEDA ESTUDANTIL - SERVIÇO DA EMPRESA PARCEIRA (AUTOCADASTRO & GERENCIAMENTO)
 * CRUD executado pela própria empresa parceira:
 * - Create: Empresa se cadastra no sistema (Nome, CNPJ, E-mail, Senha)
 * - Read: Empresa visualiza seus próprios dados cadastrais
 * - Update: Empresa edita seus dados
 * - Delete: Empresa exclui sua própria conta se desejar
 */

const API_EMPRESAS_URL = 'http://localhost:8080/api/empresas';
const STORAGE_EMPRESAS_KEY = 'moeda_estudantil_empresas_v1';
const STORAGE_EMPRESA_SESSION_KEY = 'moeda_estudantil_sessao_empresa_v1';

// Empresa de exemplo para teste rápido
const EMPRESA_PADRAO = [
  {
    id: 1,
    nome: 'Livraria Universitária Campus',
    cnpj: '12.345.678/0001-90',
    email: 'contato@livrariacampus.com.br',
    senha: '123'
  }
];

class EmpresaAuthService {
  constructor() {
    this.isBackendOnline = false;
    this.initStorage();
  }

  initStorage() {
    if (!localStorage.getItem(STORAGE_EMPRESAS_KEY)) {
      localStorage.setItem(STORAGE_EMPRESAS_KEY, JSON.stringify(EMPRESA_PADRAO));
    }
  }

  // Verifica se o backend Spring Boot está ativo
  async checkBackend() {
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 1200);
      const res = await fetch(API_EMPRESAS_URL, {
        method: 'GET',
        signal: controller.signal
      });
      clearTimeout(timeoutId);
      this.isBackendOnline = res.ok;
      return this.isBackendOnline;
    } catch {
      this.isBackendOnline = false;
      return false;
    }
  }

  // EMPRESA SE CADASTRA (Create)
  async cadastrar(dadosEmpresa) {
    if (this.isBackendOnline) {
      try {
        const res = await fetch(API_EMPRESAS_URL, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(dadosEmpresa)
        });
        if (res.ok) {
          const salva = await res.json();
          this.setSessao(salva);
          return salva;
        }
      } catch (err) {
        console.warn('Backend indisponível, usando armazenamento local:', err);
      }
    }

    // Armazenamento Local
    const raw = localStorage.getItem(STORAGE_EMPRESAS_KEY);
    const empresas = raw ? JSON.parse(raw) : [];

    const emailExiste = empresas.find(e => e.email.toLowerCase() === dadosEmpresa.email.toLowerCase());
    if (emailExiste) {
      throw new Error('Já existe uma empresa cadastrada com este e-mail.');
    }

    const cnpjExiste = empresas.find(e => e.cnpj === dadosEmpresa.cnpj);
    if (cnpjExiste) {
      throw new Error('Já existe uma empresa cadastrada com este CNPJ.');
    }

    const novaEmpresa = {
      ...dadosEmpresa,
      id: Date.now()
    };

    empresas.push(novaEmpresa);
    localStorage.setItem(STORAGE_EMPRESAS_KEY, JSON.stringify(empresas));
    this.setSessao(novaEmpresa);
    return novaEmpresa;
  }

  // LOGIN DA EMPRESA
  async login(email, senha) {
    const raw = localStorage.getItem(STORAGE_EMPRESAS_KEY);
    const empresas = raw ? JSON.parse(raw) : [];

    const empresa = empresas.find(
      e => e.email.toLowerCase() === email.toLowerCase() && e.senha === senha
    );

    if (!empresa) {
      throw new Error('E-mail ou senha incorretos.');
    }

    this.setSessao(empresa);
    return empresa;
  }

  // OBTER DADOS DA EMPRESA AUTENTICADA (Read)
  getEmpresaLogada() {
    const raw = sessionStorage.getItem(STORAGE_EMPRESA_SESSION_KEY);
    if (!raw) return null;
    const sessao = JSON.parse(raw);

    const rawEmpresas = localStorage.getItem(STORAGE_EMPRESAS_KEY);
    const empresas = rawEmpresas ? JSON.parse(rawEmpresas) : [];
    const atual = empresas.find(e => e.id === sessao.id);
    return atual || sessao;
  }

  // EDITAR DADOS DA EMPRESA (Update)
  async atualizarMeusDados(dadosAtualizados) {
    const empresaAtual = this.getEmpresaLogada();
    if (!empresaAtual) throw new Error('Nenhuma empresa autenticada.');

    const id = empresaAtual.id;

    if (this.isBackendOnline) {
      try {
        const res = await fetch(`${API_EMPRESAS_URL}/${id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(dadosAtualizados)
        });
        if (res.ok) {
          const atualizada = await res.json();
          this.setSessao(atualizada);
          return atualizada;
        }
      } catch (err) {
        console.warn('Falha na atualização remota:', err);
      }
    }

    // Atualização Local
    const raw = localStorage.getItem(STORAGE_EMPRESAS_KEY);
    const empresas = raw ? JSON.parse(raw) : [];
    const index = empresas.findIndex(e => e.id === id);

    if (index === -1) throw new Error('Empresa não encontrada.');

    const empresaFinal = {
      ...empresas[index],
      ...dadosAtualizados,
      id: id,
      senha: dadosAtualizados.senha ? dadosAtualizados.senha : empresas[index].senha
    };

    empresas[index] = empresaFinal;
    localStorage.setItem(STORAGE_EMPRESAS_KEY, JSON.stringify(empresas));
    this.setSessao(empresaFinal);
    return empresaFinal;
  }

  // EXCLUIR CONTA DA EMPRESA (Delete)
  async excluirMinhaConta() {
    const empresaAtual = this.getEmpresaLogada();
    if (!empresaAtual) throw new Error('Nenhuma empresa autenticada.');

    const id = empresaAtual.id;

    if (this.isBackendOnline) {
      try {
        await fetch(`${API_EMPRESAS_URL}/${id}`, { method: 'DELETE' });
      } catch (err) {
        console.warn('Falha na exclusão remota:', err);
      }
    }

    // Exclusão Local
    const raw = localStorage.getItem(STORAGE_EMPRESAS_KEY);
    const empresas = raw ? JSON.parse(raw) : [];
    const filtradas = empresas.filter(e => e.id !== id);
    localStorage.setItem(STORAGE_EMPRESAS_KEY, JSON.stringify(filtradas));

    this.logout();
    return true;
  }

  setSessao(empresa) {
    sessionStorage.setItem(STORAGE_EMPRESA_SESSION_KEY, JSON.stringify(empresa));
  }

  logout() {
    sessionStorage.removeItem(STORAGE_EMPRESA_SESSION_KEY);
  }
}

window.empresaAuthService = new EmpresaAuthService();
