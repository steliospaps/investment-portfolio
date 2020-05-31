package io.github.steliospaps.experimental.investment.invest.rebalance;

import io.vavr.collection.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
public class RebalanceState {
	@Default
	private List<Fund> funds = List.empty();
	@Default
	private List<MarketPrice> marketPrices = List.empty();
}
