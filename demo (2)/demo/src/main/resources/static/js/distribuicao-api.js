const DISTRIBUICAO_API = 'http://localhost:8080/api/professores';

window.distribuicaoApi = {
  contexto: professorId => AppHttp.request(`${DISTRIBUICAO_API}/${professorId}/distribuicao`),
  enviar: (professorId, dados) => AppHttp.request(`${DISTRIBUICAO_API}/${professorId}/envios`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(dados)
  })
};
