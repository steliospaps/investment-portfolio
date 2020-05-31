package io.github.steliospaps.experimental.investment.invest.bdd;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

import io.cucumber.datatable.DataTable;
import io.cucumber.java8.En;
import io.github.steliospaps.experimental.investment.invest.model.Fund;
import io.github.steliospaps.experimental.investment.invest.model.Portfolio;
import io.github.steliospaps.experimental.investment.invest.model.PortfolioItem;
import io.vavr.collection.List;

//https://github.com/cucumber/cucumber-jvm/blob/master/examples/java8-calculator/src/test/java/io/cucumber/examples/java8/ShoppingSteps.java
//https://cucumber.io/docs/gherkin/reference/
//
public class PortfolioSteps implements En{
	private Map<String, Portfolio> portfolios=new HashMap<>();
	private Map<String, Fund.FundBuilder> funds=new HashMap<>();

	public PortfolioSteps() {
	
		DataTableType((Map<String, String> row) -> PortfolioItem.of(
	            row.get("instrumentId"),
	            new BigDecimal(row.get("ratio"))));
		
		Given("a portfolio {word} with targets:", 
				(String portfolioId, DataTable dt)->{
					List<PortfolioItem> portfolioItems = List.ofAll(dt.asList(PortfolioItem.class));
					portfolios.put(portfolioId,Portfolio.of(portfolioItems));
				});
		
		Given("fund {word} with portfolio {word}", (String fundId, String portfolioId) -> {
			funds.put(fundId, Fund.builder()//
					.portfolio(Optional.ofNullable(portfolios.get(portfolioId)).get()));
		});  
		
		Given("that fund {word} has {bigdecimal} available to invest", (String fundId, BigDecimal available) -> {                                                                            
			funds.get(fundId).availableToInvest(available);
		});      
		//TODO: actually test this
		
		
	}
}
