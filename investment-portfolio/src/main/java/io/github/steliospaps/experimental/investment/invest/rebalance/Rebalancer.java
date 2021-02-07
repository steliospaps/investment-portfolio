package io.github.steliospaps.experimental.investment.invest.rebalance;

import io.vavr.control.Either;

public interface Rebalancer {
Either<RebalanceActions, RebalanceResult> rebalance(RebalanceState current);
}
