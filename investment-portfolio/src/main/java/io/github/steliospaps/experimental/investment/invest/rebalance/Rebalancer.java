package io.github.steliospaps.experimental.investment.invest.rebalance;

public interface Rebalancer {
	RebalanceActions rebalance(RebalanceState current);
}
