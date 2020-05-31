package io.github.steliospaps.experimental.investment.invest.rebalance;

import java.math.BigDecimal;

import lombok.Value;

@Value(staticConstructor = "of")
public class MarketPrice {
	String instrumentId;
	BigDecimal bid;
	BigDecimal ask;
}
