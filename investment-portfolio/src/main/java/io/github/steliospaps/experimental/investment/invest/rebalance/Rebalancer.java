package io.github.steliospaps.experimental.investment.invest.rebalance;

import io.github.steliospaps.experimental.investment.invest.rebalance.actions.RebalanceAction;
import io.github.steliospaps.experimental.investment.invest.rebalance.result.RebalanceResult;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.RebalanceState;
import io.vavr.collection.List;
import io.vavr.control.Either;

public interface Rebalancer {
	Either<List<RebalanceAction>, RebalanceResult> rebalance(RebalanceState current);
}
