function normalizar(texto) {
  return String(texto || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
}

function filtrarVantagens(vantagens, termo, somenteDisponiveis, saldo) {
  const busca = normalizar(termo);
  return (vantagens || []).filter(vantagem => {
    const texto = normalizar(`${vantagem.nome || ''} ${vantagem.descricao || ''} ${vantagem.empresaNome || ''}`);
    return (!busca || texto.includes(busca)) && (!somenteDisponiveis || Number(vantagem.custoMoedas) <= Number(saldo));
  });
}

function catalogViewModel(vantagem, saldo) {
  const faltam = Math.max(0, Number(vantagem.custoMoedas) - Number(saldo));
  return faltam > 0
    ? { disabled: true, buttonLabel: 'Saldo insuficiente', availability: `Faltam ${faltam} moedas` }
    : { disabled: false, buttonLabel: 'Resgatar', availability: 'Disponível para resgate' };
}

let alunoId;
let catalogo;
let vantagemPendente;
const $r = id => document.getElementById(id);

function escaparR(texto) {
  const el = document.createElement('div');
  el.textContent = texto ?? '';
  return el.innerHTML;
}

async function fecharModalR() {
  await window.modalGsap.fechar($r('modal'), $r('modalPainel'));
}

function renderizar() {
  if (!catalogo) return;
  $r('saldoAluno').textContent = AppFormat.number(catalogo.saldo);
  const termo = $r('buscaVantagem').value;
  const somenteDisponiveis = $r('somenteDisponiveis').checked;
  const vantagens = filtrarVantagens(catalogo.vantagens, termo, somenteDisponiveis, catalogo.saldo);
  const total = vantagens.length;
  $r('contadorVantagens').textContent = `${total} ${total === 1 ? 'vantagem encontrada' : 'vantagens encontradas'}`;

  if (!catalogo.vantagens.length) {
    $r('estadoCatalogo').hidden = false;
    $r('estadoCatalogo').textContent = 'Nenhuma vantagem disponível no momento.';
    $r('gradeVantagens').hidden = true;
    return;
  }
  if (!total) {
    $r('estadoCatalogo').hidden = false;
    $r('estadoCatalogo').textContent = 'Nenhuma vantagem corresponde aos filtros selecionados.';
    $r('gradeVantagens').hidden = true;
    return;
  }

  $r('estadoCatalogo').hidden = true;
  $r('gradeVantagens').hidden = false;
  $r('gradeVantagens').innerHTML = vantagens.map(v => {
    const foto = window.resgateApi.foto(v.fotoUrl);
    const state = catalogViewModel(v, catalogo.saldo);
    return `<li class="benefit-card">
      ${foto ? `<img src="${escaparR(foto)}" alt="${escaparR(v.nome)}" loading="lazy">` : '<div class="benefit-placeholder" aria-hidden="true">ME</div>'}
      <div class="benefit-body">
        <span class="benefit-company">${escaparR(v.empresaNome)}</span>
        <h2>${escaparR(v.nome)}</h2>
        <p>${escaparR(v.descricao)}</p>
      </div>
      <div class="benefit-footer">
        <div class="benefit-price"><span class="benefit-cost">${v.custoMoedas} moedas</span><small class="${state.disabled ? 'is-unavailable' : ''}">${state.availability}</small></div>
        <button class="btn-primary" type="button" data-id="${v.id}" ${state.disabled ? `disabled title="${state.availability}"` : ''}>${state.buttonLabel}</button>
      </div>
    </li>`;
  }).join('');
}

function confirmar(vantagem, gatilho) {
  vantagemPendente = vantagem;
  $r('modalTitulo').textContent = 'Confirmar resgate';
  $r('modalCorpo').innerHTML = `Resgatar <strong>${escaparR(vantagem.nome)}</strong> por <strong>${vantagem.custoMoedas} moedas</strong>? O cupom será exibido na tela após a confirmação.`;
  $r('modalCancelar').hidden = false;
  $r('modalConfirmar').hidden = false;
  $r('modalConfirmar').textContent = 'Confirmar resgate';
  window.modalGsap.abrir($r('modal'), $r('modalPainel'), gatilho);
}

function sucesso(resultado) {
  $r('modalTitulo').textContent = 'Resgate confirmado';
  $r('modalCorpo').innerHTML = `Seu cupom foi gerado. Guarde este código para apresentar à empresa parceira.<span class="coupon-code">${escaparR(resultado.codigoCupom)}</span>`;
  $r('modalCancelar').hidden = true;
  $r('modalConfirmar').hidden = false;
  $r('modalConfirmar').textContent = 'Concluir';
  window.modalGsap.abrir($r('modal'), $r('modalPainel'));
}

function inicializarCatalogo() {
  alunoId = new URLSearchParams(window.location.search).get('alunoId');
  if (!alunoId) {
    $r('estadoCatalogo').textContent = 'Aluno não informado. Use ?alunoId=<id>.';
    $r('estadoCatalogo').classList.add('error');
    return;
  }
  window.resgateApi.catalogo(alunoId).then(dados => { catalogo = dados; renderizar(); }).catch(e => {
    $r('estadoCatalogo').textContent = e instanceof TypeError ? 'Backend indisponível. Inicie o servidor Spring Boot.' : e.message;
    $r('estadoCatalogo').classList.add('error');
  });
  $r('buscaVantagem').addEventListener('input', renderizar);
  $r('somenteDisponiveis').addEventListener('change', renderizar);
  $r('gradeVantagens').addEventListener('click', e => {
    const botao = e.target.closest('button[data-id]');
    if (botao) confirmar(catalogo.vantagens.find(v => String(v.id) === botao.dataset.id), botao);
  });
  $r('modalConfirmar').addEventListener('click', async () => {
    if ($r('modalConfirmar').textContent === 'Concluir') return fecharModalR();
    $r('modalConfirmar').disabled = true;
    try {
      const resultado = await window.resgateApi.resgatar(alunoId, vantagemPendente.id);
      catalogo.saldo = resultado.saldoAtual;
      await fecharModalR();
      renderizar();
      sucesso(resultado);
    } catch (e) {
      await fecharModalR();
      $r('estadoCatalogo').hidden = false;
      $r('estadoCatalogo').textContent = e.message;
      $r('estadoCatalogo').classList.add('error');
    } finally { $r('modalConfirmar').disabled = false; }
  });
  $r('modalCancelar').addEventListener('click', fecharModalR);
  $r('modalFechar').addEventListener('click', fecharModalR);
  $r('modal').addEventListener('click', e => { if (e.target === $r('modal')) fecharModalR(); });
  document.addEventListener('keydown', e => { if (e.key === 'Escape' && $r('modal').getAttribute('aria-hidden') === 'false') fecharModalR(); });
}

if (typeof document !== 'undefined') document.addEventListener('DOMContentLoaded', inicializarCatalogo);
if (typeof module !== 'undefined' && module.exports) module.exports = { filtrarVantagens, catalogViewModel };
