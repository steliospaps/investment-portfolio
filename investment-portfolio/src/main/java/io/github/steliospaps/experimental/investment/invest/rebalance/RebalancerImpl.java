package io.github.steliospaps.experimental.investment.invest.rebalance;

import org.springframework.stereotype.Component;

import io.github.steliospaps.experimental.investment.invest.rebalance.actions.RebalanceAction;
import io.github.steliospaps.experimental.investment.invest.rebalance.result.RebalanceResult;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.RebalanceState;
import io.vavr.collection.List;
import io.vavr.control.Either;

@Component
public class RebalancerImpl implements Rebalancer {
	@Override
	public Either<List<RebalanceAction>, RebalanceResult> rebalance(RebalanceState current) {
		return new RebalancerIteration(current).rebalance();
	}
}
