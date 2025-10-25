package ecommerce.service;

import ecommerce.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes Funcionais da Caixa-Preta baseados na técnica de Particionamento de Equivalência.
 *
 * Objetivo: Testar um valor representativo de cada partição de equivalência identificada
 * nos requisitos para garantir que a lógica de negócio principal para cada faixa funciona
 * como esperado.
 */
@DisplayName("Teste de Partição de Equivalência para Cálculo de Custo Total")
class ParticoesTest {

    private CompraService compraService;
    private Cliente clienteBronzeSudeste; // Cliente base para não interferir com descontos de frete

    @BeforeEach
    void setUp() {
        // Inicializa o serviço e dados comuns antes de cada teste [cite: 752]
        // Usamos um cliente BRONZE e região SUDESTE como padrão para isolar os testes
        // de frete e descontos, focando em uma variável por vez.
        compraService = new CompraService(null, null, null, null); // Dependências mockadas/nulas pois não são usadas em calcularCustoTotal
        clienteBronzeSudeste = new Cliente(1L, "Cliente Bronze", Regiao.SUDESTE, TipoCliente.BRONZE);
    }

    // --- PARTIÇÕES PARA DESCONTO POR VALOR DO CARRINHO ---

    @Test
    @DisplayName("Partição (Subtotal < 500): Deve calcular o total sem aplicar desconto por valor")
    void calcularCustoTotal_QuandoSubtotalNaFaixaSemDesconto_NaoDeveAplicarDescontoDeValor() {
        // Arrange: Um produto que resulta em subtotal de R$ 300 (dentro da partição < 500)
        Produto produto = new Produto(1L, "Produto Barato","descricao", new BigDecimal("300.00"), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.ELETRONICO);
        ItemCompra item = new ItemCompra(1L,produto, 1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1l,clienteBronzeSudeste, Collections.singletonList(item), null);

        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteBronzeSudeste.getRegiao(), clienteBronzeSudeste.getTipo());

        // Assert: Custo total deve ser apenas o subtotal (frete isento para 1kg)
        assertThat(custoTotal)
                .as("Custo total para subtotal de R$ 300.00 deve ser R$ 300.00 (sem desconto por valor)")
                .isEqualByComparingTo("300.00"); // [cite: 754]
    }

    @Test
    @DisplayName("Partição (500 <= Subtotal <= 1000): Deve aplicar 10% de desconto por valor")
    void calcularCustoTotal_QuandoSubtotalNaFaixaDeDezPorcento_DeveAplicarDescontoCorretamente() {
        // Arrange: Um produto que resulta em subtotal de R$ 700 (dentro da partição 500-1000)
        Produto produto = new Produto(1L, "Produto Médio", "descricao",new BigDecimal("700.00"), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.LIVRO);
        ItemCompra item = new ItemCompra(1L,produto, 1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1l,clienteBronzeSudeste, Collections.singletonList(item), null);

        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteBronzeSudeste.getRegiao(), clienteBronzeSudeste.getTipo());

        // Assert: Custo total deve ser R$ 700 - 10% = R$ 630.00
        assertThat(custoTotal)
                .as("Custo total para subtotal de R$ 700.00 deve ser R$ 630.00 (10% de desconto)")
                .isEqualByComparingTo("630.00");
    }

    @Test
    @DisplayName("Partição (Subtotal > 1000): Deve aplicar 20% de desconto por valor")
    void calcularCustoTotal_QuandoSubtotalNaFaixaDeVintePorcento_DeveAplicarDescontoCorretamente() {
        // Arrange: Um produto com subtotal de R$ 1200 (dentro da partição > 1000)
        Produto produto = new Produto(1L, "Produto Caro","descricao", new BigDecimal("1200.00"), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.MOVEL);
        ItemCompra item = new ItemCompra(1L,produto, 1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1l,clienteBronzeSudeste, Collections.singletonList(item), null);

        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteBronzeSudeste.getRegiao(), clienteBronzeSudeste.getTipo());

        // Assert: Custo total deve ser R$ 1200 - 20% = R$ 960.00
        assertThat(custoTotal)
                .as("Custo total para subtotal de R$ 1200.00 deve ser R$ 960.00 (20% de desconto)")
                .isEqualByComparingTo("960.00");
    }

    // --- PARTIÇÕES PARA FRETE POR FAIXA DE PESO ---

    @Test
    @DisplayName("Partição (Peso <= 5kg): Deve ter frete isento")
    void calcularCustoTotal_QuandoPesoNaFaixaIsenta_DeveTerFreteZero() {
        // Arrange: Um produto com peso representativo de 3kg
        Produto produto = new Produto(1L, "Produto Leve", "descricao",new BigDecimal("100.00"), new BigDecimal("3.00"), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.ROUPA);
        ItemCompra item = new ItemCompra(1L,produto, 1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1l,clienteBronzeSudeste, Collections.singletonList(item), null);

        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteBronzeSudeste.getRegiao(), clienteBronzeSudeste.getTipo());

        // Assert: Custo total é apenas o subtotal.
        assertThat(custoTotal)
                .as("Custo total para produto de 3kg deve ser R$ 100.00 (frete isento)")
                .isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Partição (5kg < Peso <= 10kg): Deve calcular frete da Faixa B")
    void calcularCustoTotal_QuandoPesoNaFaixaB_DeveCalcularFreteCorretamente() {
        // Arrange: Um produto com peso representativo de 8kg
        Produto produto = new Produto(1L, "Produto Médio", "descricao",new BigDecimal("100.00"), new BigDecimal("8.00"), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.ROUPA);
        ItemCompra item = new ItemCompra(1L,produto, 1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1l,clienteBronzeSudeste, Collections.singletonList(item), null);

        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteBronzeSudeste.getRegiao(), clienteBronzeSudeste.getTipo());

        // Assert: Frete = (8kg * R$ 2.00/kg) + R$ 12.00 (taxa mínima) = R$ 16.00 + R$ 12.00 = R$ 28.00
        // Total = R$ 100.00 (subtotal) + R$ 28.00 (frete) = R$ 128.00
        assertThat(custoTotal)
                .as("Custo total para produto de 8kg deve incluir frete de R$ 28.00")
                .isEqualByComparingTo("128.00");
    }


    @Test
    @DisplayName("Partição (10kg < Peso <= 50kg): Deve calcular frete da Faixa C")
    void calcularCustoTotal_QuandoPesoNaFaixaC_DeveCalcularFreteCorretamente() {
        // Arrange: Um produto com peso representativo de 20kg
        Produto produto = new Produto(1L, "Produto Pesado", "descricao",new BigDecimal("100.00"), new BigDecimal("20.00"), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.MOVEL);
        ItemCompra item = new ItemCompra(1L,produto, 1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1l,clienteBronzeSudeste, Collections.singletonList(item), null);

        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteBronzeSudeste.getRegiao(), clienteBronzeSudeste.getTipo());
        
        // Assert: Frete = (20kg * R$ 4.00/kg) + R$ 12.00 (taxa mínima) = R$ 80.00 + R$ 12.00 = R$ 92.00
        // Total = R$ 100.00 (subtotal) + R$ 92.00 (frete) = R$ 192.00
        assertThat(custoTotal)
                .as("Custo total para produto de 20kg deve incluir frete de R$ 92.00")
                .isEqualByComparingTo("192.00");
    }

    @Test
    @DisplayName("Partição (Peso > 50kg): Deve calcular frete da Faixa D")
    void calcularCustoTotal_QuandoPesoNaFaixaD_DeveCalcularFreteCorretamente() {
        // Arrange: Um produto com peso representativo de 60kg
        Produto produto = new Produto(1L, "Produto Muito Pesado","descricao", new BigDecimal("100.00"), new BigDecimal("60.00"), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.MOVEL);
        ItemCompra item = new ItemCompra(1L,produto, 1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1l,clienteBronzeSudeste, Collections.singletonList(item), null);

        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteBronzeSudeste.getRegiao(), clienteBronzeSudeste.getTipo());
        
        // Assert: Frete = (60kg * R$ 7.00/kg) + R$ 12.00 (taxa mínima) = R$ 420.00 + R$ 12.00 = R$ 432.00
        // Total = R$ 100.00 (subtotal) + R$ 432.00 (frete) = R$ 532.00
        assertThat(custoTotal)
                .as("Custo total para produto de 60kg deve incluir frete de R$ 532.00")
                .isEqualByComparingTo("532.00");
    }

    @Test
@DisplayName("Partição (Peso Cúbico > Peso Físico): Deve usar peso cúbico para calcular frete")
void calcularCustoTotal_QuandoPesoCubicoMaiorQueFisico_DeveUsarPesoCubicoNoCalculo() {
    // Arrange: Um produto leve (1kg) mas muito volumoso
    // Dimensões: 50cm x 50cm x 50cm
    // Peso Cúbico = (50 * 50 * 50) / 6000 = 125000 / 6000 = 20.83 kg
    // O peso tributável deve ser 20.83 kg, não 1 kg.
    Produto produtoVolumoso = new Produto(1L, "Produto Volumoso", "descricao", new BigDecimal("100.00"), 
                                           new BigDecimal("1.00"), // Peso físico baixo
                                           new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("50.00"),false, TipoProduto.ELETRONICO); // Dimensões grandes
    ItemCompra item = new ItemCompra(1L, produtoVolumoso, 1L);
    CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1l, clienteBronzeSudeste, Collections.singletonList(item), null);

    // Act
    BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteBronzeSudeste.getRegiao(), clienteBronzeSudeste.getTipo());

    // Assert: O cálculo do frete deve usar 20.83 kg (Faixa C)
    // Frete = (20.83kg * R$ 4.00/kg) + R$ 12.00 (taxa mínima) = R$ 83.32 + R$ 12.00 = R$ 95.32
    // Total = R$ 100.00 (subtotal) + R$ 95.32 (frete) = R$ 195.32
    assertThat(custoTotal)
            .as("Custo total para produto volumoso deve ser calculado com base no peso cúbico (20.83kg)")
            .isEqualByComparingTo("195.32");
}
    
    // --- PARTIÇÕES PARA DESCONTO POR NÍVEL DE CLIENTE (SOBRE O FRETE) ---

    @Test
    @DisplayName("Partição (Cliente Ouro): Deve aplicar 100% de desconto no frete")
    void calcularCustoTotal_QuandoClienteOuro_DeveZerarOFrete() {
        // Arrange: Um cliente OURO e um produto pesado que geraria frete
        Cliente clienteOuro = new Cliente(2L, "Cliente Ouro", Regiao.SUDESTE, TipoCliente.OURO);
        Produto produtoPesado = new Produto(1L, "Produto Pesado","descricao", new BigDecimal("100.00"), new BigDecimal("15.00"), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.MOVEL);
        ItemCompra item = new ItemCompra(1L,produtoPesado, 1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1l,clienteOuro, Collections.singletonList(item), null);

        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteOuro.getRegiao(), clienteOuro.getTipo());

        // Assert: O frete seria R$ 72.00, mas é zerado. Total = R$ 100.00
        assertThat(custoTotal)
                .as("Custo total para cliente OURO deve ser apenas o subtotal, pois o frete é 100% zerado")
                .isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Partição (Cliente Prata): Deve aplicar 50% de desconto no frete")
    void calcularCustoTotal_QuandoClientePrata_DeveAplicarMetadeDoFrete() {
        // Arrange: Um cliente PRATA e um produto que gera frete de R$ 28.00
        Cliente clientePrata = new Cliente(3L, "Cliente Prata", Regiao.SUDESTE, TipoCliente.PRATA);
        Produto produtoPesado = new Produto(1L, "Produto Médio","descricao", new BigDecimal("100.00"), new BigDecimal("8.00"), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.ROUPA);
        ItemCompra item = new ItemCompra(1L,produtoPesado, 1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1l,clientePrata, Collections.singletonList(item), null);


        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clientePrata.getRegiao(), clientePrata.getTipo());

        // Assert: Frete base = R$ 28.00. Com desconto de 50% = R$ 14.00.
        // Total = R$ 100.00 + R$ 14.00 = R$ 114.00
        assertThat(custoTotal)
                .as("Custo total para cliente PRATA deve incluir 50% do valor do frete")
                .isEqualByComparingTo("114.00");
    }
    
    // --- PARTIÇÕES PARA DESCONTO POR MÚLTIPLOS ITENS DE MESMO TIPO ---

    @Test
    @DisplayName("Partição (0 a 2 itens): Não deve aplicar desconto por tipo")
    void calcularCustoTotal_Quando2ItensDoMesmoTipo_NaoDeveAplicarDescontoDeTipo() {
        // Arrange: 2 itens do mesmo tipo, que não devem acionar o desconto
        Produto produto = new Produto(1L, "Camiseta", "descricao",new BigDecimal("50.00"), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.ROUPA);
        ItemCompra item = new ItemCompra(1L, produto, 2L); // 2 unidades
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1l, clienteBronzeSudeste, Collections.singletonList(item), null);

        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteBronzeSudeste.getRegiao(), clienteBronzeSudeste.getTipo());

        // Assert: Subtotal = 2 * 50 = R$ 100.00. Nenhum desconto deve ser aplicado.
        // Total = R$ 100.00 (Frete isento para 2kg)
        assertThat(custoTotal)
                .as("Custo total para 2 itens de R$ 50 deve ser R$ 100.00 (sem desconto por tipo)")
                .isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Partição (3 a 4 itens): Deve aplicar 5% de desconto sobre os itens do mesmo tipo")
    void calcularCustoTotal_Quando4ItensDoMesmoTipo_DeveAplicar5PorcentoDeDesconto() {
        // Arrange: 4 itens do mesmo tipo
        Produto produto = new Produto(1L, "Livro", "descricao",new BigDecimal("50.00"), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.LIVRO);
        ItemCompra item = new ItemCompra(1L,produto, 4L); // 4 unidades
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1l,clienteBronzeSudeste, Collections.singletonList(item), null);

        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteBronzeSudeste.getRegiao(), clienteBronzeSudeste.getTipo());
        
        // Assert: Subtotal = 4 * 50 = R$ 200.00. Desconto de 5% sobre 200 = R$ 10.00.
        // Total = R$ 200.00 - R$ 10.00 = R$ 190.00 (Frete isento)
        assertThat(custoTotal)
                .as("Custo total para 4 itens de R$ 50 deve ser R$ 190.00 (5% de desconto)")
                .isEqualByComparingTo("190.00");
    }


    @Test
    @DisplayName("Partição (5 a 7 itens): Deve aplicar 10% de desconto sobre os itens do mesmo tipo")
    void calcularCustoTotal_Quando7ItensDoMesmoTipo_DeveAplicar10PorcentoDeDesconto() {
        // Arrange: 7 itens do mesmo tipo
        Produto produto = new Produto(1L, "Livro", "descricao",new BigDecimal("50.00"), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.LIVRO);
        ItemCompra item = new ItemCompra(1L,produto, 7L); // 7 unidades
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1l,clienteBronzeSudeste, Collections.singletonList(item), null);

        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteBronzeSudeste.getRegiao(), clienteBronzeSudeste.getTipo());
        
        // Assert: Subtotal = 7 * 50 = R$ 350.00. Desconto de 10% sobre 350 = R$ 35.00.
        // Total = R$ 350.00 - R$ 35.00 = R$ R$ 315.00 (Frete isento)
        assertThat(custoTotal)
            .as("Custo total para 7 itens de R$ 50 deve ser R$ 341.00 (R$ 315 subtotal + R$ 26 frete)")
            .isEqualByComparingTo("341.00");
    }

    @Test
    @DisplayName("Partição (8+ itens): Deve aplicar 10% de desconto sobre os itens do mesmo tipo")
    void calcularCustoTotal_Quando8ItensDoMesmoTipo_DeveAplicar15PorcentoDeDesconto() {
        // Arrange: 4 itens do mesmo tipo
        Produto produto = new Produto(1L, "Livro", "descricao",new BigDecimal("50.00"), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.LIVRO);
        ItemCompra item = new ItemCompra(1L,produto, 8L); // 8 unidades
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1l,clienteBronzeSudeste, Collections.singletonList(item), null);

        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteBronzeSudeste.getRegiao(), clienteBronzeSudeste.getTipo());
        
        // Assert: Subtotal = 8 * 50 = R$ 400.00. Desconto de 15% sobre 400 = R$ 60.00.
        // Total = R$ 400.00 - R$ 60.00 = R$ R$ 340.00 (Frete isento)
        assertThat(custoTotal)
            .as("Custo total para 8 itens de R$ 50 deve ser R$ 368.00 (R$ 340 subtotal + R$ 28 frete)")
            .isEqualByComparingTo("368.00");
    }

    // --- PARTIÇÕES PARA FRETE POR REGIÃO ---

    @Test
    @DisplayName("Partição (Região Sul): Deve aplicar multiplicador de 1.10 no frete")
    void calcularCustoTotal_QuandoRegiaoSul_DeveAplicarMultiplicadorDeFrete() {
        // Arrange: Cliente da região Sul e um produto que gera frete
        Cliente clienteSul = new Cliente(4L, "Cliente Sul", Regiao.SUL, TipoCliente.BRONZE);
        Produto produto = new Produto(1L, "Produto Médio","descricao", new BigDecimal("100.00"), new BigDecimal("8.00"), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.ROUPA);
        ItemCompra item = new ItemCompra(1L,produto, 1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1l,clienteSul, Collections.singletonList(item), null);

        
        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteSul.getRegiao(), clienteSul.getTipo());
        
        // Assert: Frete base (Sudeste) = R$ 28.00.
        // Frete Sul = R$ 28.00 * 1.05 = R$ 29.4.
        // Total = R$ 100.00 + R$ 29.04 = R$ 129.4.
        assertThat(custoTotal)
                .as("Custo total para Região Sul deve ter frete multiplicado por 1.05")
                .isEqualByComparingTo("129.4");
    }


    @Test
    @DisplayName("Partição (Região Nordeste): Deve aplicar multiplicador de 1.10 no frete")
    void calcularCustoTotal_QuandoRegiaoNordeste_DeveAplicarMultiplicadorDeFrete() {
        // Arrange: Cliente da região Nordeste e um produto que gera frete
        Cliente clienteNordeste = new Cliente(4L, "Cliente Nordeste", Regiao.NORDESTE, TipoCliente.BRONZE);
        Produto produto = new Produto(1L, "Produto Médio", "descricao",new BigDecimal("100.00"), new BigDecimal("8.00"), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.ROUPA);
        ItemCompra item = new ItemCompra(1L,produto, 1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1l,clienteNordeste, Collections.singletonList(item), null);

        
        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteNordeste.getRegiao(), clienteNordeste.getTipo());
        
        // Assert: Frete base (Sudeste) = R$ 28.00.
        // Frete Nordeste = R$ 28.00 * 1.10 = R$ 30.80.
        // Total = R$ 100.00 + R$ 30.80 = R$ 130.80.
        assertThat(custoTotal)
                .as("Custo total para Região Nordeste deve ter frete multiplicado por 1.10")
                .isEqualByComparingTo("130.80");
    }

    @Test
    @DisplayName("Partição (Região Centro-oeste): Deve aplicar multiplicador de 1.20 no frete")
    void calcularCustoTotal_QuandoRegiaoCentroOeste_DeveAplicarMultiplicadorDeFrete() {
        // Arrange: Cliente da região CentroOeste e um produto que gera frete
        Cliente clienteCentroOeste = new Cliente(4L, "Cliente CentroOeste", Regiao.CENTRO_OESTE, TipoCliente.BRONZE);
        Produto produto = new Produto(1L, "Produto Médio","descricao", new BigDecimal("100.00"), new BigDecimal("8.00"), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.ROUPA);
        ItemCompra item = new ItemCompra(1L,produto, 1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1l,clienteCentroOeste, Collections.singletonList(item), null);

        
        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteCentroOeste.getRegiao(), clienteCentroOeste.getTipo());
        
        // Assert: Frete base (Sudeste) = R$ 28.00.
        // Frete CentroOeste = R$ 28.00 * 1.20 = R$ 33.60.
        // Total = R$ 100.00 + R$ 33.60 = R$ 133.60.
        assertThat(custoTotal)
                .as("Custo total para Região CentroOeste deve ter frete multiplicado por 1.20")
                .isEqualByComparingTo("133.60");
    }

    @Test
    @DisplayName("Partição (Região Norte): Deve aplicar multiplicador de 1.30 no frete")
    void calcularCustoTotal_QuandoRegiaoNorte_DeveAplicarMultiplicadorDeFrete() {
        // Arrange: Cliente da região Norte e um produto que gera frete
        Cliente clienteNorte = new Cliente(4L, "Cliente Norte", Regiao.NORTE, TipoCliente.BRONZE);
        Produto produto = new Produto(1L, "Produto Médio","descricao", new BigDecimal("100.00"), new BigDecimal("8.00"), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, TipoProduto.ROUPA);
        ItemCompra item = new ItemCompra(1L,produto, 1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1l,clienteNorte, Collections.singletonList(item), null);

        
        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteNorte.getRegiao(), clienteNorte.getTipo());
        
        // Assert: Frete base (Sudeste) = R$ 28.00.
        // Frete Norte = R$ 28.00 * 1.30 = R$ 36.40.
        // Total = R$ 100.00 + R$ 36.40 = R$ 136.40.
        assertThat(custoTotal)
                .as("Custo total para Região Norte deve ter frete multiplicado por 1.30")
                .isEqualByComparingTo("136.40");
    }

    // --- PARTIÇÃO PARA TAXA DE ITEM FRÁGIL ---

    @Test
    @DisplayName("Partição (Item Frágil): Deve adicionar taxa de R$ 5.00 por item ao frete")
    void calcularCustoTotal_QuandoItemFragil_DeveAdicionarTaxaDeManuseioAoFrete() {
        // Arrange: Um item frágil que gera frete
        Produto produto = new Produto(1L, "Produto Frágil","descricao", new BigDecimal("100.00"), new BigDecimal("8.00"), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, true, TipoProduto.MOVEL);
        ItemCompra item = new ItemCompra(1L, produto, 1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1l,clienteBronzeSudeste, Collections.singletonList(item), null);

        // Act
        BigDecimal custoTotal = compraService.calcularCustoTotal(carrinho, clienteBronzeSudeste.getRegiao(), clienteBronzeSudeste.getTipo());

        // Assert: Frete base = R$ 28.00. Taxa frágil = R$ 5.00 * 1 = R$ 5.00.
        // Frete Total = R$ 28.00 + R$ 5.00 = R$ 33.00.
        // Custo Total = R$ 100.00 + R$ 33.00 = R$ 133.00.
        assertThat(custoTotal)
                .as("Custo total para item frágil deve incluir a taxa de R$ 5.00 no frete")
                .isEqualByComparingTo("133.00");
    }
    
}