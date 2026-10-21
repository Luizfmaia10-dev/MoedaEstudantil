(function initializeRoutes(root) {
  const CATALOG = {
    ALUNO: [
      { title: 'Resgate de Vantagens', description: 'Troque moedas por vantagens das empresas parceiras.', eyebrow: 'ALUNO', href: id => `resgatar-vantagens.html?alunoId=${id}` },
      { title: 'Consulta de Extrato', description: 'Consulte recebimentos e trocas realizadas.', eyebrow: 'ALUNO', href: id => `extrato.html?usuarioId=${id}` },
      { title: 'Transferência de Moedas', description: 'Solicite transferência de moedas para outro aluno.', eyebrow: 'ALUNO', href: id => `solicitar-transferencia.html?alunoId=${id}` },
      { title: 'Critérios de Transferência', description: 'Consulte os critérios definidos pelos professores.', eyebrow: 'ALUNO', href: id => `criterios-transferencia.html?alunoId=${id}` }
    ],
    PROFESSOR: [
      { title: 'Distribuição de Moedas', description: 'Envie moedas aos alunos com um motivo.', eyebrow: 'PROFESSOR', href: id => `distribuir-moedas.html?professorId=${id}` },
      { title: 'Consulta de Extrato', description: 'Consulte seu saldo e histórico de envios.', eyebrow: 'PROFESSOR', href: id => `extrato.html?usuarioId=${id}` },
      { title: 'Critérios de Transferência', description: 'Cadastre critérios para transferências entre alunos.', eyebrow: 'PROFESSOR', href: id => `criterios-professor.html?professorId=${id}` },
      { title: 'Gestão de Transferências', description: 'Avalie solicitações de transferência.', eyebrow: 'PROFESSOR', href: id => `avaliar-transferencias.html?professorId=${id}` }
    ],
    EMPRESA: [
      { title: 'Gestão de Vantagens', description: 'Cadastre e gerencie as vantagens oferecidas.', eyebrow: 'EMPRESA', href: id => `vantagens.html?empresaId=${id}` }
    ]
  };

  function dashboard() {
    return 'painel.html';
  }

  function cardsFor(session) {
    if (!session || !CATALOG[session.perfil] || !Number.isFinite(Number(session.id))) {
      throw new Error('Perfil inválido.');
    }
    const id = Number(session.id);
    return CATALOG[session.perfil].map(card => ({
      title: card.title,
      description: card.description,
      eyebrow: card.eyebrow,
      href: card.href(id)
    }));
  }

  const api = { dashboard, cardsFor };
  root.AppRoutes = api;
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
})(typeof window !== 'undefined' ? window : globalThis);
