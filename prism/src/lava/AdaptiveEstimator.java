package lava;

import explicit.MDP;
import prism.Prism;

import java.util.ArrayList;
import java.util.List;

public class AdaptiveEstimator {

    private List<Estimator> estimators = new ArrayList<>();
    private Estimator activeEstimate;
    private MDP<Double> mdp;
    private final Experiment ex;
    private final Prism prism;

    public AdaptiveEstimator(Estimator initialEstimator, Experiment ex, Prism prism) {
        this.estimators.add(initialEstimator);
        this.activeEstimate = initialEstimator;
        this.mdp = initialEstimator.getSUL();
        this.ex = ex;
        this.prism = prism;
    }

    public boolean checkTrajectoryAgreement() {
        return false;
    }

    public void addToBuffer() {
        ;
    }

    public boolean checkBufferAgreement() {
        return false;
    }

    public void constructNewEstimate() {
        EstimatorConstructor estimatorConstructor = BayesianEstimatorOptimistic::new;
        Estimator estimator = estimatorConstructor.get(this.prism, ex);
        estimator.set_experiment(ex);
        // TODO: feed new estimate with trajectories from buffer
    }

    public Estimator getActiveEstimate() {
        return activeEstimate;
    }



}
