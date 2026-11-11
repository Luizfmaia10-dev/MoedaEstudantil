const test = require('node:test');
const assert = require('node:assert/strict');

test('Thought Line formata o cronômetro e encerra o estado de pensamento', () => {
  const { formatElapsed, createThoughtController } = require('./thought-line.js');
  const states = [];
  let now = 0;
  const controller = createThoughtController({ now: () => now, onState: state => states.push(state) });

  controller.start(['Entendendo seu pedido', 'Validando os dados']);
  now = 1450;
  const settled = controller.settle();

  assert.equal(formatElapsed(1450), '1.4s');
  assert.equal(states[0].working, true);
  assert.equal(settled.working, false);
  assert.equal(settled.elapsed, '1.4s');
});

test('Voice Pill entrega a transcrição em português e informa indisponibilidade', () => {
  const { createVoiceController } = require('./voice-pill.js');
  const transcripts = [];
  const states = [];
  const recognition = {
    start() { this.onstart(); },
    stop() { this.onend(); }
  };
  const controller = createVoiceController({
    recognitionFactory: () => recognition,
    onTranscript: text => transcripts.push(text),
    onState: state => states.push(state)
  });

  controller.start();
  recognition.onresult({ results: [[{ transcript: 'mande vinte moedas para Ana' }]] });
  controller.stop();

  assert.equal(recognition.lang, 'pt-BR');
  assert.deepEqual(transcripts, ['mande vinte moedas para Ana']);
  assert.equal(states.some(state => state.listening), true);
  assert.equal(states.at(-1).listening, false);

  const unsupported = createVoiceController({ recognitionFactory: () => null });
  assert.equal(unsupported.start().reason, 'unsupported');
});
