const alunoId = new URLSearchParams(location.search).get('alunoId');
const $r = id => document.getElementById(id);
let catalogo;
let vantagemPendente;

function escaparR(texto) { const el = document.createElement('div'); el.textContent = texto ?? ''; return el.innerHTML; }
async function fecharModalR() { await window.modalGsap.fechar($r('modal'), $r('modalPainel')); }

function renderizar() {
  $r('saldoAluno').textContent = AppFormat.number(catalogo.saldo);
  if (!catalogo.vantagens.length) { $r('estadoCatalogo').textContent = 'Nenhuma vantagem disponível no momento.'; return; }
  $r('estadoCatalogo').hidden = true; $r('gradeVantagens').hidden = false;
  $r('gradeVantagens').innerHTML = catalogo.vantagens.map(v => {
    const foto = window.resgateApi.foto(v.fotoUrl);
    return `<li class="benefit-card">${foto ? `<img src="${escaparR(foto)}" alt="${escaparR(v.nome)}">` : '<div class="benefit-placeholder" aria-hidden="true">ME</div>'}<div class="benefit-body"><span class="benefit-company">${escaparR(v.empresaNome)}</span><h2>${escaparR(v.nome)}</h2><p>${escaparR(v.descricao)}</p></div><div class="benefit-footer"><span class="benefit-cost">${v.custoMoedas} moedas</span><button class="btn-primary" type="button" data-id="${v.id}" ${v.custoMoedas > catalogo.saldo ? 'disabled title="Saldo insuficiente"' : ''}>Resgatar</button></div></li>`;
  }).join('');
}

function confirmar(vantagem, gatilho) {
  vantagemPendente = vantagem; $r('modalTitulo').textContent = 'Confirmar resgate';
  $r('modalCorpo').innerHTML = `Resgatar <strong>${escaparR(vantagem.nome)}</strong> por <strong>${vantagem.custoMoedas} moedas</strong>? O cupom será exibido na tela após a confirmação.`;
  $r('modalCancelar').hidden = false; $r('modalConfirmar').hidden = false; $r('modalConfirmar').textContent = 'Confirmar resgate';
  window.modalGsap.abrir($r('modal'), $r('modalPainel'), gatilho);
}

function sucesso(resultado) {
  $r('modalTitulo').textContent = 'Resgate confirmado';
  $r('modalCorpo').innerHTML = `Seu cupom foi gerado. Guarde este código para apresentar à empresa parceira.<span class="coupon-code">${escaparR(resultado.codigoCupom)}</span>`;
  $r('modalCancelar').hidden = true; $r('modalConfirmar').hidden = false; $r('modalConfirmar').textContent = 'Concluir';
  window.modalGsap.abrir($r('modal'), $r('modalPainel'));
}

document.addEventListener('DOMContentLoaded', () => {
  if (!alunoId) { $r('estadoCatalogo').textContent = 'Aluno não informado. Use ?alunoId=<id>.'; $r('estadoCatalogo').classList.add('error'); return; }
  window.resgateApi.catalogo(alunoId).then(dados => { catalogo = dados; renderizar(); }).catch(e => {
    $r('estadoCatalogo').textContent = e instanceof TypeError
      ? 'Backend indisponível. Inicie o servidor Spring Boot.' : e.message;
    $r('estadoCatalogo').classList.add('error');
  });
  $r('gradeVantagens').addEventListener('click', e => { const botao = e.target.closest('button[data-id]'); if (botao) confirmar(catalogo.vantagens.find(v => String(v.id) === botao.dataset.id), botao); });
  $r('modalConfirmar').addEventListener('click', async () => {
    if ($r('modalConfirmar').textContent === 'Concluir') return fecharModalR();
    $r('modalConfirmar').disabled = true;
    try { const resultado = await window.resgateApi.resgatar(alunoId, vantagemPendente.id); catalogo.saldo = resultado.saldoAtual; await fecharModalR(); renderizar(); sucesso(resultado); }
    catch (e) { await fecharModalR(); $r('estadoCatalogo').hidden = false; $r('estadoCatalogo').textContent = e.message; $r('estadoCatalogo').classList.add('error'); }
    finally { $r('modalConfirmar').disabled = false; }
  });
  $r('modalCancelar').addEventListener('click', fecharModalR); $r('modalFechar').addEventListener('click', fecharModalR);
  $r('modal').addEventListener('click', e => { if (e.target === $r('modal')) fecharModalR(); });
  document.addEventListener('keydown', e => { if (e.key === 'Escape' && $r('modal').getAttribute('aria-hidden') === 'false') fecharModalR(); });
});
