package io.github.steliospaps.experimental.investment.invest.rebalance.actions;

import lombok.Value;

@Value(staticConstructor = "of")
public class MarketPriceRequest {
	private String instrumentId;
}
