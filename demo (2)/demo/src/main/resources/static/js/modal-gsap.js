(function modalGsapModule(root, factory) {
  'use strict';

  const exports = factory();
  if (typeof module !== 'undefined' && module.exports) module.exports = exports;
  if (root?.document) {
    const media = root.matchMedia('(prefers-reduced-motion: reduce)');
    root.modalGsap = exports.createModalAnimator({
      gsap: root.gsap,
      reducedMotion: media,
      body: root.document.body
    });
  }
})(typeof window !== 'undefined' ? window : globalThis, function modalGsapFactory() {
  const triggers = new WeakMap();

  function createModalAnimator({ gsap, reducedMotion, body }) {
    const prefersReducedMotion = () => typeof reducedMotion === 'boolean' ? reducedMotion : Boolean(reducedMotion?.matches);
    const focusable = panel => panel.querySelector?.('button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])');
    const lockPage = locked => { if (body?.style) body.style.overflow = locked ? 'hidden' : ''; };
    const remember = (surface, trigger) => triggers.set(surface, trigger || surface?.ownerDocument?.activeElement);
    const restore = surface => triggers.get(surface)?.focus?.();

    function openOverlay(overlay, panel, trigger) {
      remember(overlay, trigger);
      overlay.style.display = 'flex';
      overlay.style.visibility = 'visible';
      overlay.setAttribute('aria-hidden', 'false');
      lockPage(true);
      const complete = () => focusable(panel)?.focus?.();
      if (!gsap || prefersReducedMotion()) {
        overlay.style.opacity = '1'; panel.style.opacity = '1'; complete(); return;
      }
      gsap.killTweensOf([overlay, panel]);
      gsap.set(overlay, { autoAlpha: 0 });
      gsap.set(panel, { autoAlpha: 0, y: 28, scale: .955 });
      gsap.timeline({ defaults: { ease: 'power3.out' }, onComplete: complete })
        .to(overlay, { autoAlpha: 1, duration: .2 })
        .to(panel, { autoAlpha: 1, y: 0, scale: 1, duration: .38 }, '-=.12');
    }

    function closeOverlay(overlay, panel) {
      return new Promise(resolve => {
        const complete = () => {
          overlay.style.display = 'none';
          overlay.setAttribute('aria-hidden', 'true');
          lockPage(false); restore(overlay); resolve();
        };
        if (!gsap || prefersReducedMotion()) { complete(); return; }
        gsap.killTweensOf([overlay, panel]);
        gsap.timeline({ defaults: { ease: 'power2.in' }, onComplete: complete })
          .to(panel, { autoAlpha: 0, y: 20, scale: .97, duration: .22 })
          .to(overlay, { autoAlpha: 0, duration: .18 }, '-=.1');
      });
    }

    function openDialog(dialog, trigger, initialFocus) {
      remember(dialog, trigger);
      if (!dialog.open) dialog.showModal();
      lockPage(true);
      const complete = () => (initialFocus || focusable(dialog))?.focus?.();
      if (!gsap || prefersReducedMotion()) {
        dialog.style.visibility = 'visible'; dialog.style.opacity = '1'; complete(); return;
      }
      gsap.killTweensOf(dialog);
      gsap.set(dialog, { autoAlpha: 0, y: 32, scale: .94 });
      gsap.timeline({ defaults: { ease: 'power3.out' }, onComplete: complete })
        .to(dialog, { autoAlpha: 1, duration: .18 })
        .to(dialog, { y: 0, scale: 1, duration: .36 }, '-=.14');
    }

    function closeDialog(dialog) {
      return new Promise(resolve => {
        const complete = () => {
          if (dialog.open) dialog.close();
          lockPage(false); restore(dialog); resolve();
        };
        if (!dialog.open) { complete(); return; }
        if (!gsap || prefersReducedMotion()) { complete(); return; }
        gsap.killTweensOf(dialog);
        gsap.timeline({ defaults: { ease: 'power2.in' }, onComplete: complete })
          .to(dialog, { y: 22, scale: .96, duration: .2 })
          .to(dialog, { autoAlpha: 0, duration: .16 }, '-=.08');
      });
    }

    return {
      openOverlay, closeOverlay, openDialog, closeDialog,
      abrir: openOverlay, fechar: closeOverlay,
      abrirDialogo: openDialog, fecharDialogo: closeDialog
    };
  }

  return { createModalAnimator };
});
