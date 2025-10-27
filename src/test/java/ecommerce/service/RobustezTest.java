package ecommerce.service;

import ecommerce.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

public class RobustezTest {
    private CompraService service;
    private CarrinhoDeCompras carrinho;

    @BeforeEach
    public void setup() {
        // Instancia o serviço. Passamos 'null' para as dependências que o método 'calcularCustoTotal' não usa
        service = new CompraService(null, null, null, null);

        // Cria um novo carrinho vazio para cada teste
        carrinho = new CarrinhoDeCompras();
        carrinho.setItens(new ArrayList<>());
    }

    private Produto criarProduto(String nome, BigDecimal preco, BigDecimal pesoFisico, TipoProduto tipo,
                                 boolean fragil) {
        // Peso Cúbico = (10*10*10) / 6000 = 0.17kg
        // Ao usar pesoFisico > 0.17, garantimos que o pesoFisico será o pesoTributável
        return new Produto(null, nome, "Desc", preco,
                pesoFisico,
                new BigDecimal("10.0"), // comprimento
                new BigDecimal("10.0"), // largura
                new BigDecimal("10.0"), // altura
                fragil,
                tipo);
    }

    private void adicionarItem(Produto p, long quantidade) {
        ItemCompra item = new ItemCompra(null, p, quantidade);
        carrinho.getItens().add(item);
    }
    @Test
    @DisplayName("RB-01: Lança exceção se Carrinho for nulo")
    public void calcularCustoTotal_carrinhoNulo_lancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> {
                    service.calcularCustoTotal(null, Regiao.SUDESTE, TipoCliente.BRONZE);
                })
                .withMessage("Carrinho vazio ou não encontrado.");
    }

    @Test
    @DisplayName("RB-02: Lança exceção se Regiao for nula")
    public void calcularCustoTotal_regiaoNula_lancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> {
                    service.calcularCustoTotal(carrinho, null, TipoCliente.BRONZE);
                })
                .withMessage("Região ou cliente não identificados.");
    }

    @Test
    @DisplayName("RB-03: Lança exceção se Quantidade do item for <= 0")
    public void calcularCustoTotal_itemQuantidadeZero_lancaIllegalArgumentException() {
        adicionarItem(criarProduto("p1", new BigDecimal("100.00"), new BigDecimal("1.00"), TipoProduto.ROUPA, false),
                0L);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> {
                    service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);
                })
                .withMessage("Quantidade do item deve ser maior que zero.");
    }

    @Test
    @DisplayName("RB-04: Lança exceção se Preço do produto for negativo")
    public void calcularCustoTotal_itemPrecoNegativo_lancaIllegalArgumentException() {
        adicionarItem(criarProduto("p1", new BigDecimal("-100.00"), new BigDecimal("1.00"), TipoProduto.ROUPA, false),
                1L);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> {
                    service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);
                })
                .withMessage("Preço do produto deve ser maior que zero.");
    }

    @Test
    @DisplayName("RB-05: Lança exceção se lista de itens do carrinho for nula")
    public void calcularCustoTotal_listaItensNula_lancaIllegalArgumentException() {

        carrinho.setItens(null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> {
                    service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);
                })
                .withMessage("Carrinho vazio ou não encontrado.");
    }

    @Test
    @DisplayName("RB-06: Lança exceção se TipoCliente for nulo")
    public void calcularCustoTotal_tipoClienteNulo_lancaIllegalArgumentException() {
        adicionarItem(criarProduto("p1", new BigDecimal("10.00"), BigDecimal.ONE, TipoProduto.LIVRO, false), 1L);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> {
                    service.calcularCustoTotal(carrinho, Regiao.SUDESTE, null);
                })
                .withMessage("Região ou cliente não identificados.");
    }

    @Test
    @DisplayName("RB-07: Lança exceção se Quantidade do item for null")
    public void calcularCustoTotal_itemQuantidadeNull_lancaIllegalArgumentException() {
        Produto p = criarProduto("p1", new BigDecimal("100.00"), BigDecimal.ONE, TipoProduto.ROUPA, false);
        ItemCompra itemComQtdNull = new ItemCompra(null, p, null); // Força quantidade null
        carrinho.getItens().add(itemComQtdNull);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> {
                    service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);
                })
                .withMessage("Quantidade do item deve ser maior que zero.");
    }

    @Test
    @DisplayName("RB-08: Lança exceção se Preço do produto for null")
    public void calcularCustoTotal_itemPrecoNull_lancaIllegalArgumentException() {
        Produto pComPrecoNull = criarProduto("p1", null, BigDecimal.ONE, TipoProduto.ROUPA, false);
        adicionarItem(pComPrecoNull, 1L);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> {
                    service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);
                })
                .withMessage("Preço do produto deve ser maior que zero.");
    }
}
