const RESGATE_API = 'http://localhost:8080/api/alunos';

async function tratarResgate(res) {
  if (res.ok) return res.json();
  let mensagem = 'Não foi possível concluir a operação.';
  try { mensagem = (await res.json()).message || mensagem; } catch { /* resposta sem JSON */ }
  throw new Error(mensagem);
}

window.resgateApi = {
  catalogo: alunoId => fetch(`${RESGATE_API}/${alunoId}/vantagens`).then(tratarResgate),
  resgatar: (alunoId, vantagemId) => fetch(`${RESGATE_API}/${alunoId}/resgates/${vantagemId}`, { method: 'POST' }).then(tratarResgate),
  foto: caminho => caminho && (caminho.startsWith('http') ? caminho : `http://localhost:8080${caminho}`)
};
