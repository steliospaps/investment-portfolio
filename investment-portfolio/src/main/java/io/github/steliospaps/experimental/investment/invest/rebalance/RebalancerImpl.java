package io.github.steliospaps.experimental.investment.invest.rebalance;

import org.springframework.stereotype.Component;

@Component
public class RebalancerImpl implements Rebalancer {

	@Override
	public RebalanceActions rebalance(RebalanceState current) {
		return RebalanceActions//
				.builder()//
				.marketPriceRequests(current.getFunds()
						.map(Fund::getPortfolio)
						.flatMap(Portfolio::getItems)
						.map(PortfolioItem::getInstrumentId)
						.distinct()
						.map(MarketPriceRequest::of)
						)
				.build();
	}

}
