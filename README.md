# Trabalho de Testes de Software: Finalização de Compra de E-commerce

Este projeto implementa e testa a funcionalidade de cálculo de custo total de uma compra em um sistema de e-commerce, como parte da avaliação da disciplina de Testes de Software.

**Membros do Grupo:**
* Bianca Maciel Medeiros
* Nicole Carvalho Nogueira

---

## 1. Como Executar os Testes

### Pré-requisitos
* Java JDK 17 (ou superior)
* Maven 3.8 (ou superior)

//TODO

## 3. Documentação e Projeto dos Casos de Teste de Caixa Preta

1) Identificação das Partições e Valores limites:

#### Desconto por Múltiplos Itens (mesmo tipo)
* Partições: 0-2,3-4, 5-7, 8-mais
* Valores Limites: 2, 3, 4, 5, 7, 8

#### Desconto por Valor do Carrinho
* Partições: Subtotal < R$ 500,00: Sem desconto ; R$ 500,00 ≤ Subtotal ≤ R$ 1000,00: 10% de desconto ; Subtotal > R$ 1000,00: 20% de desconto.
* Valores Limites: 499.99, 500.00, 1000.00, 1000.01

#### Cálculo do Frete Base (por Peso Total)
* Partições: 0 ≤ peso ≤ 5,00 kg: Isento ; 5,00 < peso ≤ 10,00 kg: R$ 2,00/kg ; 10,00 < peso ≤ 50,00 kg: R$ 4,00/kg ; peso > 50,00 kg: R$ 7,00/kg.
* Valores Limites: 0.00, 5.00, 5.01, 10.00, 10.01, 50.00, 50.01

#### Benefício de Nível do Cliente (no frete)
* Partições: Bronze: Paga o frete integral ; Prata: 50% de desconto no frete ; Ouro: 100% de desconto no frete.

#### Multiplicador de Frete por Região
* Partições: Sudeste: 1,00 ; Sul: 1,05 ; Nordeste: 1,10 ; Centro-Oeste: 1,20 ; Norte: 1,30.

#### Taxa Item Frágil
* Partições: Item é frágil: Adiciona R$ 5,00 por quantidade ao frete ; Item não é frágil: Nenhuma taxa extra.

#### Taxa Mínima Frete
* Partições: Peso > 5,00 kg: Soma taxa mínima de R$ 12,00 ; Peso ≤ 5,00 kg: Não soma taxa mínima.
* Valores Limites: 5.00, 5.01

2) Tabela de Decisão:
   ![tabelaDecisao](./assets/tabelaDeDecisao.png)


4) Documentação dos casos de teste relacionando ID do teste, entrada, resultado esperado e critério coberto (partição, limite ou regra de decisão):

## 4. Documentação e Projeto dos Casos de Teste de Caixa Branca

---
