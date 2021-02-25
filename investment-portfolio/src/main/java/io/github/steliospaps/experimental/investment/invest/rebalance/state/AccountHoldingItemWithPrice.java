package io.github.steliospaps.experimental.investment.invest.rebalance.state;

import java.math.BigDecimal;

import lombok.Value;

@Value(staticConstructor = "of")
public class AccountHoldingItemWithPrice {
	private String instrumentId;
	private BigDecimal quantity;
	private BigDecimal price;
}
