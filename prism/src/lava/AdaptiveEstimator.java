package lava;

import explicit.MDP;
import prism.Prism;
import prism.PrismException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class AdaptiveEstimator {

    private final List<Estimator> estimators = new ArrayList<>();
    private Estimator activeEstimate;
    private MDP<Double> mdp;
    private final Experiment ex;
    private final Prism prism;

    private final HashMap<TransitionTriple, Integer> samplesMap = new HashMap<>();
    private final HashMap<StateActionPair, Integer> sampleSizeMap = new HashMap<>();
    private int trajectoryCounter = 0;

    public AdaptiveEstimator(Estimator initialEstimator, Experiment ex, Prism prism) {
        this.estimators.add(initialEstimator);
        this.activeEstimate = initialEstimator;
        this.mdp = initialEstimator.getSUL();
        this.ex = ex;
        this.prism = prism;
    }

    public void addTrajectory(HashMap<TransitionTriple, Integer> samplesMap, HashMap<StateActionPair, Integer> sampleSizeMap) {
        for (StateActionPair pair : sampleSizeMap.keySet()) {
            if (this.sampleSizeMap.containsKey(pair)) {
                this.sampleSizeMap.put(pair, this.sampleSizeMap.get(pair) + sampleSizeMap.get(pair));
            } else {
                this.sampleSizeMap.put(pair, sampleSizeMap.get(pair));
            }
        }

        for (TransitionTriple trip : samplesMap.keySet()) {
            if (this.samplesMap.containsKey(trip)) {
                this.samplesMap.put(trip, this.samplesMap.get(trip) + samplesMap.get(trip));
            } else {
                this.samplesMap.put(trip, samplesMap.get(trip));
            }
        }

        this.trajectoryCounter++;
    }

    public boolean checkTrajectoryAgreement() {
        return false;
    }

    public void addToBuffer() {

    }

    public boolean checkBufferAgreement() {
        // TODO: this is implementation detail, decide what exactly to be the condition
        double bufferAcceptanceRatio = 0.2;

        try {
            activeEstimate.setObservationMaps(this.samplesMap, this.sampleSizeMap);
            double[] conflictsCounts = activeEstimate.getConflicts(0.2);
            System.out.println("Conflicts Counts:" + Arrays.toString(conflictsCounts));
            if (conflictsCounts[0] / conflictsCounts[1] <= bufferAcceptanceRatio) {
                System.out.println("Buffer accepted");
                activeEstimate.getCurrentResults();
                clearObservationMaps();
            } else {
                //TODO: add check with other models in buffer
                System.out.println("Buffer rejected");
                System.out.println("Switching to fresh estimate");
                Estimator freshEstimate = getFreshEstimate(BayesianEstimatorOptimistic::new);
                this.estimators.add(freshEstimate);
                this.activeEstimate = freshEstimate;
                activeEstimate.setObservationMaps(this.samplesMap, this.sampleSizeMap);
                activeEstimate.getCurrentResults();
            }

        } catch (PrismException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    private Estimator getFreshEstimate(EstimatorConstructor estimatorConstructor) {
        Estimator estimator = estimatorConstructor.get(this.prism, ex);
        estimator.setFunctionMap(activeEstimate.getFunctionMap());
        estimator.setSimilarTransitions(activeEstimate.getSimilarTransitions());
        estimator.set_experiment(ex);

        return estimator;
    }

    public void clearObservationMaps() {
        this.sampleSizeMap.clear();
        this.samplesMap.clear();
        this.trajectoryCounter = 0;
    }

    public Estimator getActiveEstimate() {
        return activeEstimate;
    }

    public List<Estimator> getEstimators() {
        return estimators;
    }
}
