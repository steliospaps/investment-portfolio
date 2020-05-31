package io.github.steliospaps.experimental.investment.invest.rebalance;

import static java.util.function.Predicate.not;

import java.util.function.Predicate;

import org.springframework.stereotype.Component;

import io.vavr.collection.List;

@Component
public class RebalancerImpl implements Rebalancer {

	@Override
	public RebalanceActions rebalance(RebalanceState current) {
		List<String> pricedInstruments = current.getMarketPrices().map(MarketPrice::getInstrumentId);
		return RebalanceActions//
				.builder()//
				.marketPriceRequests(current.getFunds()
						.map(Fund::getPortfolio)
						.flatMap(Portfolio::getItems)
						.map(PortfolioItem::getInstrumentId)
						.distinct()
						.filter(not(pricedInstruments::contains))
						.map(MarketPriceRequest::of)
						)
				.build();
	}

}
