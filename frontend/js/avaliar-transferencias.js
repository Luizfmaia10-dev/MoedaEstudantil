/** RF-013 - Avaliação manual e automação das transferências do professor. */
const STATUS_ROTULO = { PENDENTE: 'Pendente', APROVADA: 'Aprovada', RECUSADA: 'Recusada', CANCELADA: 'Cancelada' };
const $ = id => document.getElementById(id);
let professorId = null;
let automacaoAtual = null;

function quantidadePendente(total) {
  return `${total} ${total === 1 ? 'solicitação pendente' : 'solicitações pendentes'}`;
}

function automationViewModel(config) {
  const pendentes = Number(config?.pendentes || 0);
  if (!config?.possuiCriterios) return {
    status: 'Critérios necessários',
    summary: `Cadastre seus critérios antes de ativar a IA. Há ${quantidadePendente(pendentes)} para sua revisão.`,
    action: 'Cadastrar critérios', nextActive: true, disabled: true, tone: 'warning'
  };
  if (config.ativa) return {
    status: 'Automação ativa',
    summary: `A IA analisa novas solicitações com seus critérios. ${pendentes} ${pendentes === 1 ? 'solicitação continua' : 'solicitações continuam'} pendentes para sua revisão.`,
    action: 'Desativar automação', nextActive: false, disabled: false, tone: 'active'
  };
  return {
    status: 'Automação desligada',
    summary: `Ative a IA para analisar solicitações atuais e futuras. Há ${quantidadePendente(pendentes)}.`,
    action: 'Ativar automação', nextActive: true, disabled: false, tone: 'inactive'
  };
}

function automaticDecisionBadge(solicitacao) {
  if (!solicitacao?.avaliacaoAutomatica) return '';
  const confianca = Number(solicitacao.confiancaAvaliacaoAutomatica);
  return `Avaliada pela IA${Number.isFinite(confianca) ? ` · ${Math.round(confianca * 100)}% de confiança` : ''}`;
}

function mostrarErroGeral(err) {
  $('loading').style.display = 'none';
  $('conteudo').style.display = 'block';
  const el = $('erroGeral');
  el.textContent = err instanceof TypeError ? 'Backend indisponível. Inicie o servidor Spring Boot (porta 8080).' : (typeof err === 'string' ? err : err.message);
  el.style.display = 'block';
}

function feedback(msg, tipo) {
  const el = $('feedback');
  el.textContent = msg || '';
  el.className = tipo || '';
}

function renderizarAutomacao(config) {
  automacaoAtual = config;
  const model = automationViewModel(config);
  const status = $('automacaoStatus');
  const botao = $('alternarAutomacao');
  status.textContent = model.status;
  status.dataset.tone = model.tone;
  $('automacaoResumo').textContent = model.summary;
  $('automacaoAcao').textContent = model.action;
  botao.dataset.tone = model.tone;
  botao.dataset.nextActive = String(model.nextActive);
  botao.disabled = model.disabled;
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
      ? `<div class="bloco-texto"><strong>Justificativa do aluno:</strong> ${AppFormat.escapeHtml(s.justificativa)}</div>`
      : '<div class="bloco-texto"><em>Aluno não informou justificativa.</em></div>';
    const avaliacaoData = s.dataAvaliacao ? ` · Avaliada em ${AppFormat.date(s.dataAvaliacao)}` : '';
    const parecerSalvo = !pendente && s.observacaoAvaliador
      ? `<div class="bloco-texto"><strong>Explicação da decisão:</strong> ${AppFormat.escapeHtml(s.observacaoAvaliador)}</div>` : '';
    const seloIa = automaticDecisionBadge(s);
    const auditoria = seloIa ? `<span class="badge-ia">${AppFormat.escapeHtml(seloIa)}</span>` : '';
    const controles = pendente ? `<div class="aval-controles">
      <input type="text" maxlength="300" data-obs="${s.id}" placeholder="Explicação opcional (até 300 caracteres)...">
      <button type="button" class="btn-aprovar" data-acao="aprovar" data-id="${s.id}">Aprovar</button>
      <button type="button" class="btn-recusar" data-acao="recusar" data-id="${s.id}">Recusar</button>
    </div>` : '';
    return `<li class="aval-item">
      <div class="aval-linha1"><div>
        <div class="aval-Partes">${AppFormat.escapeHtml(s.remetenteNome)} <span>(${AppFormat.escapeHtml(s.remetenteCurso || '')})</span> &rarr; ${AppFormat.escapeHtml(s.destinatarioNome)} <span>(${AppFormat.escapeHtml(s.destinatarioCurso || '')})</span></div>
        <div class="aval-meta">Solicitada em ${AppFormat.date(s.dataSolicitacao) || '—'}${avaliacaoData}</div>
      </div><div class="aval-direita">
        <span class="aval-valor">${s.quantidade} moedas</span>
        <span class="badge-status ${AppFormat.escapeHtml(s.status)}">${AppFormat.escapeHtml(STATUS_ROTULO[s.status] || s.status)}</span>
      </div></div>${auditoria}${just}${parecerSalvo}${controles}
    </li>`;
  }).join('');
}

async function carregar() {
  try {
    const [lista, automacao] = await Promise.all([
      window.transferenciaService.listarParaProfessor(professorId, $('filtroStatus').value),
      window.transferenciaService.consultarAutomacao(professorId)
    ]);
    $('loading').style.display = 'none';
    $('conteudo').style.display = 'block';
    renderizar(lista);
    renderizarAutomacao(automacao);
  } catch (err) { mostrarErroGeral(err); }
}

async function alternarAutomacao() {
  const botao = $('alternarAutomacao');
  if (!automacaoAtual?.possuiCriterios) {
    window.location.href = $('automacaoCriterios').href;
    return;
  }
  const ativa = botao.dataset.nextActive === 'true';
  feedback(ativa ? 'Ativando a automação e analisando pendências...' : 'Desativando a automação...');
  botao.disabled = true;
  try {
    renderizarAutomacao(await window.transferenciaService.configurarAutomacao(professorId, ativa));
    feedback(ativa ? 'Automação ativada. Decisões incertas continuam pendentes.' : 'Automação desativada.', 'ok');
    await carregar();
  } catch (err) {
    feedback(err instanceof TypeError ? 'Backend indisponível.' : err.message, 'erro');
    botao.disabled = false;
  }
}

async function avaliar(solicitacaoId, acao, botao) {
  const inputObs = document.querySelector(`input[data-obs="${solicitacaoId}"]`);
  const observacao = inputObs ? inputObs.value.trim() : '';
  if (acao === 'recusar' && !confirm('Confirmar a recusa desta solicitação de transferência?')) return;
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

function inicializar() {
  professorId = new URLSearchParams(window.location.search).get('professorId');
  if (!professorId) return mostrarErroGeral('Professor não informado. Acesse com ?professorId=<id> na URL.');
  const criteriosUrl = `criterios-professor.html?professorId=${encodeURIComponent(professorId)}`;
  $('linkCriterios').href = criteriosUrl;
  $('automacaoCriterios').href = criteriosUrl;
  $('alternarAutomacao').addEventListener('click', alternarAutomacao);
  $('filtroStatus').addEventListener('change', () => { feedback(''); carregar(); });
  $('listaAvaliacoes').addEventListener('click', e => {
    const btn = e.target.closest('button[data-acao]');
    if (btn) avaliar(Number(btn.dataset.id), btn.dataset.acao, btn);
  });
  carregar();
}

if (typeof document !== 'undefined') document.addEventListener('DOMContentLoaded', inicializar);
if (typeof module !== 'undefined' && module.exports) module.exports = { automationViewModel, automaticDecisionBadge };
