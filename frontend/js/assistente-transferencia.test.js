const test = require('node:test');
const assert = require('node:assert/strict');
const { createAssistantState, mergeInterpretation, canConfirm, createAssistantController } = require('./assistente-transferencia.js');

test('estado inicial não permite confirmação', () => {
  const state = createAssistantState();
  assert.equal(state.status, 'INICIAL');
  assert.equal(canConfirm(state), false);
  assert.deepEqual(state.draft, {
    destinatarioId: null,
    destinatarioNome: null,
    avaliadorId: null,
    avaliadorNome: null,
    quantidade: null,
    justificativa: null
  });
});

test('interpretação preserva o rascunho e só libera confirmação quando pronta', () => {
  const first = mergeInterpretation(createAssistantState(), {
    estado: 'PRECISA_PROFESSOR', mensagem: 'Qual professor?',
    destinatarioId: 6, destinatarioNome: 'Ana Souza', quantidade: 20,
    avaliadorId: null, avaliadorNome: null, justificativa: null,
    opcoes: [{ tipo: 'PROFESSOR', id: 2, nome: 'Carlos Lima', detalhe: 'Computação' }]
  });
  assert.equal(first.draft.destinatarioId, 6);
  assert.equal(first.draft.quantidade, 20);
  assert.equal(first.options.length, 1);
  assert.equal(canConfirm(first), false);

  const ready = mergeInterpretation(first, {
    estado: 'PRONTO', mensagem: 'Revise os dados.',
    destinatarioId: 6, destinatarioNome: 'Ana Souza', quantidade: 20,
    avaliadorId: 2, avaliadorNome: 'Carlos Lima', justificativa: null, opcoes: []
  });
  assert.equal(ready.draft.destinatarioId, 6);
  assert.equal(ready.draft.avaliadorId, 2);
  assert.equal(canConfirm(ready), true);
});

test('resposta incompleta nunca reutiliza opções antigas', () => {
  const withOptions = { ...createAssistantState(), options: [{ id: 1 }] };
  const next = mergeInterpretation(withOptions, { estado: 'PRECISA_QUANTIDADE', mensagem: 'Quantas?', opcoes: [] });
  assert.deepEqual(next.options, []);
  assert.equal(canConfirm(next), false);
});

test('controller mantém contexto entre mensagens e confirma uma única vez', async () => {
  const interpretedDrafts = [];
  let confirmations = 0;
  let release;
  const api = {
    async interpretar(_alunoId, _message, draft) {
      interpretedDrafts.push({ ...draft });
      return interpretedDrafts.length === 1
        ? { estado: 'PRECISA_PROFESSOR', mensagem: 'Qual professor?', destinatarioId: 6,
          destinatarioNome: 'Ana', quantidade: 20, opcoes: [] }
        : { estado: 'PRONTO', mensagem: 'Revise.', destinatarioId: 6, destinatarioNome: 'Ana',
          avaliadorId: 2, avaliadorNome: 'Carlos', quantidade: 20, opcoes: [] };
    },
    solicitar() {
      confirmations += 1;
      return new Promise(resolve => { release = resolve; });
    }
  };
  const controller = createAssistantController({ api, alunoId: 4 });
  await controller.send('vinte para Ana');
  await controller.send('pelo Carlos');
  assert.equal(interpretedDrafts[1].destinatarioId, 6);
  assert.equal(controller.getState().status, 'PRONTO');

  const pending = controller.confirm();
  await controller.confirm();
  assert.equal(confirmations, 1);
  release({ id: 10, status: 'PENDENTE' });
  await pending;
  assert.equal(controller.getState().status, 'SUCESSO');
});

test('controller sinaliza indisponibilidade e mantém fallback manual', async () => {
  const error = Object.assign(new Error('offline'), { status: 503 });
  const controller = createAssistantController({
    alunoId: 4,
    api: { interpretar: async () => { throw error; }, solicitar: async () => ({}) }
  });
  await controller.send('envie dez para Ana');
  assert.equal(controller.getState().status, 'INDISPONIVEL');
  assert.match(controller.getState().message, /formulário manual/i);
});

test('página oferece CTA Star Border, diálogo centralizado e entrada por voz', t => {
  const { readFileSync } = require('node:fs');
  const { resolve } = require('node:path');
  const { pathToFileURL } = require('node:url');
  const { browserResult } = require('../../test-support/chromium.cjs');
  const pagePath = resolve(__dirname, '../solicitar-transferencia.html');
  let page = readFileSync(pagePath, 'utf8').replace(/<script\b[^>]*>[\s\S]*?<\/script>/g, '');
  for (const stylesheet of ['styles.css', 'interactions.css', 'assistente-transferencia.css']) {
    page = page.replace(`css/${stylesheet}`, pathToFileURL(resolve(__dirname, `../css/${stylesheet}`)).href);
  }
  const result = browserResult(t, page.replace('</body>', `<pre id="test-result"></pre><script>
    const button = document.getElementById('abrirAssistenteIA');
    const dialog = document.getElementById('assistenteDialog');
    const live = document.getElementById('assistenteMensagens');
    const confirm = document.getElementById('assistenteConfirmar');
    const send = document.getElementById('assistenteEnviar');
    const voice = document.getElementById('assistenteVoz');
    const thought = document.getElementById('assistentePensando');
    dialog.showModal();
    const rect = dialog.getBoundingClientRect();
    document.getElementById('test-result').textContent = JSON.stringify({
      button: Boolean(button), canvas: Boolean(button?.querySelector('canvas.star-border-light')),
      minWidth: button ? parseFloat(getComputedStyle(button).minWidth) : 0,
      minHeight: button ? parseFloat(getComputedStyle(button).minHeight) : 0,
      dialogLabel: dialog?.getAttribute('aria-labelledby'), live: live?.getAttribute('aria-live'),
      confirmDisabled: confirm?.disabled, voice: Boolean(voice), thought: Boolean(thought),
      sendCanvas: Boolean(send?.querySelector('canvas.star-border-light')),
      sendTouchWidth: send ? parseFloat(getComputedStyle(send).width) : 0,
      sendBackground: send ? getComputedStyle(send).backgroundColor : '',
      sendColor: send ? getComputedStyle(send).color : '',
      backdropFilter: getComputedStyle(dialog, '::backdrop').backdropFilter,
      centered: Math.abs((rect.left + rect.width / 2) - innerWidth / 2) < 2,
      insideViewport: rect.left >= 0 && rect.right <= innerWidth && rect.top >= 0 && rect.bottom <= innerHeight,
      overflow: document.documentElement.scrollWidth > innerWidth
    });
  </script></body>`));
  if (!result) return;
  assert.equal(result.button, true);
  assert.equal(result.canvas, true);
  assert.ok(result.minWidth >= 44);
  assert.ok(result.minHeight >= 44);
  assert.equal(result.dialogLabel, 'assistenteTitulo');
  assert.equal(result.live, 'polite');
  assert.equal(result.confirmDisabled, true);
  assert.equal(result.voice, true);
  assert.equal(result.thought, true);
  assert.equal(result.sendCanvas, true);
  assert.ok(result.sendTouchWidth >= 44);
  assert.equal(result.sendBackground, 'rgb(12, 12, 15)');
  assert.equal(result.sendColor, 'rgb(184, 255, 106)');
  assert.match(result.backdropFilter, /blur\((?:1[2-9]|[2-9]\d)px\)/);
  assert.equal(result.centered, true);
  assert.equal(result.insideViewport, true);
  assert.equal(result.overflow, false);
});
