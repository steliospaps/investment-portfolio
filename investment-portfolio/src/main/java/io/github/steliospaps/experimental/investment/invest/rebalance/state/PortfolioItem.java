package io.github.steliospaps.experimental.investment.invest.rebalance.state;

import java.math.BigDecimal;

import lombok.Value;

@Value(staticConstructor = "of")
public class PortfolioItem {
	private final String InstrumentId;
	/**
	 * the quantity ratio of this instrument
	 */
	private final BigDecimal quantityRatio;
}
