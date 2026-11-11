const RESGATE_API = 'http://localhost:8080/api/alunos';

window.resgateApi = {
  catalogo: alunoId => AppHttp.request(`${RESGATE_API}/${alunoId}/vantagens`),
  resgatar: (alunoId, vantagemId) => AppHttp.request(`${RESGATE_API}/${alunoId}/resgates/${vantagemId}`, { method: 'POST' }),
  foto: caminho => caminho && (caminho.startsWith('http') ? caminho : `http://localhost:8080${caminho}`)
};
