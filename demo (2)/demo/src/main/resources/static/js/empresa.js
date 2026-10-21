/**
 * SISTEMA DE MOEDA ESTUDANTIL - APLICAÇÃO DA EMPRESA PARCEIRA
 * Controla os fluxos do próprio parceiro:
 * 1. Cadastro da Empresa (Create)
 * 2. Login & Visualização dos próprios dados (Read)
 * 3. Edição dos próprios dados (Update)
 * 4. Exclusão da própria conta (Delete)
 */

document.addEventListener('DOMContentLoaded', async () => {
  let currentProfile = null;
  // Seções Principais
  const authSection = document.getElementById('authSection');
  const profileSection = document.getElementById('profileSection');
  const userNavActions = document.getElementById('userNavActions');
  const headerEmpresaName = document.getElementById('headerEmpresaName');
  const btnLogout = document.getElementById('btnLogout');

  // Abas de Autenticação
  const tabCadastroBtn = document.getElementById('tabCadastroBtn');
  const tabLoginBtn = document.getElementById('tabLoginBtn');
  const cadastroContainer = document.getElementById('cadastroContainer');
  const loginContainer = document.getElementById('loginContainer');

  // Formulário de Cadastro (Create)
  const cadastroForm = document.getElementById('cadastroForm');
  const cadCnpj = document.getElementById('cadCnpj');

  // Formulário de Login
  const loginForm = document.getElementById('loginForm');
  const loginEmail = document.getElementById('loginEmail');
  const loginSenha = document.getElementById('loginSenha');

  // Cartão de Visualização (Read)
  const viewProfileCard = document.getElementById('viewProfileCard');
  const viewNome = document.getElementById('viewNome');
  const viewCnpj = document.getElementById('viewCnpj');
  const viewEmail = document.getElementById('viewEmail');
  const btnOpenEdit = document.getElementById('btnOpenEdit');

  // Cartão de Edição (Update)
  const editProfileCard = document.getElementById('editProfileCard');
  const editForm = document.getElementById('editForm');
  const editNome = document.getElementById('editNome');
  const editCnpj = document.getElementById('editCnpj');
  const editEmail = document.getElementById('editEmail');
  const editSenha = document.getElementById('editSenha');
  const btnCancelEdit = document.getElementById('btnCancelEdit');
  const btnCancelEditBottom = document.getElementById('btnCancelEditBottom');

  // Modal de Exclusão (Delete)
  const deleteModal = document.getElementById('deleteModal');
  const btnOpenDeleteModal = document.getElementById('btnOpenDeleteModal');
  const btnCloseDeleteModal = document.getElementById('btnCloseDeleteModal');
  const btnCancelDelete = document.getElementById('btnCancelDelete');
  const btnConfirmDelete = document.getElementById('btnConfirmDelete');

  // Inicialização
  async function init() {
    setupCnpjMask(cadCnpj);
    setupCnpjMask(editCnpj);
    setupEvents();
    await checkAuthStatus();
  }

  // Verifica se a empresa já está logada
  async function checkAuthStatus() {
    const empresa = window.empresaAuthService.getEmpresaLogada();
    if (empresa) {
      try {
        renderProfileView(await window.empresaAuthService.getMeusDados());
      } catch (err) {
        renderAuthView();
        showToast(err.message || 'Erro ao carregar dados da empresa.', 'danger');
      }
    } else {
      renderAuthView();
    }
  }

  function renderAuthView() {
    currentProfile = null;
    authSection.style.display = 'flex';
    profileSection.style.display = 'none';
    userNavActions.style.display = 'none';
  }

  function renderProfileView(empresa) {
    currentProfile = empresa;
    authSection.style.display = 'none';
    profileSection.style.display = 'flex';
    userNavActions.style.display = 'flex';

    headerEmpresaName.textContent = empresa.nome || 'Empresa Parceira';
    viewNome.textContent = empresa.nome || '-';
    viewCnpj.textContent = empresa.cnpj || '-';
    viewEmail.textContent = empresa.email || '-';

    viewProfileCard.style.display = 'block';
    editProfileCard.style.display = 'none';
  }

  function setupEvents() {
    // Alternar abas
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

    // 1. CADASTRO (CREATE)
    cadastroForm.addEventListener('submit', async (e) => {
      e.preventDefault();

      const dados = {
        nome: document.getElementById('cadNome').value.trim(),
        cnpj: document.getElementById('cadCnpj').value.trim(),
        email: document.getElementById('cadEmail').value.trim(),
        senha: document.getElementById('cadSenha').value
      };

      try {
        const novaEmpresa = await window.empresaAuthService.cadastrar(dados);
        showToast('Empresa parceira cadastrada com sucesso!', 'success');
        cadastroForm.reset();
        renderProfileView(novaEmpresa);
      } catch (err) {
        showToast(err.message || 'Erro ao cadastrar empresa.', 'danger');
      }
    });

    // 2. LOGIN
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = loginEmail.value.trim();
      const senha = loginSenha.value;

      try {
        const empresa = await window.empresaAuthService.login(email, senha);
        showToast('Login realizado com sucesso!', 'success');
        loginForm.reset();
        window.location.assign(window.AppRoutes.dashboard(empresa));
      } catch (err) {
        showToast(err.message || 'E-mail ou senha incorretos.', 'danger');
      }
    });

    // 3. EDITAR DADOS (UPDATE)
    btnOpenEdit.addEventListener('click', () => {
      const empresa = currentProfile;
      if (!empresa || !window.empresaAuthService.getEmpresaLogada()) return;

      editNome.value = empresa.nome || '';
      editCnpj.value = empresa.cnpj || '';
      editEmail.value = empresa.email || '';
      editSenha.value = '';

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
        cnpj: editCnpj.value.trim(),
        email: editEmail.value.trim()
      };

      if (editSenha.value) {
        dadosAtualizados.senha = editSenha.value;
      }

      try {
        const atualizada = await window.empresaAuthService.atualizarMeusDados(dadosAtualizados);
        showToast('Dados da empresa atualizados com sucesso!', 'success');
        renderProfileView(atualizada);
      } catch (err) {
        showToast(err.message || 'Erro ao atualizar dados da empresa.', 'danger');
      }
    });

    // 4. EXCLUIR CONTA (DELETE)
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
        await window.empresaAuthService.excluirMinhaConta();
        fecharModalExclusao();
        showToast('Conta da empresa excluída com sucesso.', 'success');
        renderAuthView();
      } catch (err) {
        showToast(err.message || 'Erro ao excluir conta.', 'danger');
      }
    });

    deleteModal.addEventListener('click', (e) => {
      if (e.target === deleteModal) fecharModalExclusao();
    });

    // LOGOUT
    btnLogout.addEventListener('click', () => {
      window.empresaAuthService.logout();
      showToast('Sessão encerrada.', 'success');
      renderAuthView();
    });
  }

  // Máscara de CNPJ: 00.000.000/0000-00
  function setupCnpjMask(input) {
    if (!input) return;
    input.addEventListener('input', (e) => {
      let v = e.target.value.replace(/\D/g, '');
      if (v.length > 14) v = v.slice(0, 14);

      if (v.length > 12) {
        v = v.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{1,2})/, '$1.$2.$3/$4-$5');
      } else if (v.length > 8) {
        v = v.replace(/^(\d{2})(\d{3})(\d{3})(\d{1,4})/, '$1.$2.$3/$4');
      } else if (v.length > 5) {
        v = v.replace(/^(\d{2})(\d{3})(\d{1,3})/, '$1.$2.$3');
      } else if (v.length > 2) {
        v = v.replace(/^(\d{2})(\d{1,3})/, '$1.$2');
      }
      e.target.value = v;
    });
  }

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

  await init();
});
