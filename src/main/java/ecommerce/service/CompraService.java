package ecommerce.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ecommerce.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;
import jakarta.transaction.Transactional;

@Service
public class CompraService
{
	private static final BigDecimal FATOR_PESO_CUBICO = new BigDecimal("6000");
	private static final BigDecimal TAXA_MINIMA_FRETE = new BigDecimal("12.00");
	private static final BigDecimal TAXA_ITEM_FRAGIL = new BigDecimal("5.00");

	private static final BigDecimal LIMITE_SUBTOTAL_DESC_10 = new BigDecimal("500.00");
	private static final BigDecimal LIMITE_SUBTOTAL_DESC_20 = new BigDecimal("1000.00");
	private static final BigDecimal DESC_10_PORCENTO = new BigDecimal("0.10");
	private static final BigDecimal DESC_20_PORCENTO = new BigDecimal("0.20");

	private static final BigDecimal PESO_FAIXA_A_MAX = new BigDecimal("5.00");
	private static final BigDecimal PESO_FAIXA_B_MAX = new BigDecimal("10.00");
	private static final BigDecimal PESO_FAIXA_C_MAX = new BigDecimal("50.00");

	private static final BigDecimal FRETE_FAIXA_B_POR_KG = new BigDecimal("2.00");
	private static final BigDecimal FRETE_FAIXA_C_POR_KG = new BigDecimal("4.00");
	private static final BigDecimal FRETE_FAIXA_D_POR_KG = new BigDecimal("7.00");

	private final CarrinhoDeComprasService carrinhoService;
	private final ClienteService clienteService;

	private final IEstoqueExternal estoqueExternal;
	private final IPagamentoExternal pagamentoExternal;

	@Autowired
	public CompraService(CarrinhoDeComprasService carrinhoService, ClienteService clienteService,
			IEstoqueExternal estoqueExternal, IPagamentoExternal pagamentoExternal)
	{
		this.carrinhoService = carrinhoService;
		this.clienteService = clienteService;

		this.estoqueExternal = estoqueExternal;
		this.pagamentoExternal = pagamentoExternal;
	}

	@Transactional
	public CompraDTO finalizarCompra(Long carrinhoId, Long clienteId)
	{
		Cliente cliente = clienteService.buscarPorId(clienteId);
		CarrinhoDeCompras carrinho = carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);

		List<Long> produtosIds = carrinho.getItens().stream().map(i -> i.getProduto().getId())
				.collect(Collectors.toList());
		List<Long> produtosQtds = carrinho.getItens().stream().map(i -> i.getQuantidade()).collect(Collectors.toList());

		DisponibilidadeDTO disponibilidade = estoqueExternal.verificarDisponibilidade(produtosIds, produtosQtds);

		if (!disponibilidade.disponivel())
		{
			throw new IllegalStateException("Itens fora de estoque.");
		}

		BigDecimal custoTotal = calcularCustoTotal(carrinho, cliente.getRegiao(), cliente.getTipo());

		PagamentoDTO pagamento = pagamentoExternal.autorizarPagamento(cliente.getId(), custoTotal.doubleValue());

		if (!pagamento.autorizado())
		{
			throw new IllegalStateException("Pagamento não autorizado.");
		}

		EstoqueBaixaDTO baixaDTO = estoqueExternal.darBaixa(produtosIds, produtosQtds);

		if (!baixaDTO.sucesso())
		{
			pagamentoExternal.cancelarPagamento(cliente.getId(), pagamento.transacaoId());
			throw new IllegalStateException("Erro ao dar baixa no estoque.");
		}

		CompraDTO compraDTO = new CompraDTO(true, pagamento.transacaoId(), "Compra finalizada com sucesso.");

		return compraDTO;
	}

	public BigDecimal calcularCustoTotal(CarrinhoDeCompras carrinho, Regiao regiao, TipoCliente tipoCliente)
	{
		if (carrinho == null || carrinho.getItens() == null) {
			throw new IllegalArgumentException("Carrinho vazio ou não encontrado.");
		}
		if (regiao == null || tipoCliente == null) {
			throw new IllegalArgumentException("Região ou cliente não identificados.");
		}

		// Cálculo do Subtotal dos Itens
		BigDecimal subtotal = calcularSubtotal(carrinho);

		// Desconto por múltiplos itens de mesmo tipo
		BigDecimal subtotalComDescontoTipo = aplicarDescontoPorTipo(carrinho, subtotal);

		// Desconto por valor de carrinho
		BigDecimal subtotalComDescontos = aplicarDescontoPorValor(subtotalComDescontoTipo);

		// Cálculo do frete base
		BigDecimal freteBase = calcularFreteBase(carrinho, regiao);

		// Benefício de nível do cliente (sobre o frete)
		BigDecimal freteFinal = aplicarDescontoClienteNoFrete(freteBase, tipoCliente);

		// Total da compra
		BigDecimal total = subtotalComDescontos.add(freteFinal);

		// Arredondamento
		return total.setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal calcularSubtotal(CarrinhoDeCompras carrinho) {
		BigDecimal subtotal = BigDecimal.ZERO;
		for (ItemCompra item : carrinho.getItens()) {

			if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
				throw new IllegalArgumentException("Quantidade do item deve ser maior que zero.");
			}
			if (item.getProduto().getPreco() == null || item.getProduto().getPreco().compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException("Preço do produto deve ser maior que zero.");
			}

			BigDecimal precoItem = item.getProduto().getPreco();
			BigDecimal quantidade = new BigDecimal(item.getQuantidade());
			subtotal = subtotal.add(precoItem.multiply(quantidade));
		}
		return subtotal;
	}

	private BigDecimal aplicarDescontoPorTipo(CarrinhoDeCompras carrinho, BigDecimal subtotalAtual) {
		Map<TipoProduto, Long> contagemPorTipo = carrinho.getItens().stream()
				.collect(Collectors.groupingBy(item -> item.getProduto().getTipo(),
						Collectors.summingLong(ItemCompra::getQuantidade)));

		BigDecimal descontoTotalTipo = BigDecimal.ZERO;

		for (Map.Entry<TipoProduto, Long> entry : contagemPorTipo.entrySet()) {
			TipoProduto tipo = entry.getKey();
			long quantidade = entry.getValue();
			BigDecimal descontoPercentual = BigDecimal.ZERO;

			if (quantidade >= 8) {
				descontoPercentual = new BigDecimal("0.15"); // 15%
			} else if (quantidade >= 5) {
				descontoPercentual = new BigDecimal("0.10"); // 10%
			} else if (quantidade >= 3) {
				descontoPercentual = new BigDecimal("0.05"); // 5%
			}

			if (descontoPercentual.compareTo(BigDecimal.ZERO) > 0) {
				BigDecimal subtotalCategoria = BigDecimal.ZERO;
				for (ItemCompra item : carrinho.getItens()) {
					if (item.getProduto().getTipo() == tipo) {
						subtotalCategoria = subtotalCategoria
								.add(item.getProduto().getPreco().multiply(new BigDecimal(item.getQuantidade())));
					}
				}
				descontoTotalTipo = descontoTotalTipo.add(subtotalCategoria.multiply(descontoPercentual));
			}
		}
		return subtotalAtual.subtract(descontoTotalTipo);
	}

	private BigDecimal aplicarDescontoPorValor(BigDecimal subtotal) {
		BigDecimal descontoPercentual = BigDecimal.ZERO;

		if (subtotal.compareTo(LIMITE_SUBTOTAL_DESC_20) > 0) { // > 1000
			descontoPercentual = DESC_20_PORCENTO; // 20%
		} else if (subtotal.compareTo(LIMITE_SUBTOTAL_DESC_10) > 0) { // > 500
			descontoPercentual = DESC_10_PORCENTO; // 10%
		}

		if (descontoPercentual.compareTo(BigDecimal.ZERO) > 0) {
			return subtotal.subtract(subtotal.multiply(descontoPercentual));
		}
		return subtotal;
	}

	private BigDecimal calcularFreteBase(CarrinhoDeCompras carrinho, Regiao regiao) {
		BigDecimal pesoTotal = calcularPesoTributavelTotal(carrinho);
		BigDecimal frete = BigDecimal.ZERO;
		BigDecimal multiplicadorRegiao = BigDecimal.ZERO;
		boolean isento = false;

		// Cálculo por faixa de peso
		if (pesoTotal.compareTo(PESO_FAIXA_A_MAX) <= 0) { // 0 <= peso <= 5.00
			isento = true; // Isento
		} else if (pesoTotal.compareTo(PESO_FAIXA_B_MAX) <= 0) { // 5.00 < peso <= 10.00
			frete = pesoTotal.multiply(FRETE_FAIXA_B_POR_KG); // R$ 2,00/kg
		} else if (pesoTotal.compareTo(PESO_FAIXA_C_MAX) <= 0) { // 10.00 < peso <= 50.00
			frete = pesoTotal.multiply(FRETE_FAIXA_C_POR_KG); // R$ 4,00/kg
		} else { // peso > 50.00
			frete = pesoTotal.multiply(FRETE_FAIXA_D_POR_KG); // R$ 7,00/kg
		}

		if (!isento) {
			frete = frete.add(TAXA_MINIMA_FRETE);
		}

		for (ItemCompra item : carrinho.getItens()) {
			if (item.getProduto().isFragil()) {
				frete = frete.add(TAXA_ITEM_FRAGIL.multiply(new BigDecimal(item.getQuantidade())));
			}
		}

		switch (regiao) {
			case SUL:
				multiplicadorRegiao = BigDecimal.valueOf(1.05);
				break; 
			case NORDESTE:
				multiplicadorRegiao = BigDecimal.valueOf(1.10);
				break; 
			case CENTRO_OESTE:
				multiplicadorRegiao = BigDecimal.valueOf(1.20);
				break; 
			case NORTE:
				multiplicadorRegiao = BigDecimal.valueOf(1.30);
				break; 
			case SUDESTE:
			default:
				multiplicadorRegiao = BigDecimal.valueOf(1.00);
				break; // Boa prática adicionar no default também
		}

		frete = frete.multiply(multiplicadorRegiao);

		return frete;
	}

	private BigDecimal aplicarDescontoClienteNoFrete(BigDecimal freteBase, TipoCliente tipoCliente) {
		switch (tipoCliente) {
			case OURO:
				return BigDecimal.ZERO; // 100% de desconto
			case PRATA:
				return freteBase.multiply(new BigDecimal("0.50")); // 50% de desconto
			case BRONZE:
			default:
				return freteBase; // Não tem desconto
		}
	}

	private BigDecimal calcularPesoTributavelTotal(CarrinhoDeCompras carrinho) {
		BigDecimal pesoTotal = BigDecimal.ZERO;
		for (ItemCompra item : carrinho.getItens()) {
			BigDecimal pesoFisico = item.getProduto().getPesoFisico();

			BigDecimal volume = item.getProduto().getComprimento().multiply(item.getProduto().getLargura()).multiply(item.getProduto().getAltura());
			BigDecimal pesoCubico = volume.divide(FATOR_PESO_CUBICO, 2, RoundingMode.HALF_UP);

			BigDecimal pesoTributavelItem = pesoFisico.max(pesoCubico);
			pesoTotal = pesoTotal.add(pesoTributavelItem.multiply(new BigDecimal(item.getQuantidade())));
		}
		return pesoTotal;
	}

}
