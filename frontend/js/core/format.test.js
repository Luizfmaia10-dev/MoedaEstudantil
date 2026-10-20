const test = require('node:test');
const assert = require('node:assert/strict');

const { escapeHtml, date, number } = require('./format.js');

test('escapeHtml neutraliza conteúdo inserido em templates', () => {
  assert.equal(escapeHtml('<script>"x" & y</script>'), '&lt;script&gt;&quot;x&quot; &amp; y&lt;/script&gt;');
});

test('date usa formato curto brasileiro', () => {
  assert.match(date('2026-10-19T12:30:00Z'), /^19\/10\/2026/);
});

test('number formata valores no locale brasileiro', () => {
  assert.equal(number(1234), '1.234');
});
