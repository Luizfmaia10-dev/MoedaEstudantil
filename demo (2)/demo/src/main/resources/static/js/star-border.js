(function initializeStarBorder(root, factory) {
  'use strict';

  const exports = factory();
  if (typeof module !== 'undefined' && module.exports) module.exports = exports;
  if (root) root.StarBorder = exports;
})(typeof window !== 'undefined' ? window : globalThis, function starBorderFactory() {
  const THEMES = {
    dark: { surface: '#0c0c0f', text: '#fafafa', track: 'rgba(255,255,255,.1)', light: '#fff' },
    light: { surface: '#fff', text: '#18181b', track: 'rgba(24,24,27,.12)', light: '#18181b' }
  };
  const HOVERS = ['lap', 'brighten', 'reveal', 'none'];
  const BLEED = 48;
  const LAP = 0.9;
  const TAU = Math.PI * 2;
  const WHITE = [255, 255, 255];
  const clamp = (value, min, max) => Math.min(max, Math.max(min, value));
  const wrap = (value, total) => ((value % total) + total) % total;
  const ease = t => (t < .5 ? 4 * t * t * t : 1 - (2 - 2 * t) ** 3 / 2);
  const mix = (a, b, t) => a.map((value, index) => value + (b[index] - value) * t);
  const rgba = (rgb, alpha) =>
    `rgba(${Math.round(rgb[0])}, ${Math.round(rgb[1])}, ${Math.round(rgb[2])}, ${clamp(alpha, 0, 1).toFixed(3)})`;
  let colorContext = null;

  function toRgb(color) {
    if (typeof document === 'undefined') return WHITE;
    colorContext = colorContext || document.createElement('canvas').getContext('2d');
    if (!colorContext) return WHITE;
    colorContext.fillStyle = '#000';
    colorContext.fillStyle = color;
    const value = String(colorContext.fillStyle);
    if (value.startsWith('#')) {
      const normalized = value.length === 4
        ? value.slice(1).split('').map(character => character + character).join('')
        : value.slice(1, 7);
      const hex = parseInt(normalized, 16);
      return [(hex >> 16) & 255, (hex >> 8) & 255, hex & 255];
    }
    const parts = value.match(/[\d.]+/g)?.map(Number) || WHITE;
    return [parts[0], parts[1], parts[2]];
  }

  function outline(width, height, radius) {
    const r = Math.max(0, Math.min(radius, width / 2, height / 2));
    const arc = Math.PI * r / 2;
    const across = Math.max(0, width - 2 * r);
    const down = Math.max(0, height - 2 * r);
    const segments = [
      { length: across, x: r, y: 0, dx: 1, dy: 0, angle: null },
      { length: arc, x: width - r, y: r, dx: 0, dy: 0, angle: -Math.PI / 2 },
      { length: down, x: width, y: r, dx: 0, dy: 1, angle: null },
      { length: arc, x: width - r, y: height - r, dx: 0, dy: 0, angle: 0 },
      { length: across, x: width - r, y: height, dx: -1, dy: 0, angle: null },
      { length: arc, x: r, y: height - r, dx: 0, dy: 0, angle: Math.PI / 2 },
      { length: down, x: 0, y: height - r, dx: 0, dy: -1, angle: null },
      { length: arc, x: r, y: r, dx: 0, dy: 0, angle: Math.PI }
    ];
    return { r, segments, total: Math.max(1, segments.reduce((sum, segment) => sum + segment.length, 0)) };
  }

  function pointAt(shape, distance, point) {
    let cursor = wrap(distance, shape.total);
    for (const segment of shape.segments) {
      if (segment.length > 0 && cursor <= segment.length) {
        if (segment.angle === null) {
          point.x = segment.x + segment.dx * cursor;
          point.y = segment.y + segment.dy * cursor;
          point.nx = segment.dy;
          point.ny = -segment.dx;
        } else {
          const angle = segment.angle + cursor / shape.r;
          point.nx = Math.cos(angle);
          point.ny = Math.sin(angle);
          point.x = segment.x + point.nx * shape.r;
          point.y = segment.y + point.ny * shape.r;
        }
        return point;
      }
      cursor -= segment.length;
    }
    return point;
  }

  function project(shape, px, py) {
    let best = Infinity;
    let found = 0;
    let offset = 0;
    for (const segment of shape.segments) {
      if (segment.length > 0) {
        let cursor;
        let x;
        let y;
        if (segment.angle === null) {
          cursor = clamp((px - segment.x) * segment.dx + (py - segment.y) * segment.dy, 0, segment.length);
          x = segment.x + segment.dx * cursor;
          y = segment.y + segment.dy * cursor;
        } else {
          let angle = wrap(Math.atan2(py - segment.y, px - segment.x) - segment.angle, TAU);
          if (angle > Math.PI / 2) angle = angle > Math.PI * 1.25 ? 0 : Math.PI / 2;
          cursor = angle * shape.r;
          x = segment.x + Math.cos(segment.angle + angle) * shape.r;
          y = segment.y + Math.sin(segment.angle + angle) * shape.r;
        }
        const distance = (px - x) ** 2 + (py - y) ** 2;
        if (distance < best) {
          best = distance;
          found = offset + cursor;
        }
      }
      offset += segment.length;
    }
    return found;
  }

  function createStarBorder(root, options = {}) {
    if (!root) throw new TypeError('O elemento do Star Border é obrigatório.');
    let canvas = root.querySelector?.('canvas.star-border-light');
    if (!canvas) {
      canvas = root.ownerDocument.createElement('canvas');
      canvas.className = 'star-border-light';
      canvas.setAttribute('aria-hidden', 'true');
      root.prepend(canvas);
    }
    const ctx = canvas.getContext('2d');
    if (!ctx) return { destroy() {}, pulse() {} };

    const theme = THEMES[options.theme] || THEMES.dark;
    const settings = {
      light: options.theme === 'light',
      color: options.color || theme.light,
      trailColor: options.trailColor || options.color || theme.light,
      duration: Math.max(.3, options.duration ?? 4),
      thickness: Math.max(.5, options.thickness ?? 1),
      radius: Math.max(0, options.radius ?? 12),
      trailLength: clamp(options.trailLength ?? .3, 0, 1),
      stars: clamp(Math.round(options.stars ?? 1), 1, 6),
      glow: clamp(options.glow ?? .6, 0, 1),
      sparkle: options.sparkle ?? false,
      direction: options.direction === 'counterclockwise' ? -1 : 1,
      hover: HOVERS.includes(options.hover) ? options.hover : 'lap',
      clickPulse: options.clickPulse ?? true
    };
    const reduce = root.ownerDocument?.defaultView?.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
    const point = { x: 0, y: 0, nx: 0, ny: -1 };
    const state = {
      width: 0, height: 0, dpr: 1, phase: 0, velocity: 0, motion: 0, lap: -1,
      time: 0, reveal: 0, boost: 0, flash: 0, emit: 0, hovered: false,
      focused: false, pointerX: 0, pointerY: 0, visible: true
    };
    const pulses = [];
    const dust = [];
    const colors = { head: toRgb(settings.color), tail: toRgb(settings.trailColor) };
    let raf = 0;
    let last = performance.now();
    let alive = true;

    root.style.setProperty('--star-border-radius', `${settings.radius}px`);
    root.style.setProperty('--star-border-thickness', `${settings.thickness}px`);
    root.style.setProperty('--star-border-surface', options.backgroundColor || theme.surface);
    root.style.setProperty('--star-border-text', options.textColor || theme.text);
    root.style.setProperty('--star-border-track', options.borderColor || theme.track);
    canvas.style.pointerEvents = 'none';
    canvas.style.position = 'absolute';
    canvas.style.top = `${-BLEED}px`;
    canvas.style.left = `${-BLEED}px`;
    canvas.style.width = `calc(100% + ${BLEED * 2}px)`;
    canvas.style.height = `calc(100% + ${BLEED * 2}px)`;
    canvas.style.zIndex = '0';
    root.dataset.starBorderReady = 'true';

    const shapeFor = () => outline(
      Math.max(1, state.width - settings.thickness),
      Math.max(1, state.height - settings.thickness),
      settings.radius - settings.thickness / 2
    );

    function wake() {
      if (raf || !alive || !state.visible) return;
      last = performance.now();
      raf = requestAnimationFrame(frame);
    }

    function measure() {
      state.width = root.offsetWidth;
      state.height = root.offsetHeight;
      state.dpr = Math.min(root.ownerDocument?.defaultView?.devicePixelRatio || 1, 2);
      canvas.width = Math.max(1, Math.round((state.width + BLEED * 2) * state.dpr));
      canvas.height = Math.max(1, Math.round((state.height + BLEED * 2) * state.dpr));
      wake();
    }

    function local(event) {
      const rect = root.getBoundingClientRect();
      return [
        (event.clientX - rect.left) * (rect.width ? state.width / rect.width : 1),
        (event.clientY - rect.top) * (rect.height ? state.height / rect.height : 1)
      ];
    }

    function stroke(x1, y1, x2, y2, width, style) {
      ctx.lineWidth = width;
      ctx.strokeStyle = style;
      ctx.beginPath();
      ctx.moveTo(x1, y1);
      ctx.lineTo(x2, y2);
      ctx.stroke();
    }

    function drawTrail(shape, head, length, sign, intensity, tip = true) {
      if (length < .5 || intensity <= 0) return;
      const steps = clamp(Math.ceil(length / 2), 2, 90);
      pointAt(shape, head, point);
      let px = point.x;
      let py = point.y;
      for (let index = 1; index <= steps; index += 1) {
        pointAt(shape, head - sign * length * (index / steps), point);
        const progress = 1 - (index - .5) / steps;
        const fade = progress * progress * intensity;
        let tone = mix(colors.tail, colors.head, progress);
        if (!settings.light) tone = mix(tone, WHITE, progress ** 6 * .65);
        ctx.lineCap = tip && index === 1 ? 'round' : 'butt';
        stroke(px, py, point.x, point.y, 2 + 6 * settings.glow,
          rgba(tone, fade * (settings.light ? .1 : .2) * settings.glow));
        stroke(px, py, point.x, point.y, settings.thickness, rgba(tone, fade));
        px = point.x;
        py = point.y;
      }
    }

    function drawHead(x, y, intensity, twinkle) {
      if (intensity <= 0) return;
      const reach = (12 + 40 * settings.glow) * (settings.light ? .6 : 1);
      const bloom = ctx.createRadialGradient(x, y, 0, x, y, reach);
      const strength = (settings.light ? .08 : .4) * settings.glow * intensity;
      bloom.addColorStop(0, rgba(colors.head, strength));
      bloom.addColorStop(.2, rgba(colors.head, strength * .4));
      bloom.addColorStop(.5, rgba(colors.head, strength * .1));
      bloom.addColorStop(1, rgba(colors.head, 0));
      ctx.fillStyle = bloom;
      ctx.fillRect(x - reach, y - reach, reach * 2, reach * 2);
      if (!settings.sparkle) return;

      const size = (1 + settings.thickness * .6) * (settings.light ? .85 : 1);
      const core = ctx.createRadialGradient(x, y, 0, x, y, size * 2.2);
      core.addColorStop(0, rgba(settings.light ? colors.head : WHITE, intensity));
      core.addColorStop(settings.light ? .3 : .4,
        rgba(settings.light ? colors.head : mix(colors.head, WHITE, .5), intensity * .9));
      core.addColorStop(1, rgba(colors.head, 0));
      ctx.fillStyle = core;
      ctx.beginPath();
      ctx.arc(x, y, size * 2.2, 0, TAU);
      ctx.fill();

      const spike = (3 + 4 * settings.glow + settings.thickness * 1.5) * twinkle;
      const center = settings.light ? colors.head : WHITE;
      const horizontal = ctx.createLinearGradient(x - spike, y, x + spike, y);
      horizontal.addColorStop(0, rgba(colors.head, 0));
      horizontal.addColorStop(.5, rgba(center, intensity * .95));
      horizontal.addColorStop(1, rgba(colors.head, 0));
      stroke(x - spike, y, x + spike, y, settings.light ? .75 : 1, horizontal);
      const vertical = ctx.createLinearGradient(x, y - spike, x, y + spike);
      vertical.addColorStop(0, rgba(colors.head, 0));
      vertical.addColorStop(.5, rgba(center, intensity * .95));
      vertical.addColorStop(1, rgba(colors.head, 0));
      stroke(x, y - spike, x, y + spike, settings.light ? .75 : 1, vertical);
    }

    function spawnDust(x, y, nx, ny, speed) {
      if (dust.length > 48) return;
      const push = 3 + Math.random() * 10;
      const drift = (Math.random() - .5) * 8;
      dust.push({
        x: x + nx * (Math.random() * 3 - 1), y: y + ny * (Math.random() * 3 - 1),
        vx: nx * push - ny * drift + speed * ny * .04,
        vy: ny * push + nx * drift - speed * nx * .04,
        age: 0, life: .35 + Math.random() * .5, size: .45 + Math.random() * .7,
        seed: Math.random() * TAU
      });
    }

    function draw(shape) {
      const glow = state.boost > .001 ? Math.min(1.5, settings.glow + .4 * state.boost) : settings.glow;
      ctx.setTransform(1, 0, 0, 1, 0, 0);
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      if (state.width < 2 || state.height < 2) return;
      const offset = (BLEED + settings.thickness / 2) * state.dpr;
      ctx.setTransform(state.dpr, 0, 0, state.dpr, offset, offset);
      ctx.globalCompositeOperation = settings.light ? 'source-over' : 'lighter';
      ctx.lineCap = 'butt';

      const total = shape.total;
      const spacing = total / settings.stars;
      const speed = Math.abs(state.motion);
      const sign = state.motion >= 0 ? 1 : -1;
      const length = Math.min(speed * settings.trailLength * settings.duration, spacing * .92, total * .6);
      const intensity = state.reveal * (1 + .4 * state.boost + .6 * state.flash);
      const ring = state.flash * (settings.light ? .18 : .32)
        + state.boost * state.reveal * (settings.light ? .07 : .12);
      if (ring > .005) {
        ctx.lineWidth = settings.thickness + 2 * glow;
        ctx.strokeStyle = rgba(colors.head, ring);
        ctx.beginPath();
        ctx.roundRect(0, 0, Math.max(1, state.width - settings.thickness),
          Math.max(1, state.height - settings.thickness), Math.max(0, shape.r));
        ctx.stroke();
      }

      const still = clamp(1 - speed / Math.max(1, (total / settings.duration) * .6), 0, 1)
        * Math.min(1, intensity);
      const rest = (10 + 14 * glow + settings.thickness * 2) * still;
      for (let index = 0; index < settings.stars; index += 1) {
        const head = state.phase + index * spacing;
        drawTrail(shape, head, length, sign, Math.min(1, intensity));
        if (still > .01) {
          drawTrail(shape, head, rest, 1, still, false);
          drawTrail(shape, head, rest, -1, still, false);
        }
      }
      for (const pulse of pulses) {
        const progress = pulse.age / .65;
        const fade = (1 - progress) ** 2;
        const travel = total / 2 * (1 - (1 - Math.min(1, pulse.age / .5)) ** 2);
        const tail = Math.min(travel, total * .16);
        for (const way of [1, -1]) {
          const front = pulse.origin + way * travel;
          drawTrail(shape, front, tail, way, fade);
          pointAt(shape, front, point);
          drawHead(point.x, point.y, fade * .8, .8);
        }
      }
      for (let index = 0; index < settings.stars; index += 1) {
        pointAt(shape, state.phase + index * spacing, point);
        const twinkle = .82 + .18 * Math.sin(state.time * 6.1 + index * 2.3)
          * Math.sin(state.time * 2.7 + index);
        drawHead(point.x, point.y, Math.min(1.3, intensity), twinkle);
      }
      for (const mote of dust) {
        const left = 1 - mote.age / mote.life;
        const shimmer = .45 + .55 * Math.sin(state.time * 20 + mote.seed);
        const alpha = left * shimmer * state.reveal * (settings.light ? .35 : .9);
        if (alpha <= .01) continue;
        ctx.fillStyle = rgba(settings.light ? colors.head : mix(colors.tail, WHITE, .6), alpha);
        ctx.beginPath();
        ctx.arc(mote.x, mote.y, mote.size * (.6 + .4 * left), 0, TAU);
        ctx.fill();
      }
    }

    function frame(now) {
      raf = 0;
      if (!alive) return;
      const dt = Math.min(.05, Math.max(.001, (now - last) / 1000));
      last = now;
      state.time += dt;
      const shape = shapeFor();
      const total = shape.total;
      const spacing = total / settings.stars;
      const engaged = state.hovered || state.focused;
      const cruise = reduce ? 0 : settings.direction * total / settings.duration;
      const revealTarget = settings.hover === 'reveal' && !engaged ? 0 : 1;
      state.reveal += (revealTarget - state.reveal)
        * (1 - Math.exp(-dt / (revealTarget > state.reveal ? .16 : .45)));
      const boostTarget = settings.hover === 'brighten' && engaged ? 1 : 0;
      state.boost += (boostTarget - state.boost)
        * (1 - Math.exp(-dt / (boostTarget > state.boost ? .18 : .5)));
      state.flash *= Math.exp(-dt / .22);
      state.velocity += (cruise - state.velocity) * (1 - Math.exp(-dt / .45));
      let extra = 0;
      if (state.lap >= 0) {
        const before = ease(Math.min(1, state.lap / LAP));
        state.lap += dt;
        extra = (ease(Math.min(1, state.lap / LAP)) - before) * total * settings.direction;
        if (state.lap >= LAP) state.lap = -1;
      }
      state.motion = state.velocity + extra / dt;
      state.phase = wrap(state.phase + state.velocity * dt + extra, total);

      const pace = Math.abs(state.motion) / Math.max(1, Math.abs(total / settings.duration));
      if (settings.sparkle && !reduce && state.reveal > .05) {
        state.emit += dt * (2 + 6 * Math.min(2, pace)) * settings.stars;
        while (state.emit > 1) {
          state.emit -= 1;
          pointAt(shape, state.phase + Math.floor(Math.random() * settings.stars) * spacing, point);
          spawnDust(point.x, point.y, point.nx, point.ny, state.motion);
        }
      }
      for (let index = dust.length - 1; index >= 0; index -= 1) {
        const mote = dust[index];
        mote.age += dt;
        mote.x += mote.vx * dt;
        mote.y += mote.vy * dt;
        mote.vx *= Math.exp(-dt * 2.2);
        mote.vy *= Math.exp(-dt * 2.2);
        if (mote.age >= mote.life) dust.splice(index, 1);
      }
      for (let index = pulses.length - 1; index >= 0; index -= 1) {
        pulses[index].age += dt;
        if (pulses[index].age >= .65) pulses.splice(index, 1);
      }
      draw(shape);

      const settling = Math.abs(revealTarget - state.reveal) > .002
        || Math.abs(boostTarget - state.boost) > .002 || state.flash > .01
        || pulses.length > 0 || dust.length > 0;
      const moving = Math.abs(state.motion) > .05 || cruise !== 0 || state.lap >= 0;
      const showing = state.reveal > .002 || revealTarget > 0;
      if (state.visible && (settling || (moving && showing) || state.hovered)) raf = requestAnimationFrame(frame);
    }

    const controller = {
      pulse(origin = state.phase) {
        if (!settings.clickPulse) return;
        pulses.push({ origin, age: 0 });
        if (pulses.length > 4) pulses.shift();
        state.flash = 1;
        wake();
      },
      destroy() {
        alive = false;
        cancelAnimationFrame(raf);
        resizeObserver?.disconnect();
        visibilityObserver?.disconnect();
        root.removeEventListener('pointerenter', onEnter);
        root.removeEventListener('pointerleave', onLeave);
        root.removeEventListener('pointerdown', onDown);
        root.removeEventListener('click', onClick);
        root.removeEventListener('focus', onFocus);
        root.removeEventListener('blur', onBlur);
        delete root.dataset.starBorderReady;
      }
    };

    function onEnter(event) {
      [state.pointerX, state.pointerY] = local(event);
      state.hovered = true;
      if (settings.hover === 'reveal' && state.reveal < .15) {
        state.phase = project(shapeFor(), state.pointerX - settings.thickness / 2,
          state.pointerY - settings.thickness / 2);
        state.velocity = 0;
      }
      if (settings.hover === 'lap' && state.lap < 0 && !reduce) state.lap = 0;
      wake();
    }
    function onLeave() { state.hovered = false; wake(); }
    function onDown(event) {
      if (event.button !== 0 && event.pointerType === 'mouse') return;
      const [x, y] = local(event);
      controller.pulse(project(shapeFor(), x - settings.thickness / 2, y - settings.thickness / 2));
    }
    function onClick(event) { if (event.detail === 0) controller.pulse(state.phase); }
    function onFocus() {
      state.focused = root.matches(':focus-visible');
      if (state.focused && settings.hover === 'lap' && state.lap < 0 && !reduce) state.lap = 0;
      wake();
    }
    function onBlur() { state.focused = false; wake(); }

    const Resize = root.ownerDocument?.defaultView?.ResizeObserver;
    const Intersection = root.ownerDocument?.defaultView?.IntersectionObserver;
    const resizeObserver = Resize ? new Resize(measure) : null;
    const visibilityObserver = Intersection ? new Intersection(entries => {
      state.visible = entries.some(entry => entry.isIntersecting);
      if (state.visible) wake();
    }) : null;
    resizeObserver?.observe(root);
    visibilityObserver?.observe(root);
    root.addEventListener('pointerenter', onEnter);
    root.addEventListener('pointerleave', onLeave);
    root.addEventListener('pointerdown', onDown);
    root.addEventListener('click', onClick);
    root.addEventListener('focus', onFocus);
    root.addEventListener('blur', onBlur);
    measure();
    return controller;
  }

  return { BLEED, LAP, outline, pointAt, project, createStarBorder };
});
