const DISTRIBUICAO_API = 'http://localhost:8080/api/professores';

async function tratar(res) {
  if (res.ok) return res.json();
  let mensagem = 'Não foi possível concluir a operação.';
  try { mensagem = (await res.json()).message || mensagem; } catch { /* resposta sem JSON */ }
  throw new Error(mensagem);
}

window.distribuicaoApi = {
  contexto: professorId => fetch(`${DISTRIBUICAO_API}/${professorId}/distribuicao`).then(tratar),
  enviar: (professorId, dados) => fetch(`${DISTRIBUICAO_API}/${professorId}/envios`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(dados)
  }).then(tratar)
};
