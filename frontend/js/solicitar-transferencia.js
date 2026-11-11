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
  $('saldoDisponivel').textContent = AppFormat.number(saldoInfo.saldoDisponivel);
  $('saldoPendente').textContent = AppFormat.number(saldoInfo.moedasPendentes);
  $('inpQuantidade').max = saldoInfo.saldoDisponivel;
  $('dicaLimite').textContent =
    `Saldo total: ${saldoInfo.saldoCorrente} · Disponível para solicitar: ${saldoInfo.saldoDisponivel} moedas.`;
}

function popularColegas(colegas) {
  const sel = $('selDestinatario');
  sel.innerHTML = '<option value="">Selecione um colega...</option>' +
    colegas.map(c => `<option value="${c.id}">${AppFormat.escapeHtml(c.nome)} — ${AppFormat.escapeHtml(c.curso || '')}</option>`).join('');
}

function popularProfessores(lista) {
  professores = lista;
  const sel = $('selAvaliador');
  sel.innerHTML = '<option value="">Selecione um professor...</option>' +
    lista.map(p => `<option value="${p.professorId}">${AppFormat.escapeHtml(p.professorNome)} (${AppFormat.escapeHtml(p.departamento || '')})</option>`).join('');
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
      ? `<div class="sol-just">Sua justificativa: "${AppFormat.escapeHtml(s.justificativa)}"</div>`
      : '';
    const obs = s.observacaoAvaliador
      ? `<div class="sol-just" style="font-style:normal;color:var(--primary-deep);">Parecer do professor: "${AppFormat.escapeHtml(s.observacaoAvaliador)}"</div>`
      : '';
    const botaoCancelar = pendente
      ? `<button type="button" class="btn-cancelar" data-cancelar="${s.id}">Cancelar</button>`
      : '';
    return `
      <li class="sol-item">
        <div>
          <div class="sol-principal">${s.quantidade} moedas para ${AppFormat.escapeHtml(s.destinatarioNome)}</div>
          <div class="sol-meta">${AppFormat.date(s.dataSolicitacao) || '—'} · Avaliador: Prof. ${AppFormat.escapeHtml(s.avaliadorNome)}</div>
          ${just}
          ${obs}
        </div>
        <div class="sol-direita">
          <span class="badge-status ${AppFormat.escapeHtml(s.status)}">${AppFormat.escapeHtml(STATUS_ROTULO[s.status] || s.status)}</span>
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

function preencherFormularioComRascunho(rascunho) {
  if (rascunho.destinatarioId) $('selDestinatario').value = String(rascunho.destinatarioId);
  if (rascunho.avaliadorId) {
    $('selAvaliador').value = String(rascunho.avaliadorId);
    atualizarCriterioSelecionado();
  }
  if (rascunho.quantidade) $('inpQuantidade').value = String(rascunho.quantidade);
  if (rascunho.justificativa) {
    $('inpJustificativa').value = rascunho.justificativa;
    $('contadorJust').textContent = `${rascunho.justificativa.length}/${LIMITE_JUSTIFICATIVA}`;
  }
  const primeiroAusente = !rascunho.destinatarioId ? $('selDestinatario')
    : !rascunho.avaliadorId ? $('selAvaliador')
      : !rascunho.quantidade ? $('inpQuantidade') : $('inpJustificativa');
  primeiroAusente.focus();
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

  window.TransferAssistant?.initializeTransferAssistant({
    document,
    api: window.transferenciaService,
    alunoId,
    modalAnimator: window.modalGsap,
    starBorder: window.StarBorder,
    thoughtLine: window.ThoughtLine,
    voicePill: window.VoicePill,
    onUseManual: preencherFormularioComRascunho,
    onConfirmed: async () => {
      await recarregarSaldoESolicitacoes();
      feedback('Solicitação criada com a ajuda da IA!', 'ok');
      const dialog = $('assistenteDialog');
      window.setTimeout(() => {
        if (!dialog.open) return;
        if (window.modalGsap?.closeDialog) window.modalGsap.closeDialog(dialog);
        else dialog.close();
      }, 900);
    }
  });
});
