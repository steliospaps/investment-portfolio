package io.github.steliospaps.experimental.investment.invest.rebalance;

import static io.github.steliospaps.experimental.investment.invest.rebalance.Rounding.roundMoney;
import static io.github.steliospaps.experimental.investment.invest.rebalance.Rounding.roundQuantity;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.math.BigDecimal;
import java.math.MathContext;
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

	private static final MathContext HIGH_PRECISION = new MathContext(100);

	private @NonNull List<Fund> fundsRequiringActions;

	private @NonNull QuotesPricer quotesPricer;

	private @NonNull MarketDataPricer marketDataPricer;

	private @NonNull Map<String, Either<List<RebalanceAction>, BigDecimal>> estimatedAgregateDelta;

	private @NonNull ControlAccount controlAccount;

	private @NonNull TradeRequestMaker tradeRequestMaker;

	private @NonNull FractionalAccount fractionalAccount;

	private Map<String, BigDecimal> tradedQuantityByInstrument;

	private Map<String, BigDecimal> fractionalQuantityByInstrument;

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
		
		tradedQuantityByInstrument = controlAccount.getHoldings()
				.map(s -> Tuple.of(s.getInstrumentId(), s.getQuantity()))
				.collect(HashMap.collector());
		
		fractionalQuantityByInstrument = fractionalAccount.getHoldings()//
						.map(s -> Tuple.of(s.getInstrumentId(), s.getQuantity()))
						.collect(HashMap.collector());


	}

	public Either<List<RebalanceAction>, RebalanceResult> rebalance() {

		if (fundsRequiringActions.isEmpty()) {
			return Either.right(RebalanceResult.NOTHING);
		} else {
			return allocate();
		}

	}

	private Either<List<RebalanceAction>, RebalanceResult> allocate() {
		var eitherAllocations = fundsRequiringActions//
				.map(f -> Tuple.of(f, getQuantityDelta(f, quotesPricer)
						.mapValues( v -> v.mapLeft(RebalanceAction.addNarrative("(allocating)")))//
						)).flatMap(i -> i._2.toStream()// i: (Fund, map)
						.map(j -> Tuple.of(i._1, j._1, j._2)))// (Fund, instrument, either)
				.map(tup -> tup._3.map(val -> Tuple.of(tup._1, tup._2, val)))// Either<?,(fund,instrument,quantity)>
				.map(either -> either.flatMap(tup -> getPrice(quotesPricer,tup._2,tup._3)//
						.map(price -> Tuple.of(tup._1, tup._2, tup._3, price))));

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
				.map(allocations2 -> addFractionalAccountTrades(allocations2)).<RebalanceResult>map(
						allocations2 -> RebalanceResult.builder().allocations(allocations2.toList()).build());

	}

	private Seq<Allocation> addFractionalAccountTrades(Seq<Allocation> allocations) {
		
		return allocations.groupBy(a -> a.getInstrumentId()).mapValues(l -> l//
				.map(Allocation::getQuantityDelta)//
				.reduce((a, b) -> a.add(b)))//
				.toStream()//
				.flatMap(tup -> {
					String instrumentId = tup._1;
					BigDecimal allocatedQty = tup._2;
					return tradedQuantityByInstrument.get(instrumentId)//
							.orElse(()->Option.of(BigDecimal.ZERO))//zero if no trades (to be here we assume that the fractional has enough
							
							.filter(tradedQty -> tradedQty.compareTo(allocatedQty)!=0)//
							.map(tradedQty -> Allocation.builder()//
									.clientAccount(null)// fractional account
									.quantityDelta(tradedQty.subtract(allocatedQty))//
									.instrumentId(instrumentId)//
									.price(getPrice(quotesPricer,instrumentId,isFractionalTradeBuy(allocatedQty,
											tradedQty, fractionalQuantityByInstrument.get(instrumentId)//
											.getOrElse(BigDecimal.ZERO)))//
											.getOrElseThrow(() -> new RuntimeException(
													"there should be an ask price for instrument " + instrumentId)))//
									.build());
				}).appendAll(allocations);
	}

	/**
	 * 
	 * @param allocatedQty how much is allocated to the clients
	 * @param tradedQty how much was traded by the clients
	 * @param fractionalQty the fractional acocunt holdings
	 * @return true if the fractional account should be priced at the buy price or the ask price.
	 */
	private boolean isFractionalTradeBuy(BigDecimal allocatedQty, BigDecimal tradedQty, BigDecimal fractionalQty) {
		if(isBuy(allocatedQty)) {
			return true;
		} else {
			return false;
		}
	}

	private Either<List<RebalanceAction>, Seq<Allocation>> requestTradesIfNotEnoughQuantity(
			Seq<Allocation> allocations) {
				
		
		log.info("availableQuantityByInstrument={}",tradedQuantityByInstrument);
		List<RebalanceAction> actions = allocations.groupBy(a -> a.getInstrumentId())//
				.mapValues(l -> l//
						.map(Allocation::getQuantityDelta)//
						.reduce((a, b) -> a.add(b)))//
				.toStream()//
				.map(tup -> requestTradesIfAllocationBiggerThanAvailableQty(tradedQuantityByInstrument, fractionalQuantityByInstrument, tup._1, tup._2))//
				.reduce((a1,a2) -> RebalanceActionUtil.combineActions(a1,a2));
				
		if(actions.isEmpty()) {
			return Either.right(allocations);
		} else {
			return Either.left(actions);
		}
	}

	private List<RebalanceAction> requestTradesIfAllocationBiggerThanAvailableQty(Map<String, BigDecimal> availableQuantityByInstrument,
			Map<String, BigDecimal> fractionalQuantityByInstrument, String instrumentId, BigDecimal allocationQuantity) {
		BigDecimal fractionalAvailableQuantity = fractionalQuantityByInstrument//
				.get(instrumentId)//
				.getOrElse(BigDecimal.ZERO);
		BigDecimal tradeQuantity = availableQuantityByInstrument.get(instrumentId)//
				.getOrElse(BigDecimal.ZERO);
		return canAllocate(tradeQuantity, 
								fractionalAvailableQuantity,
								allocationQuantity) ? List.<RebalanceAction>of()// 
							: tradeRequestMaker.makeRequest(instrumentId, allocationQuantity, fractionalAvailableQuantity)// if we cannot allocate
									.map(r -> r.withNarrative(":trade (" + tradeQuantity
											+ ") and fractional ("+fractionalAvailableQuantity
											+") not big enough for allocation (" + allocationQuantity + ")"));
	}

	private boolean canAllocate(BigDecimal tradeQuantity, BigDecimal fractionalAvailableQuantity, BigDecimal allocationQuantity) {
		log.info("comparing tradeQuantity={} allocationQuantity={} fractionalAvailableQuantity={}", tradeQuantity, allocationQuantity, fractionalAvailableQuantity);
		if (isBuy(allocationQuantity)) {
			
			boolean result = allocationQuantity.compareTo(tradeQuantity.add(fractionalAvailableQuantity)) <= 0;
			log.info("isBuy=true result={}",result);
			return result;
		} else {
			BigDecimal leftOver = allocationQuantity.subtract(tradeQuantity);
			if(leftOver.compareTo(BigDecimal.ZERO)>=0) {
				//if we allocate -6 and trade is -6 or -7 we are here
				log.info("isBuy=false result=true because trade is abs bigger");
				return true;
			} else {
				//now we have to use the fractional
				//e.g. alloc -6.3 trade is -6 leftover=-0.3 
				if(leftOver.compareTo(fractionalAvailableQuantity.subtract(BigDecimal.ONE))>0) {
					//if fractional=0.6 then leftover(-0.3) > (0.6 -1 = -0.4)
					//we can allocate
					log.info("isBuy=false result=true because fractional {} can take it");
					return true;
				} else {
					log.info("isBuy=false result=false because fractional {} cannot take it");
					return false;					
				}
			}
		}
	}

	public static boolean isBuy(BigDecimal allocationQuantity) {
		return allocationQuantity.compareTo(BigDecimal.ZERO) >= 0;
	}

	// Instrument->QuantityDiff
	private Map<String, Either<List<RebalanceAction>, BigDecimal>> estimateAggregateDelta(MarketDataPricer pricer) {

		return fundsRequiringActions.map(f -> getQuantityDelta(f, pricer)//
				.mapValues( v -> v.mapLeft(RebalanceAction.addNarrative("(estimating delta)"))))//
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
				.map((k, v) -> Tuple.of(k, 
						getPrice(pricer, k, isFundBuyingInstrument(k,f))
							.map(price -> price.multiply(v))));
		log.info("valueDeltaByInstrument={}",valueByInstrument);
		valueByInstrument = normalise(valueByInstrument, f.getAvailableToInvest());

		log.info("normalised valueDeltaByInstrument={}",valueByInstrument);
		
		Map<String, Either<List<RebalanceAction>, BigDecimal>> quantityByInstrument = valueByInstrument
				.map((k, v) -> Tuple.of(k, v.flatMap(vv -> getPrice(pricer, k, vv)
						.map(pr -> roundQuantity(vv.divide(pr, HIGH_PRECISION))))
						.mapLeft(RebalanceAction.addNarrative("fund=" + f.getAccountId()))));
		// TODO: this is target quantity not quantity delta
		return quantityByInstrument;
	}


	private static boolean isFundBuyingInstrument(String instrumentId, Fund fund) {
		return isBuy(fund.getAvailableToInvest());
	}

	private static Either<List<RebalanceAction>, BigDecimal> getPrice(Pricer pricer, String instrumentId,
			BigDecimal valueOrQuantityDelta) {
		return getPrice(pricer, instrumentId, isBuy(valueOrQuantityDelta));
	}
	
	private static Either<List<RebalanceAction>, BigDecimal> getPrice(Pricer pricer, String instrumentId,
			boolean useBuyPrice) {
		return Match(useBuyPrice).of(
				Case($(true), pricer.getAsk(instrumentId)),
				Case($(false), pricer.getBid(instrumentId)));
	}

	private static Map<String, Either<List<RebalanceAction>, BigDecimal>> normalise(
			Map<String, Either<List<RebalanceAction>, BigDecimal>> valueByInstrument, BigDecimal targetSum
			) {
		
		Either<List<RebalanceAction>, BigDecimal> sum = valueByInstrument.values()
				.reduce((a, b) -> a.flatMap(aa -> b.map(bb -> bb.add(aa))));// TODO: propagate all the rebalance actions
		return valueByInstrument.map((k, v) -> Tuple.of(k,
				v.flatMap(vv -> sum.map(ss -> roundMoney(targetSum.multiply(vv).divide(ss, HIGH_PRECISION))))));
		//TODO: revisit this so that we always allocate less or equal to the money (both for buy and sell) 
	}


	private boolean fundNeedsAction(Fund f, RebalanceConfig config) {
		return fundNeedsCachChange(f, config);
		// TODO add rebalance check
	}

	private boolean fundNeedsCachChange(Fund f, RebalanceConfig config) {
		// TODO: add some hysteresis here (from config? minInvestAmount minDivest?)
		return f.getAvailableToInvest().compareTo(config.getMaximumTolerableCash()) > 0
				|| f.getAvailableToInvest().compareTo(BigDecimal.ZERO)<0;
	}

}
