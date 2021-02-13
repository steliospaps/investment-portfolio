package io.github.steliospaps.experimental.investment.invest.rebalance.actions;

import lombok.Value;

/**
 * narative for actions. it can transparently wrap actions in order to provide narative for their reason
 * @author stelios
 *
 */
@Value
public final class Narrative implements RebalanceAction{
	private RebalanceAction actualAction;
	private String narrative;
	private Narrative(RebalanceAction actualAction, String narrative) {
		this.actualAction=actualAction;
		this.narrative=narrative;
	}
	static RebalanceAction of(RebalanceAction action, String narration) {
		if(action instanceof Narrative) {
			Narrative other = (Narrative) action;
			return new Narrative(other.actualAction, other.narrative+" "+narration);
		} else {
			return new Narrative(action, narration);
		}
	}	
}
