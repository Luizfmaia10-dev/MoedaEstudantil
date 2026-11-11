(function thoughtLineModule(root, factory) {
  'use strict';
  const exports = factory();
  if (typeof module !== 'undefined' && module.exports) module.exports = exports;
  if (root) root.ThoughtLine = exports;
})(typeof window !== 'undefined' ? window : globalThis, function thoughtLineFactory() {
  const formatElapsed = milliseconds => {
    const deciseconds = Math.max(0, Math.floor(milliseconds / 100));
    if (deciseconds < 600) return `${(deciseconds / 10).toFixed(1)}s`;
    return `${Math.floor(deciseconds / 600)}m ${((deciseconds % 600) / 10).toFixed(1)}s`;
  };

  function createThoughtController({ now = () => performance.now(), onState = () => {} } = {}) {
    let startedAt = 0;
    let state = { working: false, elapsed: '0.0s', steps: [] };
    const publish = next => { state = next; onState(state); return state; };
    return {
      start(steps = []) {
        startedAt = now();
        return publish({ working: true, elapsed: '0.0s', steps: [...steps] });
      },
      tick() {
        if (!state.working) return state;
        return publish({ ...state, elapsed: formatElapsed(now() - startedAt) });
      },
      settle() {
        return publish({ ...state, working: false, elapsed: formatElapsed(now() - startedAt) });
      },
      getState: () => state
    };
  }

  function mountThoughtLine(container) {
    if (!container) return null;
    const document = container.ownerDocument;
    const root = document.createElement('div');
    root.className = 'thought-line';
    root.dataset.working = 'false';
    root.innerHTML = '<div class="thought-line-head"><svg class="thought-line-glyph" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="m12 3 1.4 4.1L17.5 8.5l-4.1 1.4L12 14l-1.4-4.1-4.1-1.4 4.1-1.4L12 3Z"/><path d="m18.5 14 .8 2.2 2.2.8-2.2.8-.8 2.2-.8-2.2-2.2-.8 2.2-.8.8-2.2Z"/></svg><span class="thought-line-label">Pensando…</span><span class="thought-line-timer">0.0s</span></div><div class="thought-line-steps"></div><span class="sr-only" role="status"></span>';
    container.appendChild(root);
    const label = root.querySelector('.thought-line-label');
    const timer = root.querySelector('.thought-line-timer');
    const stepsNode = root.querySelector('.thought-line-steps');
    const live = root.querySelector('[role="status"]');
    let interval = 0;
    const controller = createThoughtController({
      onState(state) {
        root.dataset.working = String(state.working);
        label.textContent = state.working ? 'Pensando…' : 'Pensou por';
        timer.textContent = state.elapsed;
        live.textContent = state.working ? 'A inteligência artificial está pensando' : `Resposta preparada em ${state.elapsed}`;
        stepsNode.replaceChildren(...state.steps.map((text, index) => {
          const step = document.createElement('span');
          step.className = 'thought-line-step';
          step.textContent = text;
          step.style.opacity = state.working && index === state.steps.length - 1 ? '1' : '.62';
          return step;
        }));
      }
    });
    return {
      start(steps) {
        clearInterval(interval);
        controller.start(steps);
        interval = setInterval(() => controller.tick(), 100);
      },
      settle() {
        clearInterval(interval);
        return controller.settle();
      }
    };
  }

  return { formatElapsed, createThoughtController, mountThoughtLine };
});
