package io.github.steliospaps.experimental.investment.invest.rebalance;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Rounding {
	private Rounding() {}
	
	public static BigDecimal roundQuantity(BigDecimal qty) {
		return qty.setScale(7, RoundingMode.DOWN).stripTrailingZeros();
	}
	/**
	 * @param value +ve means buy -ve means sell
	 * @return
	 */
	public static BigDecimal roundMoney(BigDecimal value) {
		return value.setScale(2,RoundingMode.DOWN).stripTrailingZeros();//TODO:revisit this, should we always round in favour of the house? 
	}

}
