package io.github.steliospaps.experimental.investment.invest.rebalance;

import static java.util.function.Predicate.not;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

import io.vavr.collection.List;
import io.vavr.control.Either;

@Component
public class RebalancerImpl implements Rebalancer {

	@Override
	public Either<RebalanceActions, RebalanceResult> rebalance(RebalanceState current) {
		
		List<String> pricedInstruments = current.getMarketPrices().map(MarketPrice::getInstrumentId);
		List<Fund> fundsRequiringActions = current.getFunds()
				.filter(f -> fundNeedsAction(f));
		if(fundsRequiringActions.isEmpty()) {
			return Either.right(RebalanceResult.NOTHING);
		}
		RebalanceActions requestMarketData = RebalanceActions//
				.builder()//
				.marketPriceRequests(fundsRequiringActions
						.map(Fund::getPortfolio)
						.flatMap(Portfolio::getItems)
						.map(PortfolioItem::getInstrumentId)
						.distinct()
						.filter(not(pricedInstruments::contains))
						.map(MarketPriceRequest::of)
						)
				.build();
		return Either.left(requestMarketData);
	}

	private boolean fundNeedsAction(Fund f) {
		return fundNeedsCachChange(f);
		//TODO add rebalance check
	}

	private boolean fundNeedsCachChange(Fund f) {
		//TODO: add some hysteresis here (from config? minInvestAmount minDivest?)
		return f.getAvailableToInvest().compareTo(BigDecimal.ZERO)!=0;
	}

}
