package io.github.steliospaps.experimental.investment.invest.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;

import io.cucumber.datatable.DataTable;
import io.cucumber.java8.En;
import io.github.steliospaps.experimental.investment.invest.rebalance.Fund;
import io.github.steliospaps.experimental.investment.invest.rebalance.MarketPrice;
import io.github.steliospaps.experimental.investment.invest.rebalance.MarketPriceRequest;
import io.github.steliospaps.experimental.investment.invest.rebalance.Portfolio;
import io.github.steliospaps.experimental.investment.invest.rebalance.PortfolioItem;
import io.github.steliospaps.experimental.investment.invest.rebalance.RebalanceActions;
import io.github.steliospaps.experimental.investment.invest.rebalance.RebalanceResult;
import io.github.steliospaps.experimental.investment.invest.rebalance.RebalanceState;
import io.github.steliospaps.experimental.investment.invest.rebalance.Rebalancer;
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
	private List<MarketPrice> marketPrices = List.empty();
	private Either<RebalanceActions, RebalanceResult> rebalanceResult;

	
	@Autowired
	public PortfolioSteps(Rebalancer rebalancer) {

		DataTableType((java.util.Map<String, String> row) -> PortfolioItem.of(row.get("instrumentId"),
				new BigDecimal(row.get("ratio"))));

		DataTableType((java.util.Map<String, String> row) -> MarketPriceRequest.of(row.get("instrumentId")));
		
		DataTableType((java.util.Map<String, String> row) -> MarketPrice.of(
				row.get("instrumentId"),
				new BigDecimal(row.get("bid")),
				new BigDecimal(row.get("ask"))
				));
		
		Given("a portfolio {word} with targets:", (String portfolioId, DataTable dt) -> {
			List<PortfolioItem> portfolioItems = List.ofAll(dt.asList(PortfolioItem.class));
			portfolios = portfolios.put(portfolioId, Portfolio.of(portfolioItems));
		});

		Given("fund {word} with portfolio {word}", (String fundId, String portfolioId) -> {
			funds = funds.put(fundId, Fund.builder()//
					.portfolio(portfolios.get(portfolioId).get()));
		});

		Given("that fund {word} has {bigdecimal} available to invest", (String fundId, BigDecimal available) -> {
			funds.get(fundId).get().availableToInvest(available);
		});
		
		Given("market prices:", (DataTable dataTable) -> {
			marketPrices = List.ofAll(dataTable.asList(MarketPrice.class));
		});
		When("the rebalancer runs", () -> {
			rebalanceResult = rebalancer.rebalance(RebalanceState.builder()//
					.funds(funds.values().map(Fund.FundBuilder::build).toList())//
					.marketPrices(marketPrices)//
					.build());
		});

		Then("there are no allocations", () -> {
			assertEquals(0, (int) rebalanceResult.map(i -> i.getAllocations().size()).getOrElse(0));
		});
		Then("there are no market price requests", () -> {
			assertEquals(0, (int) rebalanceResult.mapLeft(i-> i.getMarketPriceRequests().size()).swap().getOrElse(0));
		});
		Then("market prices are requested for:", (DataTable dt) -> {

			assertEquals(dt.asList(MarketPriceRequest.class), 
					rebalanceResult.getLeft().getMarketPriceRequests().toJavaList());
		});

		Then("rebalancer is not done", () -> {
		    assertTrue(rebalanceResult.isLeft());
		});
		
		Then("rebalancer is done", () -> {
			assertTrue(rebalanceResult.isRight());
		});

	}
}
