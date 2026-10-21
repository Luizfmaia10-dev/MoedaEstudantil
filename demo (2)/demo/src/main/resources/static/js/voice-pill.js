(function voicePillModule(root, factory) {
  'use strict';
  const exports = factory();
  if (typeof module !== 'undefined' && module.exports) module.exports = exports;
  if (root) root.VoicePill = exports;
})(typeof window !== 'undefined' ? window : globalThis, function voicePillFactory() {
  const LOOP = 4.8;
  const SYLLABLES = [[.1, .16, .9], [.3, .12, .7], [.5, .2, 1], [.95, .14, .8], [1.15, .1, .6], [1.3, .22, .95], [1.9, .16, .85], [2.12, .12, .7], [2.3, .18, .9], [2.55, .1, .5], [3.05, .24, 1], [3.4, .12, .75], [3.6, .16, .9]];

  function simulatedLevel(time) {
    const current = time % LOOP;
    let level = .06;
    for (const [start, duration, peak] of SYLLABLES) {
      const progress = (current - start) / duration;
      if (progress >= 0 && progress <= 1) level = Math.max(level, peak * .5 * (1 - Math.cos(2 * Math.PI * progress)));
    }
    return level * (.7 + .3 * Math.abs(Math.sin(2 * Math.PI * 7.1 * current)));
  }

  function createVoiceController({ recognitionFactory, onTranscript = () => {}, onState = () => {} } = {}) {
    let recognition = null;
    let state = { listening: false, reason: 'idle' };
    const publish = next => { state = next; onState(state); return state; };
    const prepare = () => {
      if (recognition) return recognition;
      recognition = recognitionFactory?.() || null;
      if (!recognition) return null;
      recognition.lang = 'pt-BR';
      recognition.continuous = false;
      recognition.interimResults = false;
      recognition.onstart = () => publish({ listening: true, reason: 'listening' });
      recognition.onend = () => publish({ listening: false, reason: 'ended' });
      recognition.onerror = event => publish({ listening: false, reason: event?.error || 'error' });
      recognition.onresult = event => {
        const text = Array.from(event.results || [])
          .map(result => result?.[0]?.transcript || '').join(' ').trim();
        if (text) onTranscript(text);
      };
      return recognition;
    };
    return {
      start() {
        const service = prepare();
        if (!service) return publish({ listening: false, reason: 'unsupported' });
        try { service.start(); } catch (error) { return publish({ listening: false, reason: 'error' }); }
        return state;
      },
      stop(reason = 'stop') {
        if (!recognition || !state.listening) return state;
        try { recognition.stop(); } catch (error) { return publish({ listening: false, reason }); }
        return state;
      },
      cancel() {
        if (recognition?.abort) recognition.abort();
        return publish({ listening: false, reason: 'cancel' });
      },
      getState: () => state
    };
  }

  const formatClock = milliseconds => {
    const seconds = Math.floor(milliseconds / 1000);
    return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`;
  };

  function mountVoicePill(button, { onTranscript = () => {}, onUnavailable = () => {} } = {}) {
    if (!button) return null;
    const view = button.ownerDocument.defaultView;
    const Recognition = view.SpeechRecognition || view.webkitSpeechRecognition;
    const canvas = button.querySelector('.voice-pill-wave');
    const time = button.querySelector('.voice-pill-time');
    const reduced = view.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    let startedAt = 0;
    let raf = 0;
    let history = [];
    let pointerId = null;
    let downX = 0;
    let downAt = 0;
    let ownPress = false;

    const draw = now => {
      if (button.dataset.state !== 'listening') return;
      if (time) time.textContent = formatClock(now - startedAt);
      if (canvas && !reduced) {
        const rect = canvas.getBoundingClientRect();
        const dpr = Math.min(2, view.devicePixelRatio || 1);
        const width = Math.max(1, Math.round(rect.width * dpr));
        const height = Math.max(1, Math.round(rect.height * dpr));
        if (canvas.width !== width || canvas.height !== height) { canvas.width = width; canvas.height = height; }
        history.push(simulatedLevel((now - startedAt) / 1000));
        if (history.length > 22) history.shift();
        const context = canvas.getContext('2d');
        context.clearRect(0, 0, width, height);
        context.fillStyle = '#173300';
        history.forEach((level, index) => {
          const barWidth = 2 * dpr;
          const x = width - (history.length - index) * 3 * dpr;
          const barHeight = Math.max(barWidth, (.1 + .9 * level) * height);
          context.globalAlpha = .35 + .65 * level;
          context.beginPath();
          context.roundRect(x, (height - barHeight) / 2, barWidth, barHeight, barWidth / 2);
          context.fill();
        });
        context.globalAlpha = 1;
      }
      raf = view.requestAnimationFrame(draw);
    };

    const controller = createVoiceController({
      recognitionFactory: () => Recognition ? new Recognition() : null,
      onTranscript,
      onState(state) {
        button.dataset.state = state.listening ? 'listening' : 'idle';
        button.setAttribute('aria-pressed', String(state.listening));
        button.setAttribute('aria-label', state.listening ? 'Parar gravação' : 'Falar pedido de transferência');
        if (state.listening) {
          startedAt = view.performance.now();
          history = [];
          if (time) time.textContent = '0:00';
          view.cancelAnimationFrame(raf);
          raf = view.requestAnimationFrame(draw);
        } else {
          view.cancelAnimationFrame(raf);
          if (state.reason === 'unsupported') onUnavailable('Seu navegador não oferece transcrição por voz. Digite o pedido abaixo.');
          if (state.reason === 'not-allowed' || state.reason === 'service-not-allowed') onUnavailable('Permita o acesso ao microfone para usar a transferência por voz.');
        }
      }
    });

    button.addEventListener('pointerdown', event => {
      if (event.button !== 0 || !event.isPrimary) return;
      pointerId = event.pointerId;
      downX = event.clientX;
      downAt = view.performance.now();
      ownPress = !controller.getState().listening;
      button.setPointerCapture?.(pointerId);
      if (ownPress) controller.start();
    });
    button.addEventListener('pointermove', event => {
      if (event.pointerId !== pointerId || !controller.getState().listening || !ownPress) return;
      const distance = Math.max(0, downX - event.clientX);
      const progress = Math.min(1, distance / 64);
      button.style.setProperty('--vp-slide', `${-Math.min(88, distance)}px`);
      button.style.setProperty('--vp-cancel', progress.toFixed(3));
      if (progress >= 1) controller.cancel();
    });
    const release = event => {
      if (event.pointerId !== pointerId) return;
      pointerId = null;
      button.style.setProperty('--vp-slide', '0px');
      button.style.setProperty('--vp-cancel', '0');
      const held = view.performance.now() - downAt;
      if (controller.getState().listening && (!ownPress || held >= 300)) controller.stop('release');
    };
    button.addEventListener('pointerup', release);
    button.addEventListener('pointercancel', release);
    button.addEventListener('keydown', event => {
      if (event.key === 'Escape') { controller.cancel(); return; }
      if ((event.key === 'Enter' || event.key === ' ') && !event.repeat) {
        event.preventDefault();
        if (controller.getState().listening) controller.stop('key'); else controller.start();
      }
    });
    button.addEventListener('click', event => {
      if (event.detail !== 0) return;
      if (controller.getState().listening) controller.stop('key'); else controller.start();
    });
    return controller;
  }

  return { simulatedLevel, createVoiceController, mountVoicePill };
});
