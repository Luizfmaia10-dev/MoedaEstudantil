(function () {
  const gatilhos = new WeakMap();
  const reduzirMovimento = window.matchMedia('(prefers-reduced-motion: reduce)');

  function focavel(painel) {
    return painel.querySelector('button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])');
  }

  function abrir(overlay, painel, gatilho) {
    gatilhos.set(overlay, gatilho || document.activeElement);
    overlay.style.display = 'flex';
    overlay.setAttribute('aria-hidden', 'false');
    document.body.style.overflow = 'hidden';
    const concluir = () => focavel(painel)?.focus();
    if (!window.gsap || reduzirMovimento.matches) {
      overlay.style.visibility = 'visible'; overlay.style.opacity = '1'; painel.style.opacity = '1'; concluir(); return;
    }
    gsap.killTweensOf([overlay, painel]);
    gsap.set(overlay, { autoAlpha: 0 });
    gsap.set(painel, { autoAlpha: 0, y: 24, scale: .96 });
    gsap.timeline({ defaults: { ease: 'power2.out' }, onComplete: concluir })
      .to(overlay, { autoAlpha: 1, duration: .2 })
      .to(painel, { autoAlpha: 1, y: 0, scale: 1, duration: .3 }, '-=.1');
  }

  function fechar(overlay, painel) {
    return new Promise(resolve => {
      const concluir = () => {
        overlay.style.display = 'none';
        overlay.setAttribute('aria-hidden', 'true');
        document.body.style.overflow = '';
        gatilhos.get(overlay)?.focus?.();
        resolve();
      };
      if (!window.gsap || reduzirMovimento.matches) { concluir(); return; }
      gsap.killTweensOf([overlay, painel]);
      gsap.timeline({ defaults: { ease: 'power2.in' }, onComplete: concluir })
        .to(painel, { autoAlpha: 0, y: 16, scale: .97, duration: .18 })
        .to(overlay, { autoAlpha: 0, duration: .16 }, '-=.08');
    });
  }

  window.modalGsap = { abrir, fechar };
})();
