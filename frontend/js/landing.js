(function landingModule(root) {
  'use strict';

  function createLoginState(perfil) {
    const profiles = {
      ALUNO: { perfil: 'ALUNO', label: 'Aluno', cadastroHref: 'aluno.html?tab=cadastro' },
      PROFESSOR: { perfil: 'PROFESSOR', label: 'Professor', cadastroHref: null },
      EMPRESA: { perfil: 'EMPRESA', label: 'Empresa', cadastroHref: 'empresa.html?tab=cadastro' }
    };
    if (!Object.hasOwn(profiles, perfil)) throw new Error('Perfil inválido.');
    return profiles[perfil];
  }

  function calculateOrbitFrame({ progress, phase = 0, radiusX, radiusY, spin = 1 }) {
    const round = value => Math.round(value * 1000) / 1000;
    const angle = progress * Math.PI * 2 + phase;
    const depth = (Math.sin(angle) + 1) / 2;
    return {
      x: round(Math.cos(angle) * radiusX),
      y: round(Math.sin(angle) * radiusY),
      scale: round(.7 + depth * .3),
      opacity: round(.58 + depth * .42),
      zIndex: depth >= .5 ? 30 : 10,
      rotation: round(progress * 720 * spin)
    };
  }

  function initializeHeroMotion({ stage, gsap, ScrollTrigger, reducedMotion }) {
    if (!stage) return null;
    const coins = Array.from(stage.querySelectorAll('[data-orbit-coin]'));
    if (!coins.length) return null;
    const controller = { progress: .25 };

    function render() {
      const box = stage.getBoundingClientRect();
      stage.style.setProperty('--globe-scroll-spin', `${Math.round(controller.progress * 48)}deg`);
      coins.forEach(coin => {
        const frame = calculateOrbitFrame({
          progress: controller.progress,
          phase: Number(coin.dataset.orbitPhase || 0),
          radiusX: box.width * Number(coin.dataset.orbitX || .34),
          radiusY: box.height * Number(coin.dataset.orbitY || .2),
          spin: Number(coin.dataset.orbitSpin || 1)
        });
        coin.style.setProperty('--orbit-x', `${frame.x}px`);
        coin.style.setProperty('--orbit-y', `${frame.y}px`);
        coin.style.setProperty('--orbit-scale', String(frame.scale));
        coin.style.setProperty('--orbit-opacity', String(frame.opacity));
        coin.style.setProperty('--orbit-rotation', `${frame.rotation}deg`);
        coin.style.zIndex = String(frame.zIndex);
      });
    }

    render();
    if (reducedMotion || !gsap || !ScrollTrigger) {
      stage.dataset.motion = 'reduced';
      return null;
    }
    gsap.registerPlugin(ScrollTrigger);
    stage.dataset.motion = 'scroll';
    controller.progress = 0;
    return gsap.to(controller, {
      progress: 1,
      ease: 'none',
      onUpdate: render,
      scrollTrigger: {
        trigger: stage,
        start: 'top bottom',
        end: 'bottom top',
        scrub: .65,
        invalidateOnRefresh: true
      }
    });
  }

  function initializeLanding({ document, location, authApi, sessionStore, routes, matchMedia, gsap, ScrollTrigger, modalAnimator }) {
    let state = createLoginState('ALUNO');
    let opener;
    let pending = false;
    const dialog = document.getElementById('loginDialog');
    const title = document.getElementById('loginTitle');
    const form = document.getElementById('loginForm');
    const email = document.getElementById('loginEmail');
    const password = document.getElementById('loginPassword');
    const errorBox = document.getElementById('loginError');
    const submit = document.getElementById('loginSubmit');
    const signup = document.getElementById('signupLink');
    const profiles = document.querySelectorAll('.profile-switcher [data-profile]');

    function selectProfile(perfil) {
      if (pending) return;
      if (perfil !== state.perfil) password.value = '';
      state = createLoginState(perfil);
      title.textContent = `Entre como ${state.label.toLocaleLowerCase('pt-BR')}`;
      profiles.forEach(button => button.setAttribute('aria-pressed', button.dataset.profile === state.perfil));
      signup.hidden = !state.cadastroHref;
      if (state.cadastroHref) signup.setAttribute('href', state.cadastroHref);
      else signup.removeAttribute('href');
      errorBox.textContent = '';
    }

    function openLogin(button) {
      opener = button;
      selectProfile(button?.dataset.profile || 'ALUNO');
      if (modalAnimator) modalAnimator.openDialog(dialog, button, email);
      else {
        if (!dialog.open) dialog.showModal();
        email.focus();
      }
    }

    function closeLogin() {
      if (modalAnimator) return modalAnimator.closeDialog(dialog);
      dialog.close();
      return Promise.resolve();
    }

    document.querySelectorAll('[data-open-login]').forEach(button => {
      button.addEventListener('click', () => openLogin(button));
    });
    profiles.forEach(button => button.addEventListener('click', () => selectProfile(button.dataset.profile)));
    document.getElementById('closeLogin').addEventListener('click', closeLogin);
    dialog.addEventListener('cancel', event => { event.preventDefault(); closeLogin(); });
    dialog.addEventListener('close', () => opener?.focus());
    dialog.addEventListener('click', event => {
      if (event.target !== dialog) return;
      const box = dialog.getBoundingClientRect();
      if (event.clientX < box.left || event.clientX > box.right || event.clientY < box.top || event.clientY > box.bottom) closeLogin();
    });

    form.addEventListener('submit', async event => {
      event.preventDefault();
      if (pending) return;
      pending = true;
      submit.disabled = true;
      submit.textContent = 'Entrando…';
      form.setAttribute('aria-busy', 'true');
      profiles.forEach(button => { button.disabled = true; });
      errorBox.textContent = '';
      try {
        const user = await authApi.login(email.value, password.value, state.perfil);
        sessionStore.save(user);
        location.assign(routes.dashboard(user));
      } catch (error) {
        errorBox.textContent = error?.status === 401
          ? 'E-mail, senha ou perfil inválido.'
          : 'A plataforma está indisponível agora. Tente novamente.';
      } finally {
        pending = false;
        submit.disabled = false;
        submit.textContent = 'Entrar';
        form.setAttribute('aria-busy', 'false');
        profiles.forEach(button => { button.disabled = false; });
      }
    });

    const menu = document.getElementById('menuToggle');
    menu.addEventListener('click', () => {
      menu.setAttribute('aria-expanded', menu.getAttribute('aria-expanded') !== 'true');
    });
    document.querySelectorAll('#mainNav a').forEach(link => {
      link.addEventListener('click', () => menu.setAttribute('aria-expanded', 'false'));
    });
    document.querySelectorAll('[data-faq-toggle]').forEach(button => {
      button.addEventListener('click', () => {
        const expanded = button.getAttribute('aria-expanded') !== 'true';
        button.setAttribute('aria-expanded', expanded);
        document.getElementById(button.getAttribute('aria-controls')).hidden = !expanded;
      });
    });

    const stage = document.querySelector('.coin-stage');
    if (stage && matchMedia) {
      const hover = matchMedia('(hover: hover)');
      const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');
      initializeHeroMotion({ stage, gsap, ScrollTrigger, reducedMotion: reducedMotion.matches });
      stage.addEventListener('pointermove', event => {
        if (!hover.matches || reducedMotion.matches) return;
        const box = stage.getBoundingClientRect();
        const tilt = ((event.clientX - box.left) / box.width - .5) * 12;
        stage.style.setProperty('--pointer-tilt', `${tilt}deg`);
      });
      stage.addEventListener('pointerleave', () => stage.style.removeProperty('--pointer-tilt'));
      reducedMotion.addEventListener('change', () => stage.style.removeProperty('--pointer-tilt'));
    }
    selectProfile('ALUNO');
    if (new URLSearchParams(location.search).get('entrar') === '1') openLogin();
  }

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = { createLoginState, initializeLanding, calculateOrbitFrame, initializeHeroMotion };
  } else if (root.document) {
    initializeLanding({
      document: root.document, location: root.location, authApi: root.AuthApi,
      sessionStore: root.AppSession.current, routes: root.AppRoutes,
      matchMedia: root.matchMedia.bind(root), gsap: root.gsap, ScrollTrigger: root.ScrollTrigger,
      modalAnimator: root.modalGsap
    });
  }
})(typeof window !== 'undefined' ? window : globalThis);
