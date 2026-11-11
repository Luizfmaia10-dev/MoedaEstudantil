const professorId = new URLSearchParams(location.search).get('professorId');
const $d = id => document.getElementById(id);
let contexto;
let envioPendente;

function escapar(texto) {
  const el = document.createElement('div'); el.textContent = texto ?? ''; return el.innerHTML;
}

function erro(mensagem) {
  $d('erroForm').textContent = mensagem; $d('erroForm').hidden = !mensagem;
}

async function fecharModal() {
  await window.modalGsap.fechar($d('modal'), $d('modalPainel'));
}

function abrirConfirmacao(gatilho) {
  const aluno = contexto.alunos.find(item => String(item.id) === envioPendente.alunoId);
  $d('modalTitulo').textContent = 'Confirmar envio';
  $d('modalCorpo').innerHTML = `Você enviará <strong>${envioPendente.quantidade} moedas</strong> para <strong>${escapar(aluno.nome)}</strong>.<br><br>Motivo: ${escapar(envioPendente.motivo)}`;
  $d('modalCancelar').hidden = false; $d('modalConfirmar').hidden = false; $d('modalConfirmar').textContent = 'Confirmar envio';
  window.modalGsap.abrir($d('modal'), $d('modalPainel'), gatilho);
}

function abrirSucesso(resultado) {
  $d('modalTitulo').textContent = 'Moedas enviadas';
  $d('modalCorpo').innerHTML = `A transferência foi concluída e o aluno foi notificado por e-mail.<br><br>Seu novo saldo é <strong>${resultado.saldoProfessor} moedas</strong>.`;
  $d('modalCancelar').hidden = true; $d('modalConfirmar').hidden = false; $d('modalConfirmar').textContent = 'Concluir';
  window.modalGsap.abrir($d('modal'), $d('modalPainel'));
}

async function carregar() {
  if (!professorId) throw new Error('Professor não informado. Use ?professorId=<id>.');
  contexto = await window.distribuicaoApi.contexto(professorId);
  $d('saldoProfessor').textContent = AppFormat.number(contexto.saldo);
  $d('alunoId').innerHTML += contexto.alunos.map(a => `<option value="${a.id}">${escapar(a.nome)} — ${escapar(a.curso || 'Curso não informado')}</option>`).join('');
  $d('estadoPagina').hidden = true; $d('conteudoPagina').hidden = false;
}

document.addEventListener('DOMContentLoaded', () => {
  carregar().catch(e => {
    $d('estadoPagina').textContent = e instanceof TypeError
      ? 'Backend indisponível. Inicie o servidor Spring Boot.' : e.message;
    $d('estadoPagina').classList.add('error');
  });
  $d('formDistribuicao').addEventListener('submit', event => {
    event.preventDefault(); erro('');
    const quantidade = Number($d('quantidade').value); const motivo = $d('motivo').value.trim(); const alunoId = $d('alunoId').value;
    if (!alunoId) return erro('Selecione um aluno.');
    if (!Number.isInteger(quantidade) || quantidade <= 0) return erro('Informe uma quantidade inteira maior que zero.');
    if (quantidade > contexto.saldo) return erro('Saldo insuficiente para este envio.');
    if (!motivo) return erro('Informe o motivo da transferência.');
    envioPendente = { alunoId, quantidade, motivo }; abrirConfirmacao(event.submitter);
  });
  $d('modalConfirmar').addEventListener('click', async () => {
    if ($d('modalConfirmar').textContent === 'Concluir') return fecharModal();
    $d('modalConfirmar').disabled = true;
    try {
      const resultado = await window.distribuicaoApi.enviar(professorId, envioPendente);
      contexto.saldo = resultado.saldoProfessor; $d('saldoProfessor').textContent = AppFormat.number(resultado.saldoProfessor);
      await fecharModal(); $d('formDistribuicao').reset(); abrirSucesso(resultado);
    } catch (e) { await fecharModal(); erro(e.message); }
    finally { $d('modalConfirmar').disabled = false; }
  });
  $d('modalCancelar').addEventListener('click', fecharModal); $d('modalFechar').addEventListener('click', fecharModal);
  $d('modal').addEventListener('click', e => { if (e.target === $d('modal')) fecharModal(); });
  document.addEventListener('keydown', e => { if (e.key === 'Escape' && $d('modal').getAttribute('aria-hidden') === 'false') fecharModal(); });
});
