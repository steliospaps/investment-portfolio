package io.github.steliospaps.experimental.investment.invest.rebalance.state;

import io.vavr.collection.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
public class FractionalAccount {
	@Default
	private List<AccountHoldingItem> holdings = List.of();
}
