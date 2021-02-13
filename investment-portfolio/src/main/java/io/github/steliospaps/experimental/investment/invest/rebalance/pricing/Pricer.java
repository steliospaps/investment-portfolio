package io.github.steliospaps.experimental.investment.invest.rebalance.pricing;

import java.math.BigDecimal;

import io.github.steliospaps.experimental.investment.invest.rebalance.actions.RebalanceAction;
import io.vavr.collection.List;
import io.vavr.control.Either;

public interface Pricer {
	Either<List<RebalanceAction>, BigDecimal> getBid(String instrumentId);
	Either<List<RebalanceAction>, BigDecimal> getAsk(String instrumentId);

	default Either<List<RebalanceAction>, BigDecimal> getMid(String instrumentId) {
		return getBid(instrumentId)//
				.flatMap(bid -> getAsk(instrumentId)//
						.mapLeft(RebalanceAction.addNarrative("while getting mid"))//
				.map(ask -> bid.add(ask).divide(new BigDecimal(2))));
	}

}
