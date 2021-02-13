package io.github.steliospaps.experimental.investment.invest.bdd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.springframework.beans.factory.annotation.Autowired;
import io.cucumber.datatable.DataTable;
import io.cucumber.java8.En;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.ControlAccount;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.FractionalAccount;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.Fund;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.MarketPrice;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.Portfolio;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.PortfolioItem;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.Quote;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.RebalanceConfig;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.RebalanceState;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.SystemAccountStockItem;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.SystemAccountStockItemWithPrice;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.RebalanceConfig.RebalanceConfigBuilder;
import io.github.steliospaps.experimental.investment.invest.rebalance.Rebalancer;
import io.github.steliospaps.experimental.investment.invest.rebalance.actions.MarketPriceRequest;
import io.github.steliospaps.experimental.investment.invest.rebalance.actions.QuoteRequest;
import io.github.steliospaps.experimental.investment.invest.rebalance.actions.RebalanceAction;
import io.github.steliospaps.experimental.investment.invest.rebalance.actions.TradeRequest;
import io.github.steliospaps.experimental.investment.invest.rebalance.result.Allocation;
import io.github.steliospaps.experimental.investment.invest.rebalance.result.RebalanceResult;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Either;

//https://github.com/cucumber/cucumber-jvm/blob/master/examples/java8-calculator/src/test/java/io/cucumber/examples/java8/ShoppingSteps.java
//https://cucumber.io/docs/gherkin/reference/
//https://github.com/cucumber/cucumber/tree/master/datatable
public class PortfolioSteps implements En {
	private Either<List<RebalanceAction>, RebalanceResult> rebalanceResult;
	private RebalanceState.RebalanceStateBuilder rebalanceState;
	private Map<String, Portfolio> portfolios;
	private Map<String, Fund.FundBuilder> funds;
	private Map<String, String> config;

	@Autowired
	public PortfolioSteps(Rebalancer rebalancer) {

		DataTableType((java.util.Map<String, String> row) -> PortfolioItem.of(row.get("instrumentId"),
				new BigDecimal(row.get("ratio"))));

		DataTableType((java.util.Map<String, String> row) -> MarketPriceRequest.builder()
				.instrumentId(row.get("instrumentId"))//
						.build());

		DataTableType((java.util.Map<String, String> row) -> QuoteRequest.builder()
				.instrumentId(row.get("instrumentId"))//
				.quantity(Integer.parseInt(row.get("quantity")))//
				.build());

		DataTableType((java.util.Map<String, String> row) -> MarketPrice.of(row.get("instrumentId"),
				new BigDecimal(row.get("bid")), new BigDecimal(row.get("ask"))));

		DataTableType((java.util.Map<String, String> row) -> SystemAccountStockItem.builder()
				.instrumentId(row.get("instrumentId"))//
				.quantity(new BigDecimal(row.get("quantity"))).build());

		DataTableType((java.util.Map<String, String> row) -> SystemAccountStockItemWithPrice.of(row.get("instrumentId"), //
				new BigDecimal(row.get("quantity")), new BigDecimal(row.get("price"))));

		DataTableType((java.util.Map<String, String> row) -> Allocation.builder()//
				.clientAccount(row.get("to"))//
				.instrumentId(row.get("instrumentId"))//
				.quantityDelta(new BigDecimal(row.get("quantity delta"))).price(new BigDecimal(row.get("price")))//
				.build());

		DataTableType((java.util.Map<String, String> row) -> Quote.builder()//
				.quoteId(row.get("quoteId"))//
				.instrumentId(row.get("instrumentId"))//
				.bid(bigDecimalNullIfEmpty(row.get("bid")))//
				.ask(bigDecimalNullIfEmpty(row.get("ask")))//
				.build());

		DataTableType((java.util.Map<String, String> row) -> TradeRequest.builder()//
				.quoteId(row.get("quoteId"))//
				.instrumentId(row.get("instrumentId"))//
				.price(new BigDecimal(row.get("price")))//
				.quantity(Integer.parseInt(row.get("quantity")))//
				.build());

		
		
		Given("an empty rebalance state", () -> {
			rebalanceState = RebalanceState.builder()//
					.fractionalAccount(FractionalAccount.builder().build())//
					.controlAccount(ControlAccount.builder().build())//
					;
			portfolios = HashMap.empty();
			funds = HashMap.empty();
			config = HashMap.empty();
		});

		Given("quotes:", (io.cucumber.datatable.DataTable dataTable) -> {
			rebalanceState = rebalanceState.quotes(List.ofAll(dataTable.asList(Quote.class)));
		});

		Given("control account holdings:", (io.cucumber.datatable.DataTable dataTable) -> {
			rebalanceState = rebalanceState.controlAccount(ControlAccount.builder()
					.stock(List.ofAll(dataTable.asList(SystemAccountStockItemWithPrice.class))).build());
		});

		Given("fractional account with:", (DataTable dt) -> {
			rebalanceState = rebalanceState.fractionalAccount(FractionalAccount.builder()//
					.stock(List.ofAll(dt.asList(SystemAccountStockItem.class))).build());
		});

		Given("a portfolio {word} with targets:", (String portfolioId, DataTable dt) -> {
			List<PortfolioItem> portfolioItems = List.ofAll(dt.asList(PortfolioItem.class));
			portfolios = portfolios.put(portfolioId, Portfolio.of(portfolioItems));
		});

		Given("fund {word} with portfolio {word}", (String fundId, String portfolioId) -> {
			funds = funds.put(fundId, Fund.builder()//
					.accountId(fundId)//
					.portfolio(portfolios.get(portfolioId).get()));
		});

		Given("that fund {word} has {bigdecimal} available to invest", (String fundId, BigDecimal available) -> {
			funds.get(fundId).get().availableToInvest(available);
		});

		Given("config:", (DataTable dt) -> {
			dt.asLists().stream().forEach(l -> config = config.put(l.get(0), l.get(1)));
		});

		Given("market prices:", (DataTable dataTable) -> {
			rebalanceState = rebalanceState.marketPrices(List.ofAll(dataTable.asList(MarketPrice.class)));
		});
		
		When("the rebalancer runs", () -> {
			rebalanceResult = rebalancer.rebalance(rebalanceState//
					.funds(funds.values().map(Fund.FundBuilder::build).toList())//
					.config(toRebalancerConfig(config))//
					.build());
		});

		Then("there are no allocations", () -> {
			assertEquals(0, (int) rebalanceResult.map(i -> i.getAllocations().size()).getOrElse(0));
		});
		
		Then("there are no actions", ()->{
			assertTrue(rebalanceResult.isRight());
		});
		
		Then("there are allocations:", (io.cucumber.datatable.DataTable dataTable) -> {
			List<Allocation> expected = List.ofAll(dataTable.asList(Allocation.class));
			List<Allocation> actual = rebalanceResult.map(i -> i.getAllocations()).getOrElse(List.empty());
			assertThat("actual=" + actual, actual,
					IsIterableContainingInAnyOrder.containsInAnyOrder(expected.toJavaArray()));
		});

		Then("market trades are requested for:", (io.cucumber.datatable.DataTable dt) -> {
			
			List<RebalanceAction> actual = rebalanceResult.swap()
					.get();
			assertEquals(
					dt.asList(TradeRequest.class),
					actual.map(i -> i.stripNarrative()).asJava(),
					"actual="+actual);
		    
		});

		
		Then("there are no market price requests", () -> {
			assertEquals(0, (int) rebalanceResult.swap()
					.map(i ->i.size())//
					.getOrElse(0));
		});

		Then("market prices are requested for:", (DataTable dt) -> {
			List<RebalanceAction> actual = rebalanceResult.swap()
					.get();
			assertEquals(
					dt.asList(MarketPriceRequest.class),
					actual.map(i -> i.stripNarrative()).asJava(),
					"actual="+actual);
		});

		Then("market quotes are requested for:", (DataTable dt) -> {
			List<RebalanceAction> actual = rebalanceResult.swap()
					.get();
			assertEquals(
					dt.asList(QuoteRequest.class),
					actual.map(i -> i.stripNarrative()).asJava(),
					"actual="+actual);
		});

		Then("rebalancer is not done", () -> {
			assertTrue(rebalanceResult.isLeft());
		});

		Then("rebalancer is done", () -> {
			assertTrue(rebalanceResult.isRight());
		});

	}

	private BigDecimal bigDecimalNullIfEmpty(String string) {
		if(string == null || string.isBlank()) {
			return null;
		} else {
			return  new BigDecimal(string);
		}
	}

	private RebalanceConfig toRebalancerConfig(Map<String, String> config2) {
		return config2.toJavaStream().reduce(RebalanceConfig.builder(), (b, tup) -> {
			switch (tup._1) {
			case "maximumTolerableCash":
				return b.maximumTolerableCash(new BigDecimal(tup._2));
			case "maximumTolerableVarianceRatio":
				return b.maximumTolerableVarianceRatio(new BigDecimal(tup._2));
			case "overSizeQuoteRatio":
				return b.overSizeQuoteRatio(new BigDecimal(tup._2));
			default:
				throw new RuntimeException("unknown config key " + tup._1 + " in toRebalancerConfig");
			}
		}, (u1, u2) -> combine(u1, u2))//
				.build();
	}

	private RebalanceConfigBuilder combine(RebalanceConfigBuilder u1, RebalanceConfigBuilder u2) {
		throw new RuntimeException(
				"cannot combine. this should never be called. It is here only becasue the interface requires it");
	}
}
