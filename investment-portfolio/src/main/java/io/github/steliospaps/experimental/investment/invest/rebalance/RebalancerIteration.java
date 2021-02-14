package io.github.steliospaps.experimental.investment.invest.rebalance;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import io.github.steliospaps.experimental.investment.invest.rebalance.actions.RebalanceAction;
import io.github.steliospaps.experimental.investment.invest.rebalance.pricing.MarketDataPricer;
import io.github.steliospaps.experimental.investment.invest.rebalance.pricing.Pricer;
import io.github.steliospaps.experimental.investment.invest.rebalance.pricing.QuoteRequestEstimator;
import io.github.steliospaps.experimental.investment.invest.rebalance.pricing.QuotesPricer;
import io.github.steliospaps.experimental.investment.invest.rebalance.result.Allocation;
import io.github.steliospaps.experimental.investment.invest.rebalance.result.RebalanceResult;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.ControlAccount;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.Fund;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.RebalanceConfig;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.RebalanceState;
import io.vavr.Tuple;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.control.Either;

public class RebalancerIteration {

	/**
	 * math context were we try to get the least amount of money either buy or sell
	 */
	private static final MathContext MONEY_DOWN = new MathContext(2, RoundingMode.DOWN);

	/**
	 * math context were we try to get the least amount of quantity either buy or
	 * sell
	 */
	private static final MathContext QUANTITY_DOWN = new MathContext(7, RoundingMode.DOWN);

	private List<Fund> fundsRequiringActions;

	private QuotesPricer quotesPricer;

	private MarketDataPricer marketDataPricer;

	private Map<String, Either<List<RebalanceAction>, BigDecimal>> estimatedAgregateDelta;

	private ControlAccount controlAccount;

	private TradeRequestMaker tradeRequestMaker;

	public RebalancerIteration(RebalanceState current) {
		fundsRequiringActions = current.getFunds().filter(f -> fundNeedsAction(f, current.getConfig()));
		marketDataPricer = new MarketDataPricer(current.getMarketPrices());
		estimatedAgregateDelta = estimateAggregateDelta(marketDataPricer);
		QuoteRequestEstimator quoteRequestEstimator = new QuoteRequestEstimator(estimatedAgregateDelta,
				current.getConfig().getOverSizeQuoteRatio());
		quotesPricer = new QuotesPricer(current.getQuotes(), quoteRequestEstimator);
		controlAccount = current.getControlAccount();

		tradeRequestMaker = new TradeRequestMaker(current.getQuotes(), quoteRequestEstimator);
	}

	public Either<List<RebalanceAction>, RebalanceResult> rebalance() {

		if (fundsRequiringActions.isEmpty()) {
			return Either.right(RebalanceResult.NOTHING);
		} else {
			return allocate();
		}

	}

	private Either<List<RebalanceAction>, RebalanceResult> allocate() {
		// TODO: handle buy and sell sides
		var eitherAllocations = fundsRequiringActions//
				.map(f -> Tuple.of(f, getQuantityDelta(f, quotesPricer))).flatMap(i -> i._2.toStream()// i: (Fund, map)
						.map(j -> Tuple.of(i._1, j._1, j._2)))// (Fund, instrument, either)
				.map(tup -> tup._3.map(val -> Tuple.of(tup._1, tup._2, val)))// Either<?,(fund,instrument,quantity)>
				.map(either -> either.flatMap(tup -> quotesPricer.getAsk(tup._2)//
						.map(price -> Tuple.of(tup._1, tup._2, tup._3, price))));

		// TODO check we have enough trades

		return Either.sequence(eitherAllocations)//
				.mapLeft(actions -> RebalanceActionUtil.combineActions(actions))//
				// right = List<(fund, instrument, quantity, price)>
				.map(allocations2 -> allocations2.map(a -> Allocation.builder()//
						.clientAccount(a._1.getAccountId())//
						.instrumentId(a._2)//
						.quantityDelta(a._3)//
						.price(a._4)//
						.build()))//
				.flatMap(list -> requestTradesIfNotEnoughQuantity(list))//
				.<RebalanceResult>map(
						allocations2 -> RebalanceResult.builder().allocations(allocations2.toList()).build());

	}

	private Either<List<RebalanceAction>, Seq<Allocation>> requestTradesIfNotEnoughQuantity(
			Seq<Allocation> allocations) {
		HashMap<String, BigDecimal> availableByInstrument = controlAccount.getStock()
				.map(s -> Tuple.of(s.getInstrumentId(), s.getQuantity())).collect(HashMap.collector());

		return Either.sequence(allocations.groupBy(a -> a.getInstrumentId())//
				.mapValues(l -> l//
						.map(Allocation::getQuantityDelta)//
						.reduce((a, b) -> a.add(b)))//
				.toStream()//
				.map(tup -> availableByInstrument.get(tup._1)//
						.map(tradeQuantity -> canAllocate(tradeQuantity, tup._2) ? Either.right(true)// ignored
								: Either.left(tradeRequestMaker.makeRequest(tup._1, tup._2))// if we cannot allocate
										.mapLeft(RebalanceAction
												.addNarrative(":trade ("+tradeQuantity
														+") not big enough for allocation ("+tup._2+")")))
						.toEither(tradeRequestMaker.makeRequest(tup._1, tup._2)
								.map(a -> a.withNarrative(":could not find trade during allocation ")))// either if we
																										// cannot find
																										// trade
				))//
				.mapLeft(RebalanceActionUtil::combineActions)//
				.map(ignored -> allocations);// if still right we proceed with the allocations
	}

	private boolean canAllocate(BigDecimal tradeQuantity, BigDecimal allocationQuantity) {
		if (isBuy(allocationQuantity)) {
			return allocationQuantity.compareTo(tradeQuantity) <= 0;
		} else {
			return allocationQuantity.compareTo(tradeQuantity) >= 0;
		}
	}

	public static boolean isBuy(BigDecimal allocationQuantity) {
		return allocationQuantity.compareTo(BigDecimal.ZERO) >= 0;
	}

	// Instrument->QuantityDiff
	private Map<String, Either<List<RebalanceAction>, BigDecimal>> estimateAggregateDelta(MarketDataPricer pricer) {

		return fundsRequiringActions.map(f -> getQuantityDelta(f, pricer))
				.reduceOption((f1QuantDelta, f2QuantDelta) -> f1QuantDelta.merge(f1QuantDelta,
						(instrumentQuant1, instrumentQuant2) -> RebalanceActionUtil.combine(instrumentQuant1,
								instrumentQuant2, (quant1, quant2) -> quant1.add(quant2))))//
				.getOrElse(() -> HashMap.empty());
	}

	private static Map<String, Either<List<RebalanceAction>, BigDecimal>> getQuantityDelta(Fund f, Pricer pricer) {
		// TODO: make it handle not just buys from 0
		Map<String, BigDecimal> quantityTargetRatiosByInstrument = f.getPortfolio().getItems()//
				.map(a -> Tuple.of(a.getInstrumentId(), a.getQuantityRatio()))//
				.collect(HashMap.collector());
		Map<String, Either<List<RebalanceAction>, BigDecimal>> valueByInstrument = quantityTargetRatiosByInstrument//
				.map((k, v) -> Tuple.of(k, pricer.getAsk(k).map(price -> price.multiply(v))));

		valueByInstrument = normalise(valueByInstrument, f.getAvailableToInvest(), MONEY_DOWN);

		Map<String, Either<List<RebalanceAction>, BigDecimal>> quantityByInstrument = valueByInstrument
				.map((k, v) -> Tuple.of(k, v.flatMap(vv -> pricer.getAsk(k).map(pr -> vv.divide(pr, QUANTITY_DOWN)))
						.mapLeft(RebalanceAction.addNarrative("for fund " + f.getAccountId()))));
		// TODO: this is target quantity not quantity delta
		return quantityByInstrument;
	}

	private static Map<String, Either<List<RebalanceAction>, BigDecimal>> normalise(
			Map<String, Either<List<RebalanceAction>, BigDecimal>> valueByInstrument, BigDecimal targetSum,
			MathContext mathContext) {

		Either<List<RebalanceAction>, BigDecimal> sum = valueByInstrument.values()
				.reduce((a, b) -> a.flatMap(aa -> b.map(bb -> bb.add(aa))));// TODO: propagate all the rebalance actions
		return valueByInstrument.map((k, v) -> Tuple.of(k,
				// use API.For()
				v.flatMap(vv -> sum.map(ss -> targetSum.multiply(vv).divide(ss, mathContext)))));
	}

	private boolean fundNeedsAction(Fund f, RebalanceConfig config) {
		return fundNeedsCachChange(f, config);
		// TODO add rebalance check
	}

	private boolean fundNeedsCachChange(Fund f, RebalanceConfig config) {
		// TODO: add some hysteresis here (from config? minInvestAmount minDivest?)
		return f.getAvailableToInvest().compareTo(config.getMaximumTolerableCash()) > 0;
	}

}
