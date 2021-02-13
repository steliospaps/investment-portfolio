package io.github.steliospaps.experimental.investment.invest.rebalance.actions;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class TradeRequest implements RebalanceAction{
	@NonNull
	private String quoteId;
	@NonNull
	private String instrumentId;
	@NonNull
	private BigDecimal price;
	private int quantity;
}
