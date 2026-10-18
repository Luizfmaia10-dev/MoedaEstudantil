/**
 * RF-013 - Tela do professor para avaliar (aprovar ou recusar) solicitações de transferência.
 * Lê o professor da URL (avaliar-transferencias.html?professorId=1).
 * TODO: trocar o professorId da URL pela sessão quando o login existir.
 */

const STATUS_ROTULO = {
  PENDENTE: 'Pendente',
  APROVADA: 'Aprovada',
  RECUSADA: 'Recusada',
  CANCELADA: 'Cancelada'
};

const $ = id => document.getElementById(id);
let professorId = null;

function escapeHtml(texto) {
  const div = document.createElement('div');
  div.textContent = texto ?? '';
  return div.innerHTML;
}

function formatarData(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

function mostrarErroGeral(err) {
  $('loading').style.display = 'none';
  $('conteudo').style.display = 'none';
  const el = $('erroGeral');
  el.textContent = err instanceof TypeError
    ? 'Backend indisponível. Inicie o servidor Spring Boot (porta 8080).'
    : (typeof err === 'string' ? err : err.message);
  el.style.display = 'block';
}

function feedback(msg, tipo) {
  const el = $('feedback');
  el.textContent = msg || '';
  el.className = tipo || '';
}

function renderizar(lista) {
  const ul = $('listaAvaliacoes');
  const vazio = $('vazio');
  if (!lista.length) {
    ul.innerHTML = '';
    vazio.style.display = 'block';
    return;
  }
  vazio.style.display = 'none';

  ul.innerHTML = lista.map(s => {
    const pendente = s.status === 'PENDENTE';
    const just = s.justificativa
      ? `<div class="bloco-texto"><strong>Justificativa do aluno:</strong> ${escapeHtml(s.justificativa)}</div>`
      : '<div class="bloco-texto"><em>Aluno não informou justificativa.</em></div>';

    const avaliacaoData = s.dataAvaliacao ? ` · Avaliada em ${formatarData(s.dataAvaliacao)}` : '';
    const parecerSalvo = !pendente && s.observacaoAvaliador
      ? `<div class="bloco-texto"><strong>Sua explicação:</strong> ${escapeHtml(s.observacaoAvaliador)}</div>`
      : '';

    const controles = pendente ? `
      <div class="aval-controles">
        <input type="text" maxlength="300" data-obs="${s.id}" placeholder="Explicação opcional (até 300 caracteres)...">
        <button type="button" class="btn-aprovar" data-acao="aprovar" data-id="${s.id}">Aprovar</button>
        <button type="button" class="btn-recusar" data-acao="recusar" data-id="${s.id}">Recusar</button>
      </div>` : '';

    return `
      <li class="aval-item">
        <div class="aval-linha1">
          <div>
            <div class="aval-Partes">
              ${escapeHtml(s.remetenteNome)} <span>(${escapeHtml(s.remetenteCurso || '')})</span>
              &rarr;
              ${escapeHtml(s.destinatarioNome)} <span>(${escapeHtml(s.destinatarioCurso || '')})</span>
            </div>
            <div class="aval-meta">Solicitada em ${formatarData(s.dataSolicitacao)}${avaliacaoData}</div>
          </div>
          <div class="aval-direita">
            <span class="aval-valor">${s.quantidade} moedas</span>
            <span class="badge-status ${escapeHtml(s.status)}">${escapeHtml(STATUS_ROTULO[s.status] || s.status)}</span>
          </div>
        </div>
        ${just}
        ${parecerSalvo}
        ${controles}
      </li>`;
  }).join('');
}

async function carregar() {
  const status = $('filtroStatus').value;
  try {
    const lista = await window.transferenciaService.listarParaProfessor(professorId, status);
    $('loading').style.display = 'none';
    $('conteudo').style.display = 'block';
    renderizar(lista);
  } catch (err) {
    mostrarErroGeral(err);
  }
}

async function avaliar(solicitacaoId, acao, botao) {
  const inputObs = document.querySelector(`input[data-obs="${solicitacaoId}"]`);
  const observacao = inputObs ? inputObs.value.trim() : '';

  if (acao === 'recusar' && !confirm('Confirmar a recusa desta solicitação de transferência?')) {
    return;
  }

  feedback('');
  botao.disabled = true;
  try {
    if (acao === 'aprovar') {
      await window.transferenciaService.aprovar(professorId, solicitacaoId, observacao);
      feedback('Transferência aprovada e moedas creditadas!', 'ok');
    } else {
      await window.transferenciaService.recusar(professorId, solicitacaoId, observacao);
      feedback('Solicitação recusada.', 'ok');
    }
    await carregar();
  } catch (err) {
    feedback(err instanceof TypeError ? 'Backend indisponível.' : err.message, 'erro');
    botao.disabled = false;
  }
}

document.addEventListener('DOMContentLoaded', () => {
  professorId = new URLSearchParams(window.location.search).get('professorId');
  if (!professorId) {
    return mostrarErroGeral('Professor não informado. Acesse com ?professorId=<id> na URL.');
  }

  $('linkCriterios').href = `criterios-professor.html?professorId=${encodeURIComponent(professorId)}`;
  $('filtroStatus').addEventListener('change', () => {
    feedback('');
    carregar();
  });

  $('listaAvaliacoes').addEventListener('click', e => {
    const btn = e.target.closest('button[data-acao]');
    if (!btn) return;
    avaliar(Number(btn.dataset.id), btn.dataset.acao, btn);
  });

  carregar();
});
