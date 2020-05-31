package io.github.steliospaps.experimental.investment.invest.model;

import java.math.BigDecimal;

import lombok.Value;

@Value(staticConstructor = "of")
public class PortfolioItem {
	private final String InstrumentId;
	private final BigDecimal ratio;
}
