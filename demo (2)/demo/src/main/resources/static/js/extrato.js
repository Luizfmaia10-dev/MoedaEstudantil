/**
 * SISTEMA DE MOEDA ESTUDANTIL - TELA DE EXTRATO (RF-006)
 * Lê o usuário da URL (extrato.html?usuarioId=1) e renderiza saldo + movimentações.
 * TODO: trocar o usuarioId da URL pela sessão quando o login existir.
 */

const CATEGORIA_ROTULO = { ENVIO: 'Envio', RESGATE: 'Resgate', TRANSFERENCIA: 'Transferência', OUTRO: 'Outro' };

function escapeHtml(texto) {
  const div = document.createElement('div');
  div.textContent = texto ?? '';
  return div.innerHTML;
}

function formatarData(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

function mostrarErro(msg) {
  document.getElementById('extratoLoading').style.display = 'none';
  document.getElementById('extratoConteudo').style.display = 'none';
  const el = document.getElementById('extratoErro');
  el.textContent = msg;
  el.style.display = 'block';
}

function renderizar(extrato) {
  document.getElementById('extratoLoading').style.display = 'none';
  document.getElementById('extratoErro').style.display = 'none';
  document.getElementById('extratoConteudo').style.display = 'block';

  document.getElementById('titularNome').textContent = extrato.titularNome;
  document.getElementById('titularTipo').textContent = extrato.titularTipo === 'PROFESSOR' ? 'Professor' : 'Aluno';
  document.getElementById('saldoAtual').textContent = extrato.saldoAtual.toLocaleString('pt-BR');

  const corpo = document.getElementById('extratoLista');
  const vazio = document.getElementById('extratoVazio');

  if (!extrato.transacoes.length) {
    corpo.innerHTML = '';
    vazio.style.display = 'block';
    return;
  }
  vazio.style.display = 'none';

  corpo.innerHTML = extrato.transacoes.map(t => {
    const entrada = t.movimentacao === 'ENTRADA';
    return `
      <li class="extrato-item">
        <div class="extrato-info">
          <span class="extrato-desc">${escapeHtml(t.descricao)}</span>
          <span class="extrato-meta">${formatarData(t.dataHora)} · ${escapeHtml(CATEGORIA_ROTULO[t.categoria] || t.categoria)}</span>
        </div>
        <span class="extrato-valor ${entrada ? 'entrada' : 'saida'}">${entrada ? '+' : '−'}${t.quantidade}</span>
      </li>`;
  }).join('');
}

async function carregar(usuarioId, categoria) {
  document.getElementById('extratoLoading').style.display = 'block';
  try {
    renderizar(await window.extratoService.buscarExtrato(usuarioId, categoria));
  } catch (err) {
    const offline = err instanceof TypeError;
    mostrarErro(offline ? 'Backend indisponível. Inicie o servidor Spring Boot (porta 8080).' : err.message);
  }
}

document.addEventListener('DOMContentLoaded', () => {
  const usuarioId = new URLSearchParams(window.location.search).get('usuarioId');
  if (!usuarioId) {
    mostrarErro('Usuário não informado. Acesse com ?usuarioId=<id> na URL.');
    return;
  }

  const filtro = document.getElementById('filtroCategoria');
  filtro.addEventListener('change', () => carregar(usuarioId, filtro.value));
  carregar(usuarioId, '');
});
