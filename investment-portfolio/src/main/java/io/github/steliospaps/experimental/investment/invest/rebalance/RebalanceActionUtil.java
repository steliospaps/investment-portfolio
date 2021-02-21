package io.github.steliospaps.experimental.investment.invest.rebalance;
import io.github.steliospaps.experimental.investment.invest.rebalance.actions.RebalanceAction;
import io.vavr.Function2;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Either;

public class RebalanceActionUtil {
	private RebalanceActionUtil() {
	}
	public static List<RebalanceAction> combineActions(List<RebalanceAction> a1, List<RebalanceAction>  a2){
		return combineActions(List.of(a1,a2));
	}
	
	public static List<RebalanceAction> combineActions(Seq<List<RebalanceAction>> actions){
		return actions.flatMap(i -> i.toStream()).distinct().toList();
	}
	public static <T,U>  Either<List<RebalanceAction>,U> combine(Either<List<RebalanceAction>,T> a, 
			Either<List<RebalanceAction>,T> b, Function2<T,T,U> func){
		return a.<U>flatMap(aa -> b.map(bb-> func.apply(aa, bb))
				.<List<RebalanceAction>>mapLeft(bLeft -> combineActions(List.of(bLeft, a.swap().getOrElse(List.of())))));
	}
}
