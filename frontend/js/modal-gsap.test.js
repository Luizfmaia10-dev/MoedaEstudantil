const test = require('node:test');
const assert = require('node:assert/strict');
const { createModalAnimator } = require('./modal-gsap.js');

function element() {
  const attributes = new Map();
  const focusable = { focused: false, focus() { this.focused = true; } };
  return {
    style: {}, open: false, focusable,
    setAttribute(name, value) { attributes.set(name, String(value)); },
    getAttribute(name) { return attributes.get(name); },
    querySelector() { return focusable; },
    showModal() { this.open = true; },
    close() { this.open = false; }
  };
}

function fakeGsap() {
  const calls = [];
  return {
    calls,
    killTweensOf(targets) { calls.push({ method: 'kill', targets }); },
    set(target, properties) { calls.push({ method: 'set', target, properties }); Object.assign(target.style, properties); },
    timeline(options) {
      const steps = [];
      calls.push({ method: 'timeline', options, steps });
      return {
        to(target, properties, position) {
          steps.push({ target, properties, position });
          Object.assign(target.style, properties);
          if (steps.length === 2) options.onComplete?.();
          return this;
        }
      };
    }
  };
}

test('overlay abre e fecha com timeline GSAP, bloqueio de rolagem e retorno de foco', async () => {
  const overlay = element();
  const panel = element();
  const trigger = { focused: false, focus() { this.focused = true; } };
  const gsap = fakeGsap();
  const body = { style: {} };
  const animator = createModalAnimator({ gsap, reducedMotion: false, body });

  animator.openOverlay(overlay, panel, trigger);
  assert.equal(overlay.style.display, 'flex');
  assert.equal(overlay.getAttribute('aria-hidden'), 'false');
  assert.equal(body.style.overflow, 'hidden');
  assert.equal(panel.focusable.focused, true);
  assert.equal(gsap.calls.filter(call => call.method === 'timeline').length, 1);

  await animator.closeOverlay(overlay, panel);
  assert.equal(overlay.style.display, 'none');
  assert.equal(overlay.getAttribute('aria-hidden'), 'true');
  assert.equal(body.style.overflow, '');
  assert.equal(trigger.focused, true);
  assert.equal(gsap.calls.filter(call => call.method === 'timeline').length, 2);
});

test('dialog nativo só fecha depois da animação de saída', async () => {
  const dialog = element();
  const trigger = { focused: false, focus() { this.focused = true; } };
  const preferredFocus = { focused: false, focus() { this.focused = true; } };
  const gsap = fakeGsap();
  const animator = createModalAnimator({ gsap, reducedMotion: false, body: { style: {} } });

  animator.openDialog(dialog, trigger, preferredFocus);
  assert.equal(dialog.open, true);
  assert.equal(preferredFocus.focused, true);
  assert.equal(dialog.focusable.focused, false);
  await animator.closeDialog(dialog);
  assert.equal(dialog.open, false);
  assert.equal(trigger.focused, true);
});

test('movimento reduzido preserva o ciclo funcional sem timelines', async () => {
  const overlay = element();
  const panel = element();
  const trigger = { focused: false, focus() { this.focused = true; } };
  const gsap = fakeGsap();
  const animator = createModalAnimator({ gsap, reducedMotion: true, body: { style: {} } });

  animator.openOverlay(overlay, panel, trigger);
  await animator.closeOverlay(overlay, panel);
  assert.equal(gsap.calls.filter(call => call.method === 'timeline').length, 0);
  assert.equal(trigger.focused, true);
});
