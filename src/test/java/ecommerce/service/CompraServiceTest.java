package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.entity.TipoProduto;

import java.math.BigDecimal;
import java.util.ArrayList;

public class CompraServiceTest {
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
	@DisplayName("TD-07: Subtotal <500, Frete Isento, Sem Descontos")
	public void calcularCustoTotal() {

		Produto p1 = criarProduto("p1", new BigDecimal("300.00"), new BigDecimal("4.00"), TipoProduto.ROUPA, false);
		adicionarItem(p1, 1L);

		Regiao regiao = Regiao.SUDESTE;
		TipoCliente cliente = TipoCliente.BRONZE;

		BigDecimal custoTotal = service.calcularCustoTotal(carrinho, regiao, cliente);

		assertThat(custoTotal).as("Regra 7: Cenário base, frete isento, sem descontos")
				.isEqualByComparingTo("300.00");
	}

	@Test
	@DisplayName("TD-01: Subtotal >1000, 8+ itens, Peso >5kg, Ouro, Não-Frágil, Sudeste")
	public void calcularCustoTotal_Regra_1() {
		Produto p = criarProduto("p1", new BigDecimal("120.00"), new BigDecimal("1.00"), TipoProduto.ROUPA, false);
		adicionarItem(p, 10L);

		BigDecimal custoTotal = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.OURO);
		assertThat(custoTotal).as("Regra 1: Ouro zera frete, descontos de 15%% e 20%% aplicados")
				.isEqualByComparingTo("816.00");
	}

	@Test
	@DisplayName("TD-02: Subtotal >1000, 8+ itens, Peso >5kg, Prata, Não-Frágil, Sudeste")
	public void calcularCustoTotal_Regra_2() {
		Produto p = criarProduto("p1", new BigDecimal("120.00"), new BigDecimal("1.00"), TipoProduto.ROUPA, false);
		adicionarItem(p, 10L);

		BigDecimal custoTotal = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.PRATA);
		assertThat(custoTotal).as("Regra 2: Prata paga 50%% frete, descontos de 15%% e 20%% aplicados")
				.isEqualByComparingTo("832.00");
	}

	@Test
	@DisplayName("TD-03: Subtotal 500-1000, 5-7 itens, Peso >5kg, Bronze, Não-Frágil, Não-Sudeste")
	public void calcularCustoTotal_Regra_3() {
		Produto p = criarProduto("p1", new BigDecimal("100.00"), new BigDecimal("1.00"), TipoProduto.LIVRO, false);
		adicionarItem(p, 6L);

		BigDecimal custoTotal = service.calcularCustoTotal(carrinho, Regiao.NORDESTE, TipoCliente.BRONZE);
		assertThat(custoTotal).as("Regra 3: Bronze paga frete integral, descontos de 10%% e 10%% aplicados")
				.isEqualByComparingTo("512.40");
	}

	@Test
	@DisplayName("TD-04: Subtotal 500-1000, 3-4 itens, Peso <5kg, Ouro, Não-Frágil, Sudeste")
	public void calcularCustoTotal_Regra_4() {
		Produto p = criarProduto("p1", new BigDecimal("200.00"), new BigDecimal("1.00"), TipoProduto.MOVEL, false);
		adicionarItem(p, 4L);

		BigDecimal custoTotal = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.OURO);
		assertThat(custoTotal).as("Regra 4: Frete isento por peso, Ouro zera, descontos de 5%% e 10%% aplicados")
				.isEqualByComparingTo("684.00");
	}

	@Test
	@DisplayName("TD-05: Subtotal <500, 3-4 itens, Peso <5kg, Prata, Não-Frágil, Sudeste")
	public void calcularCustoTotal_Regra_5() {
		Produto p = criarProduto("p1", new BigDecimal("100.00"), new BigDecimal("1.00"), TipoProduto.ALIMENTO, false);
		adicionarItem(p, 4L);

		BigDecimal custoTotal = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.PRATA);
		assertThat(custoTotal).as("Regra 5: Frete isento, sem desconto valor, desconto 5%% tipo aplicado")
				.isEqualByComparingTo("380.00");
	}

	@Test
	@DisplayName("TD-06: Subtotal <500, 0-2 itens, Peso >5kg, Bronze, Frágil, Sudeste")
	public void calcularCustoTotal_Regra_6() {
		Produto p = criarProduto("TV", new BigDecimal("300.00"), new BigDecimal("6.00"), TipoProduto.ELETRONICO, true);
		adicionarItem(p, 1L);

		BigDecimal custoTotal = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);
		assertThat(custoTotal).as("Regra 6: Paga frete faixa B + taxa frágil, sem descontos subtotal")
				.isEqualByComparingTo("329.00");
	}

	@Test
	@DisplayName("TD-08: Subtotal >1000, 8+ itens, Peso >5kg, Bronze, Não-Frágil, Não-Sudeste")
	public void calcularCustoTotal_Regra_8() {
		Produto p = criarProduto("p1", new BigDecimal("120.00"), new BigDecimal("1.00"), TipoProduto.ROUPA, false);
		adicionarItem(p, 10L);

		BigDecimal custoTotal = service.calcularCustoTotal(carrinho, Regiao.NORTE, TipoCliente.BRONZE);
		assertThat(custoTotal).as("Regra 8: Bronze paga frete integral com multiplicador, descontos de 15%% e 20%%")
				.isEqualByComparingTo("857.60");
	}

}