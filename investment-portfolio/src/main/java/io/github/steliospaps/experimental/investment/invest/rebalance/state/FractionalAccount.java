package io.github.steliospaps.experimental.investment.invest.rebalance.state;

import io.vavr.collection.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FractionalAccount {
	private List<SystemAccountStockItem> stock;
}
