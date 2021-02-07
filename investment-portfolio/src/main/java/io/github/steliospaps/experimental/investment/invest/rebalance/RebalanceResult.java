package io.github.steliospaps.experimental.investment.invest.rebalance;

import io.vavr.collection.List;
import lombok.Builder;
import lombok.Value;
import lombok.Builder.Default;

/**
 * a rebalance solution has been reached.
 * @author stelios
 *
 */
@Value
@Builder
public class RebalanceResult {
	public static final RebalanceResult NOTHING = RebalanceResult.builder().build();
	@Default
	private List<Allocation> allocations=List.empty();
}
