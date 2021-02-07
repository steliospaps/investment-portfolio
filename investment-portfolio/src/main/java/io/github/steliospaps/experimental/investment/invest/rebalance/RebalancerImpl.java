package io.github.steliospaps.experimental.investment.invest.rebalance;

import static java.util.function.Predicate.not;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

import io.github.steliospaps.experimental.investment.invest.rebalance.actions.MarketPriceRequest;
import io.github.steliospaps.experimental.investment.invest.rebalance.actions.MarketPriceRequests;
import io.github.steliospaps.experimental.investment.invest.rebalance.actions.QuoteRequest;
import io.github.steliospaps.experimental.investment.invest.rebalance.actions.QuoteRequests;
import io.github.steliospaps.experimental.investment.invest.rebalance.actions.RebalanceActions;
import io.github.steliospaps.experimental.investment.invest.rebalance.result.RebalanceResult;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.Fund;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.MarketPrice;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.Portfolio;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.PortfolioItem;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.RebalanceConfig;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.RebalanceState;
import io.vavr.Tuple;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Either;
import lombok.Value;

@Component
public class RebalancerImpl implements Rebalancer {

	/**
	 * math context were we try to get the least amount of money either buy or sell
	 */
	private static final MathContext MONEY_DOWN = new MathContext(2, RoundingMode.DOWN);
	
	/**
	 * math context were we try to get the least amount of quantity either buy or sell
	 */
	private static final MathContext QUANTITY_DOWN = new MathContext(7, RoundingMode.DOWN);

	private static final MathContext QUOTE_REQUEST_UP = new MathContext(0, RoundingMode.UP);
	
	@Override
	public Either<RebalanceActions, RebalanceResult> rebalance(RebalanceState current) {
				
		List<Fund> fundsRequiringActions = current.getFunds()
				.filter(f -> fundNeedsAction(f, current.getConfig()));
		if(fundsRequiringActions.isEmpty()) {
			return Either.right(RebalanceResult.NOTHING);
		} else {
			return Either.<RebalanceActions, RebalanceState>right(current.toBuilder().funds(fundsRequiringActions).build())
					.flatMap(c -> checkMissingPrices(c))//
					.map(rebalanceState -> Tuple.of(rebalanceState, estimateAggregateDelta(rebalanceState)))//
					.flatMap(c-> checkQuotes(c._1,c._2))//
					.flatMap(c -> Either.right(RebalanceResult.NOTHING));
		}
		
		
		
		
	}

	@Value(staticConstructor = "of")
	public static class AggregateDelta{
		private Map<String,BigDecimal> deltaByInstrument;
	}
	//Instrument->QuantityDiff
	public static AggregateDelta estimateAggregateDelta(RebalanceState rebalanceState) {
		
		Map<String,BigDecimal> asks= rebalanceState.getMarketPrices().map(mp -> Tuple.of(mp.getInstrumentId(),mp.getAsk()))//
			.collect(HashMap.collector());
		return AggregateDelta.of(rebalanceState.getFunds().map(f -> getQuantityDelta(f, asks)).reduceOption((m1,m2)->m1.merge(m2, (a,b)->a.add(b)))//
			.getOrElse(HashMap.empty()));
	}

	private static Map<String, BigDecimal> getQuantityDelta(Fund f, Map<String, BigDecimal> prices) {
		Map<String,BigDecimal> quantityTargetRatiosByInstrument = f.getPortfolio().getItems()//
			.map(a -> Tuple.of(a.getInstrumentId(), a.getQuantityRatio()))//
			.collect(HashMap.collector());
		Map<String,BigDecimal> valueByInstrument=quantityTargetRatiosByInstrument//
				.map((k,v)->Tuple.of(k,v.multiply(prices.get(k)//
						.getOrElseThrow(()->new RuntimeException("getQuantityDelta failed to find price for "+k)))));
		valueByInstrument=normalise(valueByInstrument, f.getAvailableToInvest(), MONEY_DOWN);
		Map<String,BigDecimal> quantityByInstrument = valueByInstrument.map((k,v)->Tuple.of(k,v.divide(prices.get(k)//
				.getOrElseThrow(()->new RuntimeException("getQuantityDelta failed to find price for "+k)),QUANTITY_DOWN)));
		return quantityByInstrument;
	}

	private static Map<String, BigDecimal> normalise(Map<String, BigDecimal> map, BigDecimal targetSum, MathContext mathContext) {
		BigDecimal sum = map.values().reduce((a,b)->a.add(b));
		return map.map((k,v)->Tuple.of(k,targetSum.multiply(v).divide(sum,mathContext)));
	}

	private Either<RebalanceActions, RebalanceState> checkQuotes(RebalanceState c, AggregateDelta delta) {
		// TODO check if we already have quotes and if they are big enough
		return Either.left(QuoteRequests.of(
				delta.getDeltaByInstrument().map((k,v)->Tuple.of(k, toQuoteRequest(k,v,c))).values()
				.toList()));
	}
	
	QuoteRequest toQuoteRequest(String instrumentId, BigDecimal quantity, RebalanceState c) {
		return QuoteRequest.of(instrumentId,toQuoteRequestQuantity(quantity,c.getConfig().getOverSizeQuoteRatio()));
	}
	
	private int toQuoteRequestQuantity(BigDecimal quantity, BigDecimal overSizeQuoteRatio) {
		return quantity.multiply(BigDecimal.ONE.add(overSizeQuoteRatio)).round(QUOTE_REQUEST_UP).intValueExact();
	}

	private Either<RebalanceActions, RebalanceState> checkMissingPrices(RebalanceState current) {
		List<String> pricedInstruments = current.getMarketPrices().map(MarketPrice::getInstrumentId);
		MarketPriceRequests requestMarketData = MarketPriceRequests//
				.builder()//
				.requests(current.getFunds()
						.map(Fund::getPortfolio)
						.flatMap(Portfolio::getItems)
						.map(PortfolioItem::getInstrumentId)
						.distinct()
						.filter(not(pricedInstruments::contains))
						.map(MarketPriceRequest::of)
						)
				.build();
		if(!requestMarketData.getRequests().isEmpty()) {
			return Either.left(requestMarketData);
		} else {
			return Either.right(current);
		}
	}

	private boolean fundNeedsAction(Fund f, RebalanceConfig config) {
		return fundNeedsCachChange(f, config);
		//TODO add rebalance check
	}

	private boolean fundNeedsCachChange(Fund f, RebalanceConfig config) {
		//TODO: add some hysteresis here (from config? minInvestAmount minDivest?)
		return f.getAvailableToInvest().compareTo(config.getMaximumTolerableCash())>0;
	}

}
