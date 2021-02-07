package io.github.steliospaps.experimental.investment.invest.rebalance;

import io.vavr.collection.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

/**
 * additional actions are required 
 * @author stelios
 *
 */
@Value
@Builder
public class RebalanceActions {
	@Default
	private List<MarketPriceRequest> marketPriceRequests=List.empty();
	
}
