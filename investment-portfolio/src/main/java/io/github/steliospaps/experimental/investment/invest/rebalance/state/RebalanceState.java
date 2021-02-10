package io.github.steliospaps.experimental.investment.invest.rebalance.state;

import io.vavr.collection.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;
import lombok.Value;


@Value
@Builder(toBuilder=true)
public class RebalanceState {
	@Default
	private List<Fund> funds = List.empty();
	@Default
	private List<MarketPrice> marketPrices = List.empty();
	@Default
	private RebalanceConfig config = RebalanceConfig.builder().build();
	@NonNull
	private FractionalAccount fractionalAccount;
	@NonNull
	private ControlAccount controlAccount;
	
}
