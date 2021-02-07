package io.github.steliospaps.experimental.investment.invest.algo;

import static java.math.BigDecimal.valueOf;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.steliospaps.experimental.investment.invest.rebalance.RebalancerImpl;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.FractionalAccount;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.Fund;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.MarketPrice;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.Portfolio;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.PortfolioItem;
import io.github.steliospaps.experimental.investment.invest.rebalance.state.RebalanceState;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;

class RebalancerImplTest {
	private static final String INSTRUMENT1 = "instrument1";
	private static final String INSTRUMENT2 = "instrument2";
	
	@Nested
	static class AggregateDelta{
		
		@Test
		void testOneFundOneInstrument() {
			Portfolio porfolio1 = Portfolio.of(List.of(
					PortfolioItem.of(INSTRUMENT1, valueOf(1.0))));
			RebalanceState state = RebalanceState.builder()
					.funds(List.of(Fund.builder()//
							.portfolio(porfolio1)//
							.availableToInvest(valueOf(100))//
							.build()))//
					.marketPrices(List.of(MarketPrice.of(INSTRUMENT1, valueOf(19), valueOf(20))))//
					.fractionalAccount(FractionalAccount.builder().build())//
					.build();
			RebalancerImpl.AggregateDelta result = RebalancerImpl.estimateAggregateDelta(state);
			assertEquals(HashMap.of(INSTRUMENT1,valueOf(5)),result.getDeltaByInstrument());
		}
		
		@Test
		void testOneFundTwoInstruments() {
			Portfolio porfolio1 = Portfolio.of(List.of(
					PortfolioItem.of(INSTRUMENT1, valueOf(0.6)),
					PortfolioItem.of(INSTRUMENT2, valueOf(0.4))
					));
			RebalanceState state = RebalanceState.builder()
					.funds(List.of(Fund.builder()//
							.portfolio(porfolio1)//
							.availableToInvest(valueOf(100))//
							.build()))//
					.marketPrices(List.of(
							MarketPrice.of(INSTRUMENT1, valueOf(19), valueOf(20)),
							MarketPrice.of(INSTRUMENT2, valueOf(9), valueOf(10))
							))//
					.fractionalAccount(FractionalAccount.builder().build())//
					.build();
			RebalancerImpl.AggregateDelta result = RebalancerImpl.estimateAggregateDelta(state);
			assertEquals(HashMap.of(
					INSTRUMENT1,valueOf(3.75),
					INSTRUMENT2,valueOf(2.5)
					),result.getDeltaByInstrument());
		}
		
		//TODO: test for net sell, and crossing
	}

}
