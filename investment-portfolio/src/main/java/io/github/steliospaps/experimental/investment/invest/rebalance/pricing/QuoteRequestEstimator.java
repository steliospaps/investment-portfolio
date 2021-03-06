package io.github.steliospaps.experimental.investment.invest.rebalance.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.github.steliospaps.experimental.investment.invest.rebalance.actions.QuoteRequest;
import io.github.steliospaps.experimental.investment.invest.rebalance.actions.RebalanceAction;
import io.vavr.Tuple;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Either;

/**
 * estimate what quotes to request
 * @author stelios
 *
 */
public class QuoteRequestEstimator {

	private final Map<String, Either<List<RebalanceAction>, BigDecimal>> estimatedAgregateQuantityDelta;
	private final BigDecimal overSizeQuoteRatio;

	public QuoteRequestEstimator(Map<String, Either<List<RebalanceAction>, BigDecimal>> estimatedAgregateQuantityDelta, 
			BigDecimal overSizeQuoteRatio){
				this.estimatedAgregateQuantityDelta = estimatedAgregateQuantityDelta;
				this.overSizeQuoteRatio = overSizeQuoteRatio;
	}
	
	/**
	 * 
	 * @param instrumentId
	 * @return the quote to request or the {@link RebalanceAction}s that will allow it to calculate what to request. 
	 */
	public List<RebalanceAction> estimateQuoteRequest(String instrumentId){
		return estimatedAgregateQuantityDelta.get(instrumentId)
			.getOrElseThrow(() -> new RuntimeException("This should never happen: "
					+ "we have to get a quote for "+instrumentId+" but we have no estimatedAgregateQuantityDelta entry"))//
			.map(quantity-> Tuple.of(calculateQuoteRequestQuantity(quantity),quantity))//
			.fold(RebalanceAction.addNarrative("while estimating quote request size")
					, tup -> List.of(QuoteRequest.builder()//
					.instrumentId(instrumentId)//
					.quantity(tup._1)//
					.build()//
					.withNarrative("estimatedAgregateQuantityDelta="+tup._2)));
	}

	private int calculateQuoteRequestQuantity(BigDecimal quantity) {
		return quantity.multiply(BigDecimal.ONE.add(overSizeQuoteRatio)).setScale(0,RoundingMode.UP).intValueExact();
	}

}
