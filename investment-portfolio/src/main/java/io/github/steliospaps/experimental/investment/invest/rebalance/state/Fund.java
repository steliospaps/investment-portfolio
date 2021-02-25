package io.github.steliospaps.experimental.investment.invest.rebalance.state;

import java.math.BigDecimal;

import io.vavr.collection.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Fund {
	@NonNull
	private String accountId;
	@NonNull
	private Portfolio portfolio;
	@Default
	private BigDecimal availableToInvest=BigDecimal.ZERO;
	@Default
	private List<AccountHoldingItem> holdings=List.of();
}
