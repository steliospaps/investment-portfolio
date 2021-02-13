package io.github.steliospaps.experimental.investment.invest.rebalance;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.github.steliospaps.experimental.investment.invest.rebalance.actions.RebalanceAction;
import io.github.steliospaps.experimental.investment.invest.rebalance.actions.TradeRequest;
import io.github.steliospaps.experimental.investment.invest.rebalance.pricing.QuoteRequestEstimator;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.Quote;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import lombok.NonNull;

public class TradeRequestMaker {

	private Map<String,List<Quote>> quotes;
	private QuoteRequestEstimator quoteRequestEstimator;

	public TradeRequestMaker(List<Quote> quotes, QuoteRequestEstimator quoteRequestEstimator) {
		this.quoteRequestEstimator = quoteRequestEstimator;
		this.quotes = quotes.groupBy(Quote::getInstrumentId);
	}

	public List<RebalanceAction> makeRequest(String instrument, BigDecimal allocated) {
		return (RebalancerIteration.isBuy(allocated) ?
				getBestAsk(instrument) : 
					getBestBid(instrument))
				.<TradeRequest>map(builder -> builder.quantity(calculateTradeRequestQuantity(allocated))//
						.build())//
				.map(i -> List.<RebalanceAction>of(i))//
				.getOrElse(()->quoteRequestEstimator.estimateQuoteRequest(instrument)
						.map(a ->a.withNarrative("could not find valid quote while requesting trade")));
	}
	private static final RoundingMode TRADE_REQUEST_UP = RoundingMode.UP;
	private int calculateTradeRequestQuantity(BigDecimal quantity) {
		return quantity.setScale(0,TRADE_REQUEST_UP).intValueExact();
	}

	
	private Option<TradeRequest.TradeRequestBuilder> getBestBid(String instrument) {
		return quotes.get(instrument).flatMap(l ->
				l.filter(quote -> quote.getBid()!=null)
				.sorted((q1,q2)-> q1.getBid().compareTo(q2.getBid()))
				.map(q -> TradeRequest.builder()//
						.quoteId(q.getQuoteId())//
						.instrumentId(instrument)//
						.price(q.getBid()))
				.headOption()
				);
	}

	private Option<TradeRequest.TradeRequestBuilder> getBestAsk(String instrument) {
		return quotes.get(instrument).flatMap(l ->
		l.filter(quote -> quote.getAsk()!=null)
		.sorted((q1,q2)-> q2.getAsk().compareTo(q1.getAsk()))//inverse order small is better
		.map(q -> TradeRequest.builder()//
				.quoteId(q.getQuoteId())//
				.instrumentId(instrument)//
				.price(q.getAsk()))
		.headOption()
		);
	}

}
