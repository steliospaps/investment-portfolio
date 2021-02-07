package io.github.steliospaps.experimental.investment.invest.rebalance.actions;

import io.vavr.collection.List;
import lombok.Value;

/**
 * additional actions are required 
 * @author stelios
 *
 */
@Value(staticConstructor = "of")
public class QuoteRequests implements RebalanceActions{
	private List<QuoteRequest> requests;
}
