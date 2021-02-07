package io.github.steliospaps.experimental.investment.invest.rebalance.actions;

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
public class MarketPriceRequests implements RebalanceActions{
	@Default
	private List<MarketPriceRequest> requests=List.empty();
	
}
