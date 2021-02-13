package io.github.steliospaps.experimental.investment.invest.rebalance.actions;

import java.util.function.Function;

import io.vavr.collection.List;

/**
 * additional actions are required 
 * @author stelios
 *
 */
public interface RebalanceAction {
	default RebalanceAction withNarrative(String narrative) {
		return Narrative.of(this, narrative);
	}
	
	public static Function<List<RebalanceAction>,List<RebalanceAction>> addNarrative(String string) {
		return l -> l.map(action -> action.withNarrative(string));
	}
	
	default RebalanceAction stripNarrative() {
		if(this instanceof Narrative) {
			return ((Narrative)this).getActualAction();
		}
		return this;
	}

}
