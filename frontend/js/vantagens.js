/**
 * SISTEMA DE MOEDA ESTUDANTIL - TELA DE GESTÃO DE VANTAGENS (RF-008)
 * Lê a empresa da URL (vantagens.html?empresaId=5).
 * TODO: trocar o empresaId da URL pela sessão quando o login existir.
 */

const TIPOS_IMAGEM = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
const TAMANHO_MAX_BYTES = 5 * 1024 * 1024;

let empresaId = null;
let vantagens = [];
let editandoId = null; // null = criando

const $ = id => document.getElementById(id);

function mostrarSomente(estado) {
  $('vantLoading').style.display = estado === 'loading' ? 'block' : 'none';
  $('vantErro').style.display = estado === 'erro' ? 'block' : 'none';
  $('vantVazio').style.display = estado === 'vazio' ? 'block' : 'none';
  $('vantLista').style.display = estado === 'lista' ? 'grid' : 'none';
}

function mostrarErroGeral(err) {
  $('vantErro').textContent = err instanceof TypeError
    ? 'Backend indisponível. Inicie o servidor Spring Boot (porta 8080).'
    : err.message;
  mostrarSomente('erro');
}

function renderizar() {
  if (!vantagens.length) return mostrarSomente('vazio');

  $('vantLista').innerHTML = vantagens.map(v => {
    const url = window.vantagemService.urlFoto(v.fotoUrl);
    const foto = url
      ? `<img class="vant-foto" src="${AppFormat.escapeHtml(url)}" alt="${AppFormat.escapeHtml(v.nome)}">`
      : '<div class="vant-foto">🎁</div>';
    return `
      <li class="vant-card">
        ${foto}
        <div class="vant-corpo">
          <h3>${AppFormat.escapeHtml(v.nome)}</h3>
          <p>${AppFormat.escapeHtml(v.descricao)}</p>
          <span class="vant-custo">${v.custoMoedas} moedas</span>
        </div>
        <div class="vant-acoes">
          <button type="button" data-acao="editar" data-id="${v.id}">Editar</button>
          <button type="button" class="remover" data-acao="remover" data-id="${v.id}">Remover</button>
        </div>
      </li>`;
  }).join('');
  mostrarSomente('lista');
}

async function carregar() {
  mostrarSomente('loading');
  try {
    vantagens = await window.vantagemService.listar(empresaId);
    renderizar();
  } catch (err) {
    mostrarErroGeral(err);
  }
}

/* ---------- Modal ---------- */

function mostrarErroForm(msg) {
  const el = $('formErro');
  el.textContent = msg || '';
  el.style.display = msg ? 'block' : 'none';
}

function atualizarPrevia(src) {
  const img = $('fPrevia');
  if (src) { img.src = src; img.style.display = 'block'; }
  else { img.removeAttribute('src'); img.style.display = 'none'; }
}

function abrirModal(vantagem, gatilho) {
  editandoId = vantagem ? vantagem.id : null;
  $('modalTitulo').textContent = vantagem ? 'Editar vantagem' : 'Nova vantagem';
  $('fNome').value = vantagem ? vantagem.nome : '';
  $('fDescricao').value = vantagem ? vantagem.descricao : '';
  $('fCusto').value = vantagem ? vantagem.custoMoedas : '';
  $('fFoto').value = '';
  $('fotoDica').textContent = vantagem ? '(deixe vazio para manter a atual)' : '(obrigatória)';
  atualizarPrevia(vantagem ? window.vantagemService.urlFoto(vantagem.fotoUrl) : null);
  mostrarErroForm('');
  if (window.modalGsap) window.modalGsap.openOverlay($('modalFundo'), $('formVantagem'), gatilho);
  else { $('modalFundo').classList.add('aberto'); $('fNome').focus(); }
}

function fecharModal() {
  if (window.modalGsap) return window.modalGsap.closeOverlay($('modalFundo'), $('formVantagem'));
  $('modalFundo').classList.remove('aberto');
  return Promise.resolve();
}

function validarFormulario() {
  const nome = $('fNome').value.trim();
  const descricao = $('fDescricao').value.trim();
  const custo = Number($('fCusto').value);
  const foto = $('fFoto').files[0];

  if (!nome) return 'Informe o nome da vantagem.';
  if (!descricao) return 'Informe a descrição da vantagem.';
  if (!Number.isInteger(custo) || custo <= 0) return 'O custo deve ser um número inteiro maior que zero.';
  if (!editandoId && !foto) return 'Selecione a foto do produto.';
  if (foto) {
    if (!TIPOS_IMAGEM.includes(foto.type)) return 'Formato de imagem inválido. Use JPG, PNG, WEBP ou GIF.';
    if (foto.size > TAMANHO_MAX_BYTES) return 'A imagem excede o tamanho máximo de 5 MB.';
  }
  return null;
}

async function salvar(evento) {
  evento.preventDefault();
  const erro = validarFormulario();
  if (erro) return mostrarErroForm(erro);

  const dados = {
    nome: $('fNome').value.trim(),
    descricao: $('fDescricao').value.trim(),
    custoMoedas: Number($('fCusto').value),
    foto: $('fFoto').files[0] || null
  };

  $('btnSalvar').disabled = true;
  try {
    if (editandoId) await window.vantagemService.atualizar(empresaId, editandoId, dados);
    else await window.vantagemService.criar(empresaId, dados);
    await fecharModal();
    await carregar();
  } catch (err) {
    mostrarErroForm(err instanceof TypeError ? 'Backend indisponível.' : err.message);
  } finally {
    $('btnSalvar').disabled = false;
  }
}

async function remover(id) {
  const v = vantagens.find(x => x.id === id);
  if (!v || !confirm(`Remover a vantagem "${v.nome}"? Resgates já realizados não serão afetados.`)) return;
  try {
    await window.vantagemService.remover(empresaId, id);
    await carregar();
  } catch (err) {
    alert(err.message);
  }
}

document.addEventListener('DOMContentLoaded', () => {
  empresaId = new URLSearchParams(window.location.search).get('empresaId');
  if (!empresaId) {
    $('btnNova').style.display = 'none';
    $('vantErro').textContent = 'Empresa não informada. Acesse com ?empresaId=<id> na URL.';
    return mostrarSomente('erro');
  }

  $('btnNova').addEventListener('click', event => abrirModal(null, event.currentTarget));
  $('btnCancelar').addEventListener('click', fecharModal);
  $('modalFundo').addEventListener('click', e => { if (e.target === $('modalFundo')) fecharModal(); });
  $('formVantagem').addEventListener('submit', salvar);

  $('fFoto').addEventListener('change', () => {
    const arquivo = $('fFoto').files[0];
    atualizarPrevia(arquivo ? URL.createObjectURL(arquivo) : null);
  });

  $('vantLista').addEventListener('click', e => {
    const btn = e.target.closest('button[data-acao]');
    if (!btn) return;
    const id = Number(btn.dataset.id);
    if (btn.dataset.acao === 'editar') abrirModal(vantagens.find(v => v.id === id), btn);
    else remover(id);
  });

  carregar();
});
