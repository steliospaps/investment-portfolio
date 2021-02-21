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
import io.github.steliospaps.experimental.investment.invest.rebalance.state.FractionalAccount;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.Fund;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.RebalanceConfig;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.RebalanceState;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.control.Either;
import io.vavr.control.Option;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

	private @NonNull List<Fund> fundsRequiringActions;

	private @NonNull QuotesPricer quotesPricer;

	private @NonNull MarketDataPricer marketDataPricer;

	private @NonNull Map<String, Either<List<RebalanceAction>, BigDecimal>> estimatedAgregateDelta;

	private @NonNull ControlAccount controlAccount;

	private @NonNull TradeRequestMaker tradeRequestMaker;

	private @NonNull FractionalAccount fractionalAccount;

	public RebalancerIteration(RebalanceState current) {
		fundsRequiringActions = current.getFunds().filter(f -> fundNeedsAction(f, current.getConfig()));
		marketDataPricer = new MarketDataPricer(current.getMarketPrices());
		estimatedAgregateDelta = estimateAggregateDelta(marketDataPricer);
		QuoteRequestEstimator quoteRequestEstimator = new QuoteRequestEstimator(estimatedAgregateDelta,
				current.getConfig().getOverSizeQuoteRatio());
		quotesPricer = new QuotesPricer(current.getQuotes(), quoteRequestEstimator);
		controlAccount = current.getControlAccount();
		fractionalAccount = current.getFractionalAccount();

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
				.map(allocations2 -> addFractionalAcountTrades(allocations2)).<RebalanceResult>map(
						allocations2 -> RebalanceResult.builder().allocations(allocations2.toList()).build());

	}

	private Seq<Allocation> addFractionalAcountTrades(Seq<Allocation> allocations) {
		HashMap<String, BigDecimal> availableQuantityByInstrument = controlAccount.getStock()
				.map(s -> Tuple.of(s.getInstrumentId(), s.getQuantity())).collect(HashMap.collector());
		
		return allocations.groupBy(a -> a.getInstrumentId()).mapValues(l -> l//
				.map(Allocation::getQuantityDelta)//
				.reduce((a, b) -> a.add(b)))//
				.toStream()//
				.flatMap(tup -> {
					String instrumentId = tup._1;
					BigDecimal allocatedQty = tup._2;
					return availableQuantityByInstrument.get(instrumentId)//
							.orElse(()->Option.of(BigDecimal.ZERO))//zero if no trades (to be here we assume that the fractional has enough
							.map(controlAccountQty -> controlAccountQty.subtract(allocatedQty))//
							.filter(leftOver -> leftOver.compareTo(BigDecimal.ZERO)!=0)//
							.map(leftOver -> Allocation.builder()//
									.clientAccount(null)// fractional account
									.quantityDelta(leftOver)//
									.instrumentId(instrumentId)//
									.price(quotesPricer.getAsk(instrumentId)//
											.getOrElseThrow(() -> new RuntimeException(
													"there should be an ask price for instrument " + instrumentId)))//
									.build());
				}).appendAll(allocations);
	}

	private Either<List<RebalanceAction>, Seq<Allocation>> requestTradesIfNotEnoughQuantity(
			Seq<Allocation> allocations) {
		Map<String, BigDecimal> availableQuantityByInstrument = controlAccount.getStock()
				.map(s -> Tuple.of(s.getInstrumentId(), s.getQuantity()))
				.appendAll(fractionalAccount.getStock()//
						.map(s -> Tuple.of(s.getInstrumentId(), s.getQuantity())))
				.groupBy(t2 -> t2._1)//
				.mapValues(l -> l.map(Tuple2::_2).reduce((a,b)->a.add(b)));
		
		log.info("availableQuantityByInstrument={}",availableQuantityByInstrument);
		List<RebalanceAction> actions = allocations.groupBy(a -> a.getInstrumentId())//
				.mapValues(l -> l//
						.map(Allocation::getQuantityDelta)//
						.reduce((a, b) -> a.add(b)))//
				.toStream()//
				.map(tup -> requestTradesIfAllocationBiggerThanAvailableQty(availableQuantityByInstrument, tup._1, tup._2))//
				.reduce((a1,a2) -> RebalanceActionUtil.combineActions(a1,a2));
				
		if(actions.isEmpty()) {
			return Either.right(allocations);
		} else {
			return Either.left(actions);
		}
	}

	private List<RebalanceAction> requestTradesIfAllocationBiggerThanAvailableQty(Map<String, BigDecimal> availableQuantityByInstrument,
			String instrumentId, BigDecimal allocationQuantity) {
		return availableQuantityByInstrument.get(instrumentId)//
				.map(tradeQuantity -> canAllocate(tradeQuantity, allocationQuantity) ? List.<RebalanceAction>of()// 
						: tradeRequestMaker.makeRequest(instrumentId, allocationQuantity)// if we cannot allocate
								.map(r -> r.withNarrative(":trade (" + tradeQuantity
										+ ") not big enough for allocation (" + allocationQuantity + ")")))//
				.getOrElse(()-> tradeRequestMaker.makeRequest(instrumentId, allocationQuantity)
						.map(a -> a.withNarrative(":could not find trade during allocation ")));
	}

	private boolean canAllocate(BigDecimal tradeQuantity, BigDecimal allocationQuantity) {
		log.info("comparing tradeQuantity={} allocationQuantity={}", tradeQuantity, allocationQuantity);
		if (isBuy(allocationQuantity)) {
			
			boolean result = allocationQuantity.compareTo(tradeQuantity) <= 0;
			log.info("isBuy=true result={}",result);
			return result;
		} else {
			boolean result = allocationQuantity.compareTo(tradeQuantity) >= 0;
			log.info("isBuy=false result={}",result);
			return result;
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
