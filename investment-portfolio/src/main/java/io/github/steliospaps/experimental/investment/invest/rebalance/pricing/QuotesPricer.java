package io.github.steliospaps.experimental.investment.invest.rebalance.pricing;

import java.math.BigDecimal;

import io.github.steliospaps.experimental.investment.invest.rebalance.actions.RebalanceAction;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.Quote;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Either;
/**
 * get prices based on the received quotes
 * 
 * TODO: deal with net crossing? use the actual trades?
 * @author stelios
 *
 */
public class QuotesPricer implements Pricer {

	private Map<String, List<Quote>> quotes;
	private QuoteRequestEstimator quoteRequestEstimator;

	public QuotesPricer(List<Quote> quotes, QuoteRequestEstimator quoteRequestEstimator) {
		this.quoteRequestEstimator = quoteRequestEstimator;
		this.quotes = quotes.groupBy(Quote::getInstrumentId);
	}
	
	@Override
	public Either<List<RebalanceAction>, BigDecimal> getBid(String instrumentId) {
		return quotes.get(instrumentId)//
				.map(l ->l.map(Quote::getBid)
						.filter(i -> i!=null)//
						.reduce((a,b)-> a.max(b)))//
				.toEither(quoteRequestEstimator.estimateQuoteRequest(instrumentId))
				.mapLeft(RebalanceAction.addNarrative("while getting a bid"));
	}

	@Override
	public Either<List<RebalanceAction>, BigDecimal> getAsk(String instrumentId) {
		return quotes.get(instrumentId)//
				.flatMap(l ->l.map(Quote::getAsk)
						.filter(i -> i!=null)//
						.reduceOption((a,b)-> a.min(b)))//
				.toEither(quoteRequestEstimator.estimateQuoteRequest(instrumentId))
				.mapLeft(RebalanceAction.addNarrative("while getting an ask"));
	}

}
