/**
 * SISTEMA DE MOEDA ESTUDANTIL - SERVIÇO DO ALUNO (AUTOCADASTRO & GERENCIAMENTO)
 * CRUD executado pelo próprio aluno:
 * - Create: Aluno se cadastra no sistema
 * - Read: Aluno visualiza seus próprios dados
 * - Update: Aluno edita suas informações cadastrais
 * - Delete: Aluno exclui sua própria conta se desejar
 */

const API_BASE_URL = 'http://localhost:8080/api';
const STORAGE_ALUNOS_KEY = 'moeda_estudantil_alunos_v2';
const STORAGE_SESSION_KEY = 'moeda_estudantil_sessao_aluno_v2';
const STORAGE_INST_KEY = 'moeda_estudantil_instituicoes_v2';

// Instituições de ensino pré-cadastradas (conforme requisito do documento)
const INSTITUICOES_PADRAO = [
  { id: 1, nome: 'PUC Minas' },
  { id: 2, nome: 'Universidade Federal de Minas Gerais (UFMG)' },
  { id: 3, nome: 'Centro Federal de Educação Tecnológica (CEFET-MG)' },
  { id: 4, nome: 'Universidade do Estado de Minas Gerais (UEMG)' }
];

// Aluno inicial para teste imediato de login
const ALUNO_INICIAL = [
  {
    id: 1,
    nome: 'Caio César Santos',
    email: 'caio.santos@sga.pucminas.br',
    senha: '123',
    cpf: '123.456.789-01',
    rg: 'MG-19.823.456',
    curso: 'Engenharia de Software',
    instituicaoId: 1,
    instituicaoNome: 'PUC Minas',
    endereco: 'Av. Dom José Gaspar, 500 - Coração Eucarístico, Belo Horizonte - MG'
  }
];

class AlunoAuthService {
  constructor() {
    this.isBackendOnline = false;
    this.initStorage();
  }

  initStorage() {
    if (!localStorage.getItem(STORAGE_ALUNOS_KEY)) {
      localStorage.setItem(STORAGE_ALUNOS_KEY, JSON.stringify(ALUNO_INICIAL));
    }
    if (!localStorage.getItem(STORAGE_INST_KEY)) {
      localStorage.setItem(STORAGE_INST_KEY, JSON.stringify(INSTITUICOES_PADRAO));
    }
  }

  // Retorna instituições pré-cadastradas para seleção no cadastro
  getInstituicoes() {
    const raw = localStorage.getItem(STORAGE_INST_KEY);
    return raw ? JSON.parse(raw) : INSTITUICOES_PADRAO;
  }

  // Verifica se o backend Spring Boot está ativo
  async checkBackend() {
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 1200);
      const res = await fetch(`${API_BASE_URL}/instituicoes`, {
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

  // ALUNO SE CADASTRA (Create)
  async cadastrar(dadosAluno) {
    const instituicoes = this.getInstituicoes();
    const inst = instituicoes.find(i => i.id === Number(dadosAluno.instituicaoId));

    if (this.isBackendOnline) {
      try {
        const res = await fetch(`${API_BASE_URL}/alunos`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(dadosAluno)
        });
        if (res.ok) {
          const alunoCriado = await res.json();
          this.setSessao(alunoCriado);
          return alunoCriado;
        }
      } catch (err) {
        console.warn('Falha na comunicação com backend, usando armazenamento local:', err);
      }
    }

    // Armazenamento local
    const raw = localStorage.getItem(STORAGE_ALUNOS_KEY);
    const alunos = raw ? JSON.parse(raw) : [];

    // Validar se o e-mail já está cadastrado
    const emailExistente = alunos.find(a => a.email.toLowerCase() === dadosAluno.email.toLowerCase());
    if (emailExistente) {
      throw new Error('Já existe um aluno cadastrado com este e-mail.');
    }

    const novoAluno = {
      ...dadosAluno,
      id: Date.now(),
      instituicaoId: Number(dadosAluno.instituicaoId),
      instituicaoNome: inst ? inst.nome : 'Instituição de Ensino'
    };

    alunos.push(novoAluno);
    localStorage.setItem(STORAGE_ALUNOS_KEY, JSON.stringify(alunos));
    this.setSessao(novoAluno);
    return novoAluno;
  }

  // LOGIN DO ALUNO
  async login(email, senha) {
    const raw = localStorage.getItem(STORAGE_ALUNOS_KEY);
    const alunos = raw ? JSON.parse(raw) : [];

    const aluno = alunos.find(
      a => a.email.toLowerCase() === email.toLowerCase() && a.senha === senha
    );

    if (!aluno) {
      throw new Error('E-mail ou senha incorretos.');
    }

    this.setSessao(aluno);
    return aluno;
  }

  // ALUNO OBTÉM SEUS PRÓPRIOS DADOS (Read)
  getAlunoLogado() {
    const raw = sessionStorage.getItem(STORAGE_SESSION_KEY);
    if (!raw) return null;
    const sessao = JSON.parse(raw);

    // Atualiza os dados mais recentes do localStorage
    const rawAlunos = localStorage.getItem(STORAGE_ALUNOS_KEY);
    const alunos = rawAlunos ? JSON.parse(rawAlunos) : [];
    const atual = alunos.find(a => a.id === sessao.id);
    return atual || sessao;
  }

  // ALUNO EDITA SEUS DADOS (Update)
  async atualizarMeusDados(dadosAtualizados) {
    const alunoAtual = this.getAlunoLogado();
    if (!alunoAtual) throw new Error('Nenhum aluno autenticado.');

    const id = alunoAtual.id;
    const instituicoes = this.getInstituicoes();
    const inst = instituicoes.find(i => i.id === Number(dadosAtualizados.instituicaoId));

    if (this.isBackendOnline) {
      try {
        const res = await fetch(`${API_BASE_URL}/alunos/${id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(dadosAtualizados)
        });
        if (res.ok) {
          const atualizado = await res.json();
          this.setSessao(atualizado);
          return atualizado;
        }
      } catch (err) {
        console.warn('Falha ao atualizar no backend:', err);
      }
    }

    // Atualização local
    const raw = localStorage.getItem(STORAGE_ALUNOS_KEY);
    const alunos = raw ? JSON.parse(raw) : [];
    const index = alunos.findIndex(a => a.id === id);

    if (index === -1) throw new Error('Aluno não encontrado.');

    const alunoFinal = {
      ...alunos[index],
      ...dadosAtualizados,
      id: id,
      instituicaoId: Number(dadosAtualizados.instituicaoId),
      instituicaoNome: inst ? inst.nome : alunos[index].instituicaoNome,
      senha: dadosAtualizados.senha ? dadosAtualizados.senha : alunos[index].senha
    };

    alunos[index] = alunoFinal;
    localStorage.setItem(STORAGE_ALUNOS_KEY, JSON.stringify(alunos));
    this.setSessao(alunoFinal);
    return alunoFinal;
  }

  // ALUNO EXCLUI SUA PRÓPRIA CONTA (Delete)
  async excluirMinhaConta() {
    const alunoAtual = this.getAlunoLogado();
    if (!alunoAtual) throw new Error('Nenhum aluno autenticado.');

    const id = alunoAtual.id;

    if (this.isBackendOnline) {
      try {
        await fetch(`${API_BASE_URL}/alunos/${id}`, { method: 'DELETE' });
      } catch (err) {
        console.warn('Falha ao excluir no backend:', err);
      }
    }

    // Exclusão local
    const raw = localStorage.getItem(STORAGE_ALUNOS_KEY);
    const alunos = raw ? JSON.parse(raw) : [];
    const filtrados = alunos.filter(a => a.id !== id);
    localStorage.setItem(STORAGE_ALUNOS_KEY, JSON.stringify(filtrados));

    this.logout();
    return true;
  }

  setSessao(aluno) {
    sessionStorage.setItem(STORAGE_SESSION_KEY, JSON.stringify(aluno));
  }

  logout() {
    sessionStorage.removeItem(STORAGE_SESSION_KEY);
  }
}

window.alunoAuthService = new AlunoAuthService();
