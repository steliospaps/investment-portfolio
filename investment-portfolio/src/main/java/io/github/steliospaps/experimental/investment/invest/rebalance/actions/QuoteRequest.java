package io.github.steliospaps.experimental.investment.invest.rebalance.actions;

import lombok.Value;

@Value(staticConstructor = "of")
public class QuoteRequest {
	private String instrumentId;
	private int quantity;
}
