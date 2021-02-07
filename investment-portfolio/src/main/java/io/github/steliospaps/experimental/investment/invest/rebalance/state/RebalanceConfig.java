package io.github.steliospaps.experimental.investment.invest.rebalance.state;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
public class RebalanceConfig {
	@Default
	private BigDecimal maximumTolerableCash=new BigDecimal("0.01");
	@Default
	private BigDecimal maximumTolerableVarianceRatio=new BigDecimal("0.005");
	@Default
	private BigDecimal overSizeQuoteRatio = new BigDecimal("0.2");
}
