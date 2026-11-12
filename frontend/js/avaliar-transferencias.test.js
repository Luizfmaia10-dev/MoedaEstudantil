const test = require('node:test');
const assert = require('node:assert/strict');
const { automationViewModel, automaticDecisionBadge } = require('./avaliar-transferencias.js');

test('automação ativa oferece desativação e informa solicitações processadas', () => {
  assert.deepEqual(automationViewModel({
    ativa: true,
    possuiCriterios: true,
    pendentes: 2
  }), {
    status: 'Automação ativa',
    summary: 'A IA analisa novas solicitações com seus critérios. 2 solicitações continuam pendentes para sua revisão.',
    action: 'Desativar automação',
    nextActive: false,
    disabled: false,
    tone: 'active'
  });
});

test('automação exige critérios antes da ativação', () => {
  const model = automationViewModel({ ativa: false, possuiCriterios: false, pendentes: 4 });

  assert.equal(model.status, 'Critérios necessários');
  assert.equal(model.action, 'Cadastrar critérios');
  assert.equal(model.disabled, true);
  assert.match(model.summary, /4 solicitações pendentes/);
});

test('decisão automática recebe selo de auditoria com confiança', () => {
  assert.equal(automaticDecisionBadge({ avaliacaoAutomatica: false }), '');
  assert.equal(automaticDecisionBadge({
    avaliacaoAutomatica: true,
    confiancaAvaliacaoAutomatica: 0.91
  }), 'Avaliada pela IA · 91% de confiança');
});
