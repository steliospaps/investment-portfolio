package io.github.steliospaps.experimental.investment.invest.bdd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static io.vavr.Predicates.*;
import static io.vavr.API.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hamcrest.core.IsIterableContaining;
import org.springframework.beans.factory.annotation.Autowired;

import io.cucumber.datatable.DataTable;
import io.cucumber.java8.En;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.ControlAccount;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.FractionalAccount;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.Fund;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.MarketPrice;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.Portfolio;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.PortfolioItem;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.RebalanceConfig;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.RebalanceState;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.SystemAccountStockItem;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.SystemAccountStockItemWithPrice;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.RebalanceConfig.RebalanceConfigBuilder;
import io.github.steliospaps.experimental.investment.invest.rebalance.Rebalancer;
import io.github.steliospaps.experimental.investment.invest.rebalance.actions.MarketPriceRequest;
import io.github.steliospaps.experimental.investment.invest.rebalance.actions.MarketPriceRequests;
import io.github.steliospaps.experimental.investment.invest.rebalance.actions.QuoteRequest;
import io.github.steliospaps.experimental.investment.invest.rebalance.actions.QuoteRequests;
import io.github.steliospaps.experimental.investment.invest.rebalance.actions.RebalanceActions;
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
	private Map<String, Portfolio> portfolios = HashMap.empty();
	private Map<String, Fund.FundBuilder> funds = HashMap.empty();
	private Map<String, String> config = HashMap.empty();
	private List<MarketPrice> marketPrices = List.empty();
	private Either<RebalanceActions, RebalanceResult> rebalanceResult;
	private FractionalAccount fractionalAccount = FractionalAccount.builder().build();
	private ControlAccount controlAccount = ControlAccount.builder().build();

	@Autowired
	public PortfolioSteps(Rebalancer rebalancer) {

		DataTableType((java.util.Map<String, String> row) -> PortfolioItem.of(row.get("instrumentId"),
				new BigDecimal(row.get("ratio"))));

		DataTableType((java.util.Map<String, String> row) -> MarketPriceRequest.of(row.get("instrumentId")));

		DataTableType((java.util.Map<String, String> row) -> QuoteRequest
				.of(row.get("instrumentId"),Integer.parseInt(row.get("quantity"))));

		DataTableType((java.util.Map<String, String> row) -> MarketPrice.of(row.get("instrumentId"),
				new BigDecimal(row.get("bid")), new BigDecimal(row.get("ask"))));

		DataTableType((java.util.Map<String, String> row) -> SystemAccountStockItem.builder()
				.instrumentId(row.get("instrumentId"))//
				.quantity(new BigDecimal(row.get("quantity"))).build());

		DataTableType((java.util.Map<String, String> row) -> SystemAccountStockItemWithPrice.of(
				row.get("instrumentId"),//
				new BigDecimal(row.get("quantity")),
				new BigDecimal(row.get("price"))));

		DataTableType((java.util.Map<String, String> row) -> Allocation.builder()//
				.clientAccount(row.get("to"))//
				.instrumentId(row.get("instrumentId"))//
				.quantityDelta(new BigDecimal(row.get("quantity delta")))
				.price(new BigDecimal(row.get("price")))//
				.build());

			Given("control account holdings:", (io.cucumber.datatable.DataTable dataTable) -> {
				controlAccount=ControlAccount.builder().stock(List.ofAll(dataTable.asList(SystemAccountStockItemWithPrice.class)))
						.build();
			});


			Then("there are allocations:", (io.cucumber.datatable.DataTable dataTable) -> {
				List<Allocation> expected = List.ofAll(dataTable.asList(Allocation.class));
				List<Allocation> actual = rebalanceResult.map(i -> i.getAllocations()).getOrElse(List.empty());
				assertThat("actual="+actual,
						actual,
						IsIterableContainingInAnyOrder.containsInAnyOrder(expected.toJavaArray()));
			});



		
		Given("fractional account with:", (DataTable dt) -> {
			fractionalAccount = FractionalAccount.builder().stock(List.ofAll(dt.asList(SystemAccountStockItem.class)))
					.build();
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
			marketPrices = List.ofAll(dataTable.asList(MarketPrice.class));
		});
		When("the rebalancer runs", () -> {
			rebalanceResult = rebalancer.rebalance(RebalanceState.builder()//
					.funds(funds.values().map(Fund.FundBuilder::build).toList())//
					.marketPrices(marketPrices)//
					.config(toRebalancerConfig(config))//
					.fractionalAccount(fractionalAccount)//
					.controlAccount(controlAccount)//
					.build());
		});

		Then("there are no allocations", () -> {
			assertEquals(0, (int) rebalanceResult.map(i -> i.getAllocations().size()).getOrElse(0));
		});
		Then("there are no market price requests", () -> {
			assertEquals(0, (int) rebalanceResult.swap().map(i->
			Match(i).of(
					Case($(instanceOf(MarketPriceRequests.class)), j->j.getRequests().size()),
					Case($(),j->0)
					))//
					.getOrElse(0));
		});
		
		Then("market prices are requested for:", (DataTable dt) -> {

			assertEquals(dt.asList(MarketPriceRequest.class),
					rebalanceResult.swap().map(i->
					Match(i).of(
							Case($(instanceOf(MarketPriceRequests.class)), j->j.getRequests().toJavaList()),
							Case($(),j->List.of().toJavaList())
							))//
					.getOrElse(List.<RebalanceResult>of().toJavaList()));
		});

		Then("market quotes are requested for:", (DataTable dt) -> {

			assertEquals(dt.asList(QuoteRequest.class),
					rebalanceResult.swap().map(i->
					Match(i).of(
							Case($(instanceOf(QuoteRequests.class)), j->j.getRequests().toJavaList()),
							Case($(),j->List.of().toJavaList())
							))//
					.getOrElse(List.<RebalanceResult>of().toJavaList()));
		});

		Then("rebalancer is not done", () -> {
			assertTrue(rebalanceResult.isLeft());
		});

		Then("rebalancer is done", () -> {
			assertTrue(rebalanceResult.isRight());
		});

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
		throw new RuntimeException("cannot combine");
	}
}
