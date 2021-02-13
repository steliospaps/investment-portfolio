package io.github.steliospaps.experimental.investment.invest.rebalance.state;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class Quote {
	@NonNull
	String quoteId;
	@NonNull
	String instrumentId;
	/**
	 * can be null
	 */
	BigDecimal bid;
	/**
	 * can be null
	 */
	BigDecimal ask;
}
