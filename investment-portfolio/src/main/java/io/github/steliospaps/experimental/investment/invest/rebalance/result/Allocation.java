package io.github.steliospaps.experimental.investment.invest.rebalance.result;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;

/**
 * an allocation from the control account to a client or fractional accounts.
 * positive means the client account (or fractional) bought from the control account 
 * @author stelios
 *
 */
@Value
@Builder
public class Allocation {
	/**
	 * if null this is a fractional account allocation
	 */
	private String clientAccount;
	private String instrumentId;
	private BigDecimal quantityDelta;
	private BigDecimal price;
}
