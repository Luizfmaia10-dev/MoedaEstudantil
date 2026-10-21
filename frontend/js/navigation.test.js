const test = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve, sep } = require('node:path');
const { pathToFileURL } = require('node:url');
const { browserResult, contrastRatio } = require('../../test-support/chromium.cjs');
const { cardsFor } = require('./core/routes.js');

const roots = [resolve(__dirname, '..'), resolve(__dirname, '../../demo (2)/demo/src/main/resources/static')];
const features = ['resgatar-vantagens.html', 'extrato.html', 'solicitar-transferencia.html',
  'criterios-transferencia.html', 'distribuir-moedas.html', 'criterios-professor.html',
  'avaliar-transferencias.html', 'vantagens.html'];

for (const [rootIndex, root] of roots.entries()) {
  test(`todas as funcionalidades voltam ao painel sem atalhos de identidade fixa (${rootIndex ? 'espelho' : 'fonte'})`, t => {
    const pages = features.map(file => ({ file, root, html: readFileSync(resolve(root, file), 'utf8') }));
    const payload = JSON.stringify(pages).replaceAll('<', '\\u003c');
    const result = browserResult(t, `<!doctype html><meta charset="utf-8"><pre id="test-result"></pre><script>
      const pages = ${payload};
      document.getElementById('test-result').textContent = JSON.stringify(pages.map(page => {
        const dom = new DOMParser().parseFromString(page.html, 'text/html');
        return { file: page.file, root: page.root, back: dom.querySelector('.brand-wrapper')?.getAttribute('href'),
          links: [...dom.querySelectorAll('a[href]')].map(link => link.getAttribute('href')) };
      }));
    </script>`);
    if (!result) return;
    assert.equal(result.length, 8);
    for (const page of result) {
      assert.equal(page.back, 'painel.html', `${page.root}/${page.file}`);
      assert.ok(page.links.every(href => !href.includes('dev-menu.html') && !/[?&](?:alunoId|professorId|empresaId|usuarioId)=\d+/.test(href)), page.file);
    }
    for (const perfil of ['ALUNO', 'PROFESSOR', 'EMPRESA']) {
      for (const card of cardsFor({ id: 87, perfil })) {
        const destination = new URL(card.href, 'https://moeda.example/');
        for (const id of destination.searchParams.values()) assert.equal(id, '87');
      }
    }
  });
}

for (const [rootIndex, root] of roots.entries()) {
  for (const [file, perfil, serviceName] of [
    ['aluno.html', 'ALUNO', 'alunoAuthService'], ['empresa.html', 'EMPRESA', 'empresaAuthService']
  ]) {
    test(`${file} mantém formulário e retorno ao painel após cadastro com identidade forest/lime (${rootIndex ? 'espelho' : 'fonte'})`, t => {
      const base = pathToFileURL(root + sep).href;
      const setup = `<script>
        sessionStorage.clear();
        window.requests = [];
        window.AppHttp = { request: async (url, options = {}) => {
          window.requests.push({ url, options });
          if (url.endsWith('/instituicoes')) return [{ id: 2, nome: 'PUC Minas' }];
          const { senha, ...data } = JSON.parse(options.body);
          return { ...data, id: 37, instituicaoNome: 'PUC Minas' };
        } };
      </script>`;
      const page = readFileSync(resolve(root, file), 'utf8')
        .replace('<head>', `<head><base href="${base}">`)
        .replace('<script src="js/core/http.js"></script>', setup);
      const result = browserResult(t, page.replace('</body>', `<pre id="test-result"></pre><script>
        addEventListener('load', async () => {
          const values = { cadNome: 'Conta Nova', cadEmail: 'nova@exemplo.com', cadSenha: 'senha-nova',
            cadCpf: '123.456.789-00', cadRg: 'MG-1', cadInstituicao: '2', cadCurso: 'Computação',
            cadEndereco: 'Rua A', cadCnpj: '12.345.678/0001-90' };
          for (const [id, value] of Object.entries(values)) { const input = document.getElementById(id); if (input) input.value = value; }
          document.getElementById('cadastroForm').dispatchEvent(new Event('submit', { cancelable: true }));
          await new Promise(resolve => setTimeout(resolve, 0));
          const link = document.getElementById('dashboardLink');
          const button = document.querySelector('#cadastroForm button[type=submit]');
          const field = document.getElementById('cadNome');
          document.getElementById('test-result').textContent = JSON.stringify({
            profile: getComputedStyle(document.getElementById('profileSection')).display,
            back: link?.getAttribute('href') ?? null, backVisible: !!link?.getClientRects().length,
            session: AppSession.current.read(), current: window.${serviceName}.${perfil === 'ALUNO' ? 'getAlunoLogado' : 'getEmpresaLogada'}(),
            request: window.requests.find(request => request.options.method === 'POST'),
            header: getComputedStyle(document.querySelector('.navbar')).backgroundColor,
            button: getComputedStyle(button).backgroundColor, buttonText: getComputedStyle(button).color,
            fieldHeight: getComputedStyle(field).minHeight,
            overflow: document.documentElement.scrollWidth > innerWidth
          });
        });
      </script></body>`));
      if (!result) return;
      assert.equal(result.profile, 'flex');
      assert.equal(result.back, 'painel.html');
      assert.equal(result.backVisible, true);
      assert.deepEqual(result.session, { id: 37, nome: 'Conta Nova', email: 'nova@exemplo.com', perfil });
      assert.deepEqual(result.current, result.session);
      const submitted = JSON.parse(result.request.options.body);
      assert.equal(submitted.senha, 'senha-nova');
      assert.equal(submitted.nome, 'Conta Nova');
      assert.equal(result.header, 'rgb(22, 51, 0)');
      assert.equal(result.button, 'rgb(159, 232, 112)');
      assert.ok(contrastRatio(result.buttonText, result.button) >= 4.5);
      assert.ok(parseFloat(result.fieldHeight) >= 48);
      assert.equal(result.overflow, false);
    });
  }
}
