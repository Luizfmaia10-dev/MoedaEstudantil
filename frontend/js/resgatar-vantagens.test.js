const test = require('node:test');
const assert = require('node:assert/strict');
const { filtrarVantagens, catalogViewModel } = require('./resgatar-vantagens.js');

test('busca por nome, descrição ou empresa sem diferenciar acentos', () => {
  const itens = [{ nome: 'Café grátis', descricao: 'Bebida', empresaNome: 'Cantina' }];

  assert.equal(filtrarVantagens(itens, 'cafe', false, 0).length, 1);
  assert.equal(filtrarVantagens(itens, 'cantina', false, 0).length, 1);
  assert.equal(filtrarVantagens(itens, 'lanche', false, 0).length, 0);
});

test('filtro de saldo mantém somente vantagens resgatáveis', () => {
  const itens = [{ custoMoedas: 20 }, { custoMoedas: 80 }];

  assert.deepEqual(filtrarVantagens(itens, '', true, 50), [itens[0]]);
  assert.deepEqual(catalogViewModel(itens[1], 50), {
    disabled: true,
    buttonLabel: 'Saldo insuficiente',
    availability: 'Faltam 30 moedas'
  });
});

test('vantagem acessível informa que pode ser resgatada', () => {
  assert.deepEqual(catalogViewModel({ custoMoedas: 20 }, 50), {
    disabled: false,
    buttonLabel: 'Resgatar',
    availability: 'Disponível para resgate'
  });
});
