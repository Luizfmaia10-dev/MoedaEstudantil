/**
 * RF-011 - Tela do aluno: consulta (somente leitura) dos critérios dos professores da sua instituição.
 * Lê o aluno da URL (criterios-transferencia.html?alunoId=4).
 * TODO: trocar o alunoId da URL pela sessão quando o login existir.
 */

const $ = id => document.getElementById(id);
let professores = [];

function escapeHtml(texto) {
  const div = document.createElement('div');
  div.textContent = texto ?? '';
  return div.innerHTML;
}

function erro(err) {
  $('loading').style.display = 'none';
  const el = $('erro');
  el.textContent = err instanceof TypeError
    ? 'Backend indisponível. Inicie o servidor Spring Boot (porta 8080).'
    : (typeof err === 'string' ? err : err.message);
  el.style.display = 'block';
}

function renderizar() {
  const termo = $('busca').value.trim().toLowerCase();
  const filtrados = professores.filter(p =>
    `${p.professorNome} ${p.departamento || ''}`.toLowerCase().includes(termo));

  $('vazio').style.display = !professores.length ? 'block' : 'none';
  $('lista').innerHTML = filtrados.map(p => `
    <li class="prof-card">
      <h3>${escapeHtml(p.professorNome)}</h3>
      <div class="depto">${escapeHtml(p.departamento)}</div>
      <p>${escapeHtml(p.criterios)}</p>
    </li>`).join('');
}

document.addEventListener('DOMContentLoaded', async () => {
  const alunoId = new URLSearchParams(window.location.search).get('alunoId');
  if (!alunoId) return erro('Aluno não informado. Acesse com ?alunoId=<id> na URL.');

  try {
    professores = await window.criterioService.listarParaAluno(alunoId);
    $('loading').style.display = 'none';
    if (professores.length) $('busca').style.display = 'block';
    renderizar();
  } catch (err) {
    return erro(err);
  }
  $('busca').addEventListener('input', renderizar);
});
