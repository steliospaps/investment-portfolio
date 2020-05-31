package io.github.steliospaps.experimental.investment.invest.model;

import io.vavr.collection.List;
import lombok.Value;

@Value(staticConstructor = "of")
public class Portfolio {
	private final List<PortfolioItem> items;
}
