package io.github.steliospaps.experimental.investment.invest.rebalance;

import io.github.steliospaps.experimental.investment.invest.rebalance.actions.RebalanceActions;
import io.github.steliospaps.experimental.investment.invest.rebalance.result.RebalanceResult;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.RebalanceState;
import io.vavr.control.Either;

public interface Rebalancer {
	Either<RebalanceActions, RebalanceResult> rebalance(RebalanceState current);
}
