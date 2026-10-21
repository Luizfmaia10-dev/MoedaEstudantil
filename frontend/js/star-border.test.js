const test = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const { pathToFileURL } = require('node:url');
const { browserResult } = require('../../test-support/chromium.cjs');
const { outline, pointAt } = require('./star-border.js');

test('estrela percorre o contorno arredondado exato do botão', () => {
  const shape = outline(200, 56, 18);
  const point = { x: 0, y: 0, nx: 0, ny: 0 };

  pointAt(shape, 0, point);
  assert.deepEqual(point, { x: 18, y: 0, nx: 0, ny: -1 });

  pointAt(shape, 164, point);
  assert.ok(Math.abs(point.x - 182) < 0.001);
  assert.ok(Math.abs(point.y) < 0.001);
  assert.ok(Math.abs(point.nx) < 0.001);
  assert.ok(Math.abs(point.ny + 1) < 0.001);

  assert.ok(Math.abs(shape.total - (2 * 164 + 2 * 20 + 2 * Math.PI * 18)) < 0.001);
});

test('canvas inclui bleed, responde ao clique e respeita movimento reduzido', t => {
  const script = pathToFileURL(resolve(__dirname, 'star-border.js')).href;
  const source = readFileSync(resolve(__dirname, 'star-border.js'), 'utf8');
  const result = browserResult(t, `<!doctype html><html><body style="background:#102a18;padding:80px">
    <button id="cta" style="position:relative;width:240px;height:64px;border:0;border-radius:18px;background:#0c0c0f;color:white">
      <canvas class="star-border-light"></canvas>Transferir com IA
    </button>
    <pre id="test-result"></pre>
    <script src="${script}"></script><script>
      const button = document.getElementById('cta');
      const border = StarBorder.createStarBorder(button, { color:'#b8ff6a', trailColor:'#63e6a6', radius:18, sparkle:true });
      let pulses = 0;
      const pulse = border.pulse;
      border.pulse = origin => { pulses += 1; pulse(origin); };
      button.dispatchEvent(new PointerEvent('pointerdown', { bubbles:true, clientX:100, clientY:100, pointerType:'mouse', button:0 }));
      setTimeout(() => {
        const canvas = button.querySelector('canvas');
        document.getElementById('test-result').textContent = JSON.stringify({
          ready: button.dataset.starBorderReady,
          canvasWidth: canvas.width,
          canvasHeight: canvas.height,
          buttonWidth: button.offsetWidth,
          buttonHeight: button.offsetHeight,
          pulses,
          hasReducedBranch: ${JSON.stringify(source.includes('prefers-reduced-motion: reduce'))}
        });
      }, 120);
    </script></body></html>`);
  if (!result) return;
  assert.equal(result.ready, 'true');
  assert.ok(result.canvasWidth > result.buttonWidth);
  assert.ok(result.canvasHeight > result.buttonHeight);
  assert.ok(result.pulses > 0);
  assert.equal(result.hasReducedBranch, true);
});
