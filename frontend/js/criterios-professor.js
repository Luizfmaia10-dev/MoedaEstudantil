/**
 * RF-011 - Tela do professor: edita o texto de critérios de transferência.
 * Lê o professor da URL (criterios-professor.html?professorId=1).
 * TODO: trocar o professorId da URL pela sessão quando o login existir.
 */

const LIMITE = 1000;
const $ = id => document.getElementById(id);

function atualizarContador() {
  const tamanho = $('criterios').value.length;
  const el = $('contador');
  el.textContent = `${tamanho}/${LIMITE}`;
  el.classList.toggle('excedido', tamanho > LIMITE);
  $('btnSalvar').disabled = tamanho > LIMITE;
}

function mensagem(texto, tipo) {
  const el = $('msg');
  el.textContent = texto || '';
  el.className = tipo || '';
}

function erroGeral(err) {
  $('loading').style.display = 'none';
  $('conteudo').style.display = 'none';
  const el = $('erroGeral');
  el.textContent = err instanceof TypeError
    ? 'Backend indisponível. Inicie o servidor Spring Boot (porta 8080).'
    : (typeof err === 'string' ? err : err.message);
  el.style.display = 'block';
}

async function salvar(professorId) {
  mensagem('');
  $('btnSalvar').disabled = true;
  try {
    const dto = await window.criterioService.salvar(professorId, $('criterios').value);
    $('criterios').value = dto.criterios || '';
    atualizarContador();
    mensagem('Critérios salvos com sucesso.', 'ok');
  } catch (err) {
    mensagem(err instanceof TypeError ? 'Backend indisponível.' : err.message, 'erro');
    atualizarContador();
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  const professorId = new URLSearchParams(window.location.search).get('professorId');
  if (!professorId) return erroGeral('Professor não informado. Acesse com ?professorId=<id> na URL.');

  try {
    const dto = await window.criterioService.buscarDoProfessor(professorId);
    $('professorInfo').textContent = `${dto.professorNome} — ${dto.departamento || ''}`;
    $('criterios').value = dto.criterios || '';
    $('loading').style.display = 'none';
    $('conteudo').style.display = 'block';
    atualizarContador();
  } catch (err) {
    return erroGeral(err);
  }

  $('criterios').addEventListener('input', () => { mensagem(''); atualizarContador(); });
  $('btnSalvar').addEventListener('click', () => salvar(professorId));
});
