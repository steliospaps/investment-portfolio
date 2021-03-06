package io.github.steliospaps.experimental.investment.invest.rebalance.actions;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QuoteRequest implements RebalanceAction{
	private String instrumentId;
	private int quantity;
}
