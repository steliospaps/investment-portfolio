package io.github.steliospaps.experimental.investment.invest.rebalance.state;

import io.vavr.collection.List;
import lombok.Value;

@Value(staticConstructor = "of")
public class Portfolio {
	private final List<PortfolioItem> items;
}
