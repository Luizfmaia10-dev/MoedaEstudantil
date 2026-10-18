/**
 * RF-012 - Tela do aluno para solicitar transferência de moedas a outro aluno da mesma instituição.
 * Lê o aluno da URL (solicitar-transferencia.html?alunoId=4).
 * TODO: trocar o alunoId da URL pela sessão quando o login existir.
 */

const LIMITE_JUSTIFICATIVA = 300;
const STATUS_ROTULO = {
  PENDENTE: 'Pendente',
  APROVADA: 'Aprovada',
  RECUSADA: 'Recusada',
  CANCELADA: 'Cancelada'
};

const $ = id => document.getElementById(id);

let alunoId = null;
let saldoInfo = { saldoCorrente: 0, moedasPendentes: 0, saldoDisponivel: 0 };
let professores = [];

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
  const el = $('formFeedback');
  el.textContent = msg || '';
  el.className = tipo || '';
}

function atualizarCabecalhoSaldo() {
  $('saldoDisponivel').textContent = saldoInfo.saldoDisponivel.toLocaleString('pt-BR');
  $('saldoPendente').textContent = saldoInfo.moedasPendentes.toLocaleString('pt-BR');
  $('inpQuantidade').max = saldoInfo.saldoDisponivel;
  $('dicaLimite').textContent =
    `Saldo total: ${saldoInfo.saldoCorrente} · Disponível para solicitar: ${saldoInfo.saldoDisponivel} moedas.`;
}

function popularColegas(colegas) {
  const sel = $('selDestinatario');
  sel.innerHTML = '<option value="">Selecione um colega...</option>' +
    colegas.map(c => `<option value="${c.id}">${escapeHtml(c.nome)} — ${escapeHtml(c.curso || '')}</option>`).join('');
}

function popularProfessores(lista) {
  professores = lista;
  const sel = $('selAvaliador');
  sel.innerHTML = '<option value="">Selecione um professor...</option>' +
    lista.map(p => `<option value="${p.professorId}">${escapeHtml(p.professorNome)} (${escapeHtml(p.departamento || '')})</option>`).join('');
}

function atualizarCriterioSelecionado() {
  const id = Number($('selAvaliador').value);
  const prof = professores.find(p => p.professorId === id);
  const box = $('criterioBox');
  if (!prof) {
    box.style.display = 'none';
    $('criterioTexto').textContent = '';
    return;
  }
  $('criterioTexto').textContent = prof.criterios || 'Este professor não cadastrou critérios específicos.';
  box.style.display = 'block';
}

function renderizarSolicitacoes(lista) {
  const ul = $('listaSolicitacoes');
  const vazio = $('vazioSolicitacoes');
  if (!lista.length) {
    ul.innerHTML = '';
    vazio.style.display = 'block';
    return;
  }
  vazio.style.display = 'none';
  ul.innerHTML = lista.map(s => {
    const pendente = s.status === 'PENDENTE';
    const just = s.justificativa
      ? `<div class="sol-just">Sua justificativa: "${escapeHtml(s.justificativa)}"</div>`
      : '';
    const obs = s.observacaoAvaliador
      ? `<div class="sol-just" style="font-style:normal;color:var(--primary-deep);">Parecer do professor: "${escapeHtml(s.observacaoAvaliador)}"</div>`
      : '';
    const botaoCancelar = pendente
      ? `<button type="button" class="btn-cancelar" data-cancelar="${s.id}">Cancelar</button>`
      : '';
    return `
      <li class="sol-item">
        <div>
          <div class="sol-principal">${s.quantidade} moedas para ${escapeHtml(s.destinatarioNome)}</div>
          <div class="sol-meta">${formatarData(s.dataSolicitacao)} · Avaliador: Prof. ${escapeHtml(s.avaliadorNome)}</div>
          ${just}
          ${obs}
        </div>
        <div class="sol-direita">
          <span class="badge-status ${escapeHtml(s.status)}">${escapeHtml(STATUS_ROTULO[s.status] || s.status)}</span>
          ${botaoCancelar}
        </div>
      </li>`;
  }).join('');
}

async function recarregarSaldoESolicitacoes() {
  const [saldo, solicitacoes] = await Promise.all([
    window.transferenciaService.buscarSaldoDisponivel(alunoId),
    window.transferenciaService.listarSolicitacoes(alunoId)
  ]);
  saldoInfo = saldo;
  atualizarCabecalhoSaldo();
  renderizarSolicitacoes(solicitacoes);
}

function validarFormulario() {
  const destinatarioId = Number($('selDestinatario').value);
  const avaliadorId = Number($('selAvaliador').value);
  const quantidade = Number($('inpQuantidade').value);
  const justificativa = $('inpJustificativa').value.trim();

  if (!destinatarioId) return 'Selecione o aluno destinatário.';
  if (!avaliadorId) return 'Selecione o professor avaliador.';
  if (!Number.isInteger(quantidade) || quantidade <= 0) {
    return 'A quantidade deve ser um número inteiro maior que zero.';
  }
  if (quantidade > saldoInfo.saldoDisponivel) {
    return `A quantidade não pode ultrapassar seu saldo disponível (${saldoInfo.saldoDisponivel} moedas).`;
  }
  if (justificativa.length > LIMITE_JUSTIFICATIVA) {
    return `A justificativa deve ter no máximo ${LIMITE_JUSTIFICATIVA} caracteres.`;
  }
  return null;
}

async function enviar(e) {
  e.preventDefault();
  feedback('');
  const erro = validarFormulario();
  if (erro) return feedback(erro, 'erro');

  $('btnEnviar').disabled = true;
  try {
    await window.transferenciaService.solicitar(alunoId, {
      destinatarioId: Number($('selDestinatario').value),
      avaliadorId: Number($('selAvaliador').value),
      quantidade: Number($('inpQuantidade').value),
      justificativa: $('inpJustificativa').value.trim()
    });
    $('inpQuantidade').value = '';
    $('inpJustificativa').value = '';
    $('contadorJust').textContent = `0/${LIMITE_JUSTIFICATIVA}`;
    await recarregarSaldoESolicitacoes();
    feedback('Solicitação enviada com sucesso!', 'ok');
  } catch (err) {
    feedback(err instanceof TypeError ? 'Backend indisponível.' : err.message, 'erro');
  } finally {
    $('btnEnviar').disabled = false;
  }
}

async function cancelar(solicitacaoId) {
  if (!confirm('Deseja cancelar esta solicitação pendente?')) return;
  feedback('');
  try {
    await window.transferenciaService.cancelar(alunoId, solicitacaoId);
    await recarregarSaldoESolicitacoes();
    feedback('Solicitação cancelada.', 'ok');
  } catch (err) {
    feedback(err instanceof TypeError ? 'Backend indisponível.' : err.message, 'erro');
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  alunoId = new URLSearchParams(window.location.search).get('alunoId');
  if (!alunoId) return mostrarErroGeral('Aluno não informado. Acesse com ?alunoId=<id> na URL.');

  try {
    const [saldo, colegas, profs, solicitacoes] = await Promise.all([
      window.transferenciaService.buscarSaldoDisponivel(alunoId),
      window.transferenciaService.listarColegas(alunoId),
      window.transferenciaService.listarProfessoresComCriterios(alunoId),
      window.transferenciaService.listarSolicitacoes(alunoId)
    ]);
    saldoInfo = saldo;
    atualizarCabecalhoSaldo();
    popularColegas(colegas);
    popularProfessores(profs);
    renderizarSolicitacoes(solicitacoes);

    $('loading').style.display = 'none';
    $('conteudo').style.display = 'flex';
  } catch (err) {
    return mostrarErroGeral(err);
  }

  $('selAvaliador').addEventListener('change', atualizarCriterioSelecionado);
  $('inpJustificativa').addEventListener('input', () => {
    $('contadorJust').textContent = `${$('inpJustificativa').value.length}/${LIMITE_JUSTIFICATIVA}`;
  });
  $('formTransferencia').addEventListener('submit', enviar);
  $('listaSolicitacoes').addEventListener('click', e => {
    const btn = e.target.closest('button[data-cancelar]');
    if (btn) cancelar(Number(btn.dataset.cancelar));
  });
});
