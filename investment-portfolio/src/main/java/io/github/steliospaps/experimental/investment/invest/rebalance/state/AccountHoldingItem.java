package io.github.steliospaps.experimental.investment.invest.rebalance.state;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AccountHoldingItem {
	private String instrumentId;
	private BigDecimal quantity;
}
