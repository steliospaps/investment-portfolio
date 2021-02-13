package io.github.steliospaps.experimental.investment.invest.rebalance.pricing;

import java.math.BigDecimal;
import io.github.steliospaps.experimental.investment.invest.rebalance.actions.MarketPriceRequest;
import io.github.steliospaps.experimental.investment.invest.rebalance.actions.RebalanceAction;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.MarketPrice;
import io.vavr.Tuple;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Either;
import io.vavr.control.Option;

public class MarketDataPricer implements Pricer {

	private final Map<String, MarketPrice> prices;

	public MarketDataPricer(List<MarketPrice> marketPrices) {
		this.prices = marketPrices.map(mp -> Tuple.of(mp.getInstrumentId(), mp)).collect(HashMap.collector());
	}

	@Override
	public Either<List<RebalanceAction>, BigDecimal> getBid(String instrumentId) {
		return prices.get(instrumentId)//
				.flatMap(mp -> Option.of(mp.getBid()))//
				.toEither(()-> List.of(
						MarketPriceRequest.builder().instrumentId(instrumentId).build().withNarrative("while getting bid")));
	}

	@Override
	public Either<List<RebalanceAction>, BigDecimal> getAsk(String instrumentId) {
		return prices.get(instrumentId)//
				.flatMap(mp -> Option.of(mp.getAsk()))//
				.toEither(()-> List.of(
						MarketPriceRequest.builder().instrumentId(instrumentId).build().withNarrative("while getting ask")));
	}


}
