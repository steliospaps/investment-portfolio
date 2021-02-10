package io.github.steliospaps.experimental.investment.invest.rebalance.state;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;

@Value(staticConstructor = "of")
public class SystemAccountStockItemWithPrice {
	private String instrumentId;
	private BigDecimal quantity;
	private BigDecimal price;
}
