package ecommerce.service;

import ecommerce.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes Funcionais da Caixa-Preta com Análise de Valor Limite,
 * utilizando Testes Parametrizados para simplificar e evitar repetição.
 */
@DisplayName("Teste de Valor Limite (Parametrizado) para Cálculo de Custo Total")
class FronteirasTest {

    private CompraService compraService;
    private Cliente clienteBronzeSudeste;

    @BeforeEach
    void setUp() {
        compraService = new CompraService(null, null, null, null);
        clienteBronzeSudeste = new Cliente(1L, "Cliente Padrão", Regiao.SUDESTE, TipoCliente.BRONZE);
    }

    @DisplayName("VL-01 a VL-04: Deve aplicar descontos corretamente nos limites de subtotal do carrinho")
    @ParameterizedTest(name = "Subtotal: {0} -> Esperado: {1}")
    @CsvSource({
            "499.99,  499.99, 'Subtotal abaixo do limite não deve ter desconto'",
            "500.00,  500.00, 'Subtotal no limite exato não deve ter desconto'",
            "1000.00, 900.00, 'Subtotal no limite superior deve ter 10% de desconto'",
            "1000.01, 800.01, 'Subtotal acima do limite deve ter 20% de desconto'"
    })
    void calcularCustoTotal_NosLimitesDeDescontoPorValor(BigDecimal subtotal, String expectedTotal, String description) {
        // Arrange
        Produto produto = new Produto(1L, "Produto Limite", "Desc", subtotal, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.LIVRO);
        ItemCompra item = new ItemCompra(1L, produto, 1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, clienteBronzeSudeste, Collections.singletonList(item), null);

        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteBronzeSudeste.getRegiao(), clienteBronzeSudeste.getTipo());

        // Assert
        assertThat(custoTotal).as(description).isEqualByComparingTo(expectedTotal);
    }

    @DisplayName("VL-05 a VL-10: Deve tratar frete corretamente em todos os limites de peso")
    @ParameterizedTest(name = "Peso: {0} kg -> Esperado: {1}")
    @CsvSource({
            "5.00, 100.00,  'Peso no limite exato da isenção não deve ter frete'",
            "5.01, 122.02,  'Peso acima do limite da isenção (Faixa B)'",
            "10.00, 132.00, 'Peso no limite superior da Faixa B'",
            "10.01, 152.04, 'Peso no início da Faixa C'",
            "50.00, 312.00, 'Peso no limite superior da Faixa C'",
            "50.01, 462.07, 'Peso no início da Faixa D'"
    })
    void calcularCustoTotal_NosLimitesDePesoDoFrete(BigDecimal peso, String expectedTotal, String description) {
        // Arrange
        Produto produto = new Produto(1L, "Produto Limite", "Desc", new BigDecimal("100.00"), peso, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.ROUPA);
        ItemCompra item = new ItemCompra(1L, produto, 1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, clienteBronzeSudeste, Collections.singletonList(item), null);

        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteBronzeSudeste.getRegiao(), clienteBronzeSudeste.getTipo());

        // Assert
        assertThat(custoTotal).as(description).isEqualByComparingTo(expectedTotal);
    }

    @DisplayName("VL-11 a VL-16: Deve aplicar descontos corretamente em todos os limites de quantidade de itens")
    @ParameterizedTest(name = "Quantidade: {0} -> Esperado: {1}")
    @CsvSource({
            "2, 100.00, '2 itens não devem receber desconto'",
            "3, 142.50, '3 itens (limite) devem receber 5% de desconto'",
            "4, 190.00, '4 itens ainda devem receber 5% de desconto'",
            "5, 225.00, '5 itens (limite) devem receber 10% de desconto'",
            "7, 341.00, '7 itens (limite) ainda devem receber 10% de desconto'", // Frete de 7kg = R$26
            "8, 368.00, '8 itens (limite) devem receber 15% de desconto'"  // Frete de 8kg = R$28
    })
    void calcularCustoTotal_NosLimitesDeDescontoPorQuantidade(long quantidade, String expectedTotal, String description) {
        // Arrange
        Produto produto = new Produto(1L, "Produto Limite", "Desc", new BigDecimal("50.00"), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.LIVRO);
        ItemCompra item = new ItemCompra(1L, produto, quantidade);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, clienteBronzeSudeste, Collections.singletonList(item), null);

        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteBronzeSudeste.getRegiao(), clienteBronzeSudeste.getTipo());

        // Assert
        assertThat(custoTotal).as(description).isEqualByComparingTo(expectedTotal);
    }
}