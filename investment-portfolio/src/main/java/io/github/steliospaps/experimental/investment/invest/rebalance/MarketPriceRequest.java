package io.github.steliospaps.experimental.investment.invest.rebalance;

import lombok.Value;

@Value(staticConstructor = "of")
public class MarketPriceRequest {
	private String instrumentId;
}
