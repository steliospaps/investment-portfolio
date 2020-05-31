package io.github.steliospaps.experimental.investment.invest.model;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
public class Fund {
	private Portfolio portfolio;
	@Default
	private BigDecimal availableToInvest=BigDecimal.ZERO;
}
