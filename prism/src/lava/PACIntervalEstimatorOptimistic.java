package lava;

import prism.Prism;
import prism.PrismException;
import strat.Strategy;

public class PACIntervalEstimatorOptimistic extends PACIntervalEstimator {
    public PACIntervalEstimatorOptimistic(Prism prism, Experiment ex) {
        super(prism, ex);
    }

    public Strategy buildStrategy() throws PrismException {
        return this.buildWeightedOptimisticStrategy(this.getEstimate(), this.ex.strategyWeight);
    }
}

