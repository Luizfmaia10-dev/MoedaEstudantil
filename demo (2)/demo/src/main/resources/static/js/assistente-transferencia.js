(function initializeAssistantModule(root, factory) {
  'use strict';

  const exports = factory();
  if (typeof module !== 'undefined' && module.exports) module.exports = exports;
  if (root) root.TransferAssistant = exports;
})(typeof window !== 'undefined' ? window : globalThis, function assistantFactory() {
  const EMPTY_DRAFT = Object.freeze({
    destinatarioId: null,
    destinatarioNome: null,
    avaliadorId: null,
    avaliadorNome: null,
    quantidade: null,
    justificativa: null
  });

  function createAssistantState() {
    return { status: 'INICIAL', message: '', draft: { ...EMPTY_DRAFT }, options: [], busy: false };
  }

  function mergeInterpretation(state, response) {
    const draft = { ...state.draft };
    for (const key of Object.keys(EMPTY_DRAFT)) {
      if (Object.prototype.hasOwnProperty.call(response, key)) draft[key] = response[key];
    }
    return {
      ...state,
      status: response.estado || 'INICIAL',
      message: response.mensagem || '',
      draft,
      options: Array.isArray(response.opcoes) ? response.opcoes : [],
      busy: false
    };
  }

  function canConfirm(state) {
    const draft = state?.draft || {};
    return state?.status === 'PRONTO'
      && Number.isInteger(draft.quantidade) && draft.quantidade > 0
      && Boolean(draft.destinatarioId) && Boolean(draft.avaliadorId);
  }

  function createAssistantController({ api, alunoId, onState = () => {}, onConfirmed = () => {} }) {
    let state = createAssistantState();

    const publish = next => {
      state = next;
      onState(state);
      return state;
    };

    async function send(message) {
      const clean = String(message || '').trim();
      if (!clean || state.busy) return state;
      publish({ ...state, status: 'INTERPRETANDO', message: '', busy: true, options: [] });
      try {
        const response = await api.interpretar(alunoId, clean, state.draft);
        return publish(mergeInterpretation(state, response));
      } catch (error) {
        const unavailable = error?.status === 503 || error instanceof TypeError;
        return publish({
          ...state,
          status: unavailable ? 'INDISPONIVEL' : 'ERRO',
          message: unavailable
            ? 'A IA local está indisponível. Você ainda pode usar o formulário manual.'
            : (error?.message || 'Não consegui interpretar essa mensagem. Tente novamente.'),
          busy: false,
          options: []
        });
      }
    }

    async function choose(option) {
      if (!option || state.busy) return state;
      const draft = { ...state.draft };
      if (option.tipo === 'DESTINATARIO') {
        draft.destinatarioId = option.id;
        draft.destinatarioNome = option.nome;
      } else if (option.tipo === 'PROFESSOR') {
        draft.avaliadorId = option.id;
        draft.avaliadorNome = option.nome;
      }
      state = { ...state, draft, options: [] };
      return send(`Escolhi ${option.nome}. Continue com estes dados.`);
    }

    async function confirm() {
      if (!canConfirm(state) || state.busy) return null;
      const payload = {
        destinatarioId: state.draft.destinatarioId,
        avaliadorId: state.draft.avaliadorId,
        quantidade: state.draft.quantidade,
        justificativa: state.draft.justificativa || ''
      };
      publish({ ...state, status: 'CONFIRMANDO', message: '', busy: true });
      try {
        const result = await api.solicitar(alunoId, payload);
        publish({ ...state, status: 'SUCESSO', message: 'Solicitação enviada para avaliação!', busy: false });
        await onConfirmed(result);
        return result;
      } catch (error) {
        publish({ ...state, status: 'ERRO', message: error?.message || 'Não foi possível criar a solicitação.', busy: false });
        return null;
      }
    }

    return { getState: () => state, send, choose, confirm };
  }

  function initializeTransferAssistant({ document, api, alunoId, modalAnimator, starBorder, thoughtLine, voicePill,
    onConfirmed = () => {}, onUseManual = () => {} }) {
    const byId = id => document.getElementById(id);
    const openButton = byId('abrirAssistenteIA');
    const dialog = byId('assistenteDialog');
    const input = byId('assistenteInput');
    const composer = byId('assistenteComposer');
    const sendButton = byId('assistenteEnviar');
    const messages = byId('assistenteMensagens');
    const options = byId('assistenteOpcoes');
    const summary = byId('assistenteResumo');
    const confirmButton = byId('assistenteConfirmar');
    if (!openButton || !dialog || !input || !composer || !messages || !options || !summary || !confirmButton) return null;

    starBorder?.createStarBorder(openButton, {
      color: '#b8ff6a', trailColor: '#63e6a6', duration: 4, direction: 'clockwise',
      stars: 1, trailLength: .3, thickness: 1, radius: 18, glow: .85,
      hover: 'lap', sparkle: true, clickPulse: true, theme: 'dark'
    });
    if (sendButton) {
      starBorder?.createStarBorder(sendButton, {
        color: '#f1ffdc', trailColor: '#63e6a6', duration: 2.8, direction: 'clockwise',
        stars: 1, trailLength: .34, thickness: 1.2, radius: 12, glow: .95,
        hover: 'lap', sparkle: true, clickPulse: true, theme: 'dark',
        backgroundColor: '#0c0c0f', textColor: '#b8ff6a', borderColor: 'rgb(184 255 106 / .2)'
      });
    }

    const thinking = byId('assistentePensando');
    const thought = thoughtLine?.mountThoughtLine(thinking);
    let thinkingHideTimer = 0;
    let voiceController = null;
    let lastAssistantMessage = '';
    function appendMessage(text, type = 'assistant') {
      if (!text) return;
      const bubble = document.createElement('div');
      bubble.className = `ai-message ai-message--${type}`;
      const label = document.createElement('span');
      label.className = 'ai-message-label';
      label.textContent = type === 'user' ? 'Você' : 'Assistente';
      const paragraph = document.createElement('p');
      paragraph.textContent = text;
      bubble.append(label, paragraph);
      messages.appendChild(bubble);
      messages.scrollTop = messages.scrollHeight;
    }

    function showTyping(show) {
      if (!thinking || !thought) return;
      document.defaultView?.clearTimeout(thinkingHideTimer);
      if (show) {
        thinking.hidden = false;
        messages.appendChild(thinking);
        thought.start(['Entendendo seu pedido', 'Validando destinatário e quantidade']);
        messages.scrollTop = messages.scrollHeight;
        return;
      }
      if (thinking.hidden) return;
      thought.settle();
      thinkingHideTimer = document.defaultView?.setTimeout(() => { thinking.hidden = true; }, 350);
    }

    function renderOptions(items) {
      options.replaceChildren();
      for (const option of items) {
        const button = document.createElement('button');
        button.type = 'button';
        button.textContent = option.detalhe ? `${option.nome} · ${option.detalhe}` : option.nome;
        button.addEventListener('click', () => controller.choose(option));
        options.appendChild(button);
      }
    }

    function render(state) {
      showTyping(state.status === 'INTERPRETANDO');
      if (state.message && state.message !== lastAssistantMessage) {
        lastAssistantMessage = state.message;
        appendMessage(state.message, state.status === 'ERRO' || state.status === 'INDISPONIVEL' ? 'error' : 'assistant');
      }
      renderOptions(state.options);
      const ready = canConfirm(state) || state.status === 'CONFIRMANDO' || state.status === 'SUCESSO';
      summary.hidden = !ready;
      byId('assistenteResumoDestinatario').textContent = state.draft.destinatarioNome || '—';
      byId('assistenteResumoQuantidade').textContent = state.draft.quantidade ?? '—';
      byId('assistenteResumoProfessor').textContent = state.draft.avaliadorNome || '—';
      byId('assistenteResumoJustificativa').textContent = state.draft.justificativa || 'Não informada';
      confirmButton.disabled = !canConfirm(state) || state.busy;
      confirmButton.textContent = state.status === 'CONFIRMANDO' ? 'Enviando solicitação…'
        : state.status === 'SUCESSO' ? 'Solicitação enviada ✓' : 'Confirmar solicitação';
      input.disabled = state.busy;
      if (sendButton) sendButton.disabled = state.busy;
      const voiceButton = byId('assistenteVoz');
      if (voiceButton) voiceButton.disabled = state.busy;
    }

    const controller = createAssistantController({ api, alunoId, onState: render, onConfirmed });
    voiceController = voicePill?.mountVoicePill(byId('assistenteVoz'), {
      onTranscript(text) {
        input.value = text;
        composer.requestSubmit();
      },
      onUnavailable(message) { appendMessage(message, 'error'); }
    });
    const open = () => modalAnimator?.openDialog
      ? modalAnimator.openDialog(dialog, openButton, input)
      : (dialog.showModal(), input.focus());
    const close = () => {
      voiceController?.stop('close');
      return modalAnimator?.closeDialog
        ? modalAnimator.closeDialog(dialog)
        : (dialog.close(), openButton.focus());
    };

    openButton.addEventListener('click', open);
    byId('fecharAssistenteIA')?.addEventListener('click', close);
    dialog.addEventListener('cancel', event => { event.preventDefault(); close(); });
    dialog.addEventListener('click', event => { if (event.target === dialog) close(); });
    composer.addEventListener('submit', async event => {
      event.preventDefault();
      const message = input.value.trim();
      if (!message) return;
      appendMessage(message, 'user');
      input.value = '';
      await controller.send(message);
    });
    input.addEventListener('keydown', event => {
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        composer.requestSubmit();
      }
    });
    for (const suggestion of document.querySelectorAll('[data-ai-suggestion]')) {
      suggestion.addEventListener('click', () => {
        input.value = suggestion.dataset.aiSuggestion;
        input.focus();
      });
    }
    confirmButton.addEventListener('click', () => controller.confirm());
    byId('assistenteManual')?.addEventListener('click', async () => {
      onUseManual(controller.getState().draft);
      await close();
    });
    render(controller.getState());
    return controller;
  }

  return { createAssistantState, mergeInterpretation, canConfirm, createAssistantController, initializeTransferAssistant };
});
