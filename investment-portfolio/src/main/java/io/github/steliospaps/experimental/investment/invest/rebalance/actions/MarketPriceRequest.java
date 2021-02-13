package io.github.steliospaps.experimental.investment.invest.rebalance.actions;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MarketPriceRequest implements RebalanceAction{
	private String instrumentId;
}
