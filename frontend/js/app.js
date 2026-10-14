/**
 * SISTEMA DE MOEDA ESTUDANTIL - APLICAÇÃO DO ALUNO
 * Controla os fluxos do próprio aluno:
 * 1. Cadastro (Create)
 * 2. Login & Visualização dos próprios dados (Read)
 * 3. Edição dos próprios dados (Update)
 * 4. Exclusão da própria conta (Delete)
 */

document.addEventListener('DOMContentLoaded', () => {
  // Seções Principais
  const authSection = document.getElementById('authSection');
  const profileSection = document.getElementById('profileSection');
  const userNavActions = document.getElementById('userNavActions');
  const headerStudentName = document.getElementById('headerStudentName');
  const btnLogout = document.getElementById('btnLogout');

  // Abas de Autenticação
  const tabCadastroBtn = document.getElementById('tabCadastroBtn');
  const tabLoginBtn = document.getElementById('tabLoginBtn');
  const cadastroContainer = document.getElementById('cadastroContainer');
  const loginContainer = document.getElementById('loginContainer');

  // Formulário de Cadastro (Create)
  const cadastroForm = document.getElementById('cadastroForm');
  const cadInstituicao = document.getElementById('cadInstituicao');
  const cadCpf = document.getElementById('cadCpf');

  // Formulário de Login
  const loginForm = document.getElementById('loginForm');
  const loginEmail = document.getElementById('loginEmail');
  const loginSenha = document.getElementById('loginSenha');
  const btnQuickLogin = document.getElementById('btnQuickLogin');

  // Cartão de Visualização (Read)
  const viewProfileCard = document.getElementById('viewProfileCard');
  const viewNome = document.getElementById('viewNome');
  const viewEmail = document.getElementById('viewEmail');
  const viewInstituicao = document.getElementById('viewInstituicao');
  const viewCurso = document.getElementById('viewCurso');
  const viewCpf = document.getElementById('viewCpf');
  const viewRg = document.getElementById('viewRg');
  const viewEndereco = document.getElementById('viewEndereco');
  const btnOpenEdit = document.getElementById('btnOpenEdit');

  // Cartão de Edição (Update)
  const editProfileCard = document.getElementById('editProfileCard');
  const editForm = document.getElementById('editForm');
  const editNome = document.getElementById('editNome');
  const editEmail = document.getElementById('editEmail');
  const editSenha = document.getElementById('editSenha');
  const editCpf = document.getElementById('editCpf');
  const editRg = document.getElementById('editRg');
  const editInstituicao = document.getElementById('editInstituicao');
  const editCurso = document.getElementById('editCurso');
  const editEndereco = document.getElementById('editEndereco');
  const btnCancelEdit = document.getElementById('btnCancelEdit');
  const btnCancelEditBottom = document.getElementById('btnCancelEditBottom');

  // Modal de Exclusão (Delete)
  const deleteModal = document.getElementById('deleteModal');
  const btnOpenDeleteModal = document.getElementById('btnOpenDeleteModal');
  const btnCloseDeleteModal = document.getElementById('btnCloseDeleteModal');
  const btnCancelDelete = document.getElementById('btnCancelDelete');
  const btnConfirmDelete = document.getElementById('btnConfirmDelete');

  // Inicialização
  function init() {
    populateInstituicoes();
    setupCpfMask(cadCpf);
    setupCpfMask(editCpf);
    setupEvents();
    checkAuthStatus();
  }

  // Popula os selects com as instituições pré-cadastradas
  function populateInstituicoes() {
    const insts = window.alunoAuthService.getInstituicoes();

    cadInstituicao.innerHTML = '<option value="" disabled selected>Selecione sua instituição...</option>';
    editInstituicao.innerHTML = '<option value="" disabled selected>Selecione sua instituição...</option>';

    insts.forEach(inst => {
      const opt1 = document.createElement('option');
      opt1.value = inst.id;
      opt1.textContent = inst.nome;
      cadInstituicao.appendChild(opt1);

      const opt2 = document.createElement('option');
      opt2.value = inst.id;
      opt2.textContent = inst.nome;
      editInstituicao.appendChild(opt2);
    });
  }

  // Verifica se o aluno já está logado
  function checkAuthStatus() {
    const aluno = window.alunoAuthService.getAlunoLogado();
    if (aluno) {
      renderProfileView(aluno);
    } else {
      renderAuthView();
    }
  }

  // Exibe a tela de login/cadastro
  function renderAuthView() {
    authSection.style.display = 'flex';
    profileSection.style.display = 'none';
    userNavActions.style.display = 'none';
  }

  // Exibe a tela com os dados do próprio aluno
  function renderProfileView(aluno) {
    authSection.style.display = 'none';
    profileSection.style.display = 'flex';
    userNavActions.style.display = 'flex';

    // Atualiza nome no cabeçalho
    headerStudentName.textContent = aluno.nome ? aluno.nome.split(' ')[0] : 'Aluno';

    // Preenche dados do perfil
    viewNome.textContent = aluno.nome || '-';
    viewEmail.textContent = aluno.email || '-';
    viewInstituicao.textContent = aluno.instituicaoNome || 'Instituição';
    viewCurso.textContent = aluno.curso || '-';
    viewCpf.textContent = aluno.cpf || '-';
    viewRg.textContent = aluno.rg || '-';
    viewEndereco.textContent = aluno.endereco || '-';

    // Garante que está no modo de visualização
    viewProfileCard.style.display = 'block';
    editProfileCard.style.display = 'none';
  }

  // Configuração de Eventos
  function setupEvents() {
    // Alternar entre abas de Cadastro e Login
    tabCadastroBtn.addEventListener('click', () => {
      tabCadastroBtn.classList.add('active');
      tabLoginBtn.classList.remove('active');
      cadastroContainer.style.display = 'block';
      loginContainer.style.display = 'none';
    });

    tabLoginBtn.addEventListener('click', () => {
      tabLoginBtn.classList.add('active');
      tabCadastroBtn.classList.remove('active');
      cadastroContainer.style.display = 'none';
      loginContainer.style.display = 'block';
    });

    // 1. SUBMIT CADASTRO (CREATE)
    cadastroForm.addEventListener('submit', async (e) => {
      e.preventDefault();

      const dados = {
        nome: document.getElementById('cadNome').value.trim(),
        email: document.getElementById('cadEmail').value.trim(),
        senha: document.getElementById('cadSenha').value,
        cpf: document.getElementById('cadCpf').value.trim(),
        rg: document.getElementById('cadRg').value.trim(),
        instituicaoId: document.getElementById('cadInstituicao').value,
        curso: document.getElementById('cadCurso').value.trim(),
        endereco: document.getElementById('cadEndereco').value.trim()
      };

      try {
        const novoAluno = await window.alunoAuthService.cadastrar(dados);
        showToast('Cadastro realizado com sucesso!', 'success');
        cadastroForm.reset();
        renderProfileView(novoAluno);
      } catch (err) {
        showToast(err.message || 'Erro ao realizar cadastro.', 'danger');
      }
    });

    // 2. SUBMIT LOGIN
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = loginEmail.value.trim();
      const senha = loginSenha.value;

      try {
        const aluno = await window.alunoAuthService.login(email, senha);
        showToast('Login realizado com sucesso!', 'success');
        loginForm.reset();
        renderProfileView(aluno);
      } catch (err) {
        showToast(err.message || 'E-mail ou senha incorretos.', 'danger');
      }
    });

    // Teste Rápido de Login
    if (btnQuickLogin) {
      btnQuickLogin.addEventListener('click', async () => {
        try {
          const aluno = await window.alunoAuthService.login('caio.santos@sga.pucminas.br', '123');
          showToast('Logado com conta de teste!', 'success');
          renderProfileView(aluno);
        } catch (err) {
          showToast('Erro no login de teste.', 'danger');
        }
      });
    }

    // 3. EDITAR MEUS DADOS (UPDATE)
    btnOpenEdit.addEventListener('click', () => {
      const aluno = window.alunoAuthService.getAlunoLogado();
      if (!aluno) return;

      editNome.value = aluno.nome || '';
      editEmail.value = aluno.email || '';
      editSenha.value = '';
      editCpf.value = aluno.cpf || '';
      editRg.value = aluno.rg || '';
      editInstituicao.value = aluno.instituicaoId || '';
      editCurso.value = aluno.curso || '';
      editEndereco.value = aluno.endereco || '';

      viewProfileCard.style.display = 'none';
      editProfileCard.style.display = 'block';
    });

    const cancelarEdicao = () => {
      editProfileCard.style.display = 'none';
      viewProfileCard.style.display = 'block';
    };

    btnCancelEdit.addEventListener('click', cancelarEdicao);
    btnCancelEditBottom.addEventListener('click', cancelarEdicao);

    editForm.addEventListener('submit', async (e) => {
      e.preventDefault();

      const dadosAtualizados = {
        nome: editNome.value.trim(),
        email: editEmail.value.trim(),
        cpf: editCpf.value.trim(),
        rg: editRg.value.trim(),
        instituicaoId: editInstituicao.value,
        curso: editCurso.value.trim(),
        endereco: editEndereco.value.trim()
      };

      if (editSenha.value) {
        dadosAtualizados.senha = editSenha.value;
      }

      try {
        const atualizado = await window.alunoAuthService.atualizarMeusDados(dadosAtualizados);
        showToast('Seus dados foram atualizados com sucesso!', 'success');
        renderProfileView(atualizado);
      } catch (err) {
        showToast('Erro ao atualizar dados.', 'danger');
      }
    });

    // 4. EXCLUIR MINHA CONTA (DELETE)
    btnOpenDeleteModal.addEventListener('click', () => {
      deleteModal.classList.add('active');
    });

    const fecharModalExclusao = () => {
      deleteModal.classList.remove('active');
    };

    btnCloseDeleteModal.addEventListener('click', fecharModalExclusao);
    btnCancelDelete.addEventListener('click', fecharModalExclusao);

    btnConfirmDelete.addEventListener('click', async () => {
      try {
        await window.alunoAuthService.excluirMinhaConta();
        fecharModalExclusao();
        showToast('Sua conta foi excluída com sucesso.', 'success');
        renderAuthView();
      } catch (err) {
        showToast('Erro ao excluir conta.', 'danger');
      }
    });

    // Fechar modal ao clicar fora
    deleteModal.addEventListener('click', (e) => {
      if (e.target === deleteModal) fecharModalExclusao();
    });

    // LOGOUT
    btnLogout.addEventListener('click', () => {
      window.alunoAuthService.logout();
      showToast('Sessão encerrada.', 'success');
      renderAuthView();
    });
  }

  // Máscara Simples de CPF (000.000.000-00)
  function setupCpfMask(input) {
    if (!input) return;
    input.addEventListener('input', (e) => {
      let v = e.target.value.replace(/\D/g, '');
      if (v.length > 11) v = v.slice(0, 11);
      if (v.length > 9) {
        v = v.replace(/(\d{3})(\d{3})(\d{3})(\d{1,2})/, '$1.$2.$3-$4');
      } else if (v.length > 6) {
        v = v.replace(/(\d{3})(\d{3})(\d{1,3})/, '$1.$2.$3');
      } else if (v.length > 3) {
        v = v.replace(/(\d{3})(\d{1,3})/, '$1.$2');
      }
      e.target.value = v;
    });
  }

  // Notificações Toast
  function showToast(mensagem, tipo = 'success') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast ${tipo}`;
    toast.textContent = mensagem;

    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transition = 'opacity 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }, 3000);
  }

  init();
});
