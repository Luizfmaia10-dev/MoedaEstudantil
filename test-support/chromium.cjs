const assert = require('node:assert/strict');
const { existsSync, mkdtempSync, writeFileSync, rmSync } = require('node:fs');
const { tmpdir } = require('node:os');
const { join } = require('node:path');
const { pathToFileURL } = require('node:url');
const { spawnSync } = require('node:child_process');

function browserResult(t, html) {
  const browser = [process.env.LANDING_BROWSER_EXECUTABLE,
    'C:/Program Files/Google/Chrome/Application/chrome.exe',
    'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
    '/usr/bin/chromium', '/usr/bin/chromium-browser', '/usr/bin/google-chrome',
    '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
  ].find(candidate => candidate && existsSync(candidate));
  if (!browser) { t.skip('Configure LANDING_BROWSER_EXECUTABLE para verificar o DOM e o CSS em Chromium.'); return null; }
  const directory = mkdtempSync(join(tmpdir(), 'moeda-browser-'));
  t.after(() => rmSync(directory, { recursive: true, force: true }));
  const harness = join(directory, 'test.html');
  writeFileSync(harness, html);
  const result = spawnSync(browser, ['--headless', '--disable-gpu', '--no-sandbox', '--window-size=1440,1000',
    `--user-data-dir=${join(directory, 'profile')}`, '--allow-file-access-from-files',
    '--virtual-time-budget=2000', '--dump-dom', pathToFileURL(harness).href
  ], { encoding: 'utf8', timeout: 20000, windowsHide: true });
  assert.equal(result.status, 0, result.error?.message || result.stderr);
  const output = result.stdout.match(/<pre id="test-result">([^<]+)<\/pre>/);
  assert.ok(output, 'O navegador deve produzir o resultado do DOM e CSS reais.');
  return JSON.parse(output[1].replaceAll('&amp;', '&').replaceAll('&lt;', '<').replaceAll('&gt;', '>'));
}

function contrastRatio(first, second) {
  function luminance(color) {
    const channels = color.match(/[\d.]+/g).slice(0, 3).map(channel => {
      const value = Number(channel) / 255;
      return value <= .04045 ? value / 12.92 : ((value + .055) / 1.055) ** 2.4;
    });
    return channels[0] * .2126 + channels[1] * .7152 + channels[2] * .0722;
  }
  const colors = [luminance(first), luminance(second)];
  return (Math.max(...colors) + .05) / (Math.min(...colors) + .05);
}

module.exports = { browserResult, contrastRatio };
