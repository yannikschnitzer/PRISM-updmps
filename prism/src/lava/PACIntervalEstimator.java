package lava;

import common.Interval;
import explicit.Distribution;
import explicit.IMDP;
import explicit.IMDPSimple;
import explicit.MDP;
import param.Function;
import prism.Evaluator;
import prism.Prism;

import java.util.*;

public class PACIntervalEstimator extends MAPEstimator {

	protected double error_tolerance;
	protected HashMap<TransitionTriple, Double> tiedModes = new HashMap<>();
	protected HashMap<TransitionTriple, Integer> tiedStateActionCounts = new HashMap<>();

    public PACIntervalEstimator(Prism prism, Experiment ex) {
		super(prism, ex);
		error_tolerance = ex.error_tolerance;
		this.name = "PAC";
    }

	/**
	 * Combine transition-triple and state-action pair counts for similar tranisitons, i.e., tie the parameters.
	 */
	public void tieParameters() {
		List<List<TransitionTriple>> similarTransitions = this.getSimilarTransitions();

		for (List<TransitionTriple> transitions : similarTransitions) {
			// Compute mode over all similar transitions
			int num = 0;
			int denum = 0;

			for (TransitionTriple t : transitions) {
				StateActionPair sa = t.getStateAction();
				num += samplesMap.getOrDefault(t, 0);
				denum += sampleSizeMap.getOrDefault(sa, 0);
			}

			for (TransitionTriple t : transitions) {
				double mode = (double) num / (double) denum;
				tiedModes.put(t, mode);
				tiedStateActionCounts.put(t, denum);
			}
		}

	}

	@Override
	protected Interval<Double> getTransitionInterval(TransitionTriple t) {
		double precision = 1e-8;
		if (!this.samplesMap.containsKey(t)){
			return new Interval<>(precision, 1-precision);
		}
		//double point = mode(t);
		double point = tiedModes.get(t);
//		System.out.println("Transition:" + t + " Old Mode: " + point + " Old Count:"
//				+ getStateActionCount(t.getStateAction()) + " New Mode: " + tiedModes.get(t) + " New Count:" + tiedStateActionCounts.get(t));
//		//System.out.println("Point = " + point);
		double confidence_interval = confidenceInterval(t);
		//System.out.println("confidence_interval = " + confidence_interval);
		double lower_bound = Math.max(point - confidence_interval, precision);
		double upper_bound = Math.min(point + confidence_interval, 1-precision);
		//System.out.println("confidence interval: " + confidence_interval);
		//System.out.println("[l, u]: [" + lower_bound +", "+  upper_bound+ "]")
		return new Interval<>(lower_bound, upper_bound);
	}


	/**
	 * Get minimum (width) interval for each transition.
	 * @return Map from transition to minimal interval
	 */
	public Map<TransitionTriple, Interval<Double>> computeMinIntervals() {
		Map<Function, List<TransitionTriple>> functionMap = this.getFunctionMap();
		Map<TransitionTriple, Interval<Double>> minIntervalMap = new HashMap<>();

		for (Function func : functionMap.keySet()){
			List<TransitionTriple> transitions = functionMap.get(func);
			List<Interval<Double>> intervals = new ArrayList<>();

			for (TransitionTriple transition : transitions){
				intervals.add(getTransitionInterval(transition));
			}

			Interval<Double> minInterval = Collections.min(intervals, Comparator.comparingDouble(interval -> interval.getUpper() - interval.getLower()));

			for (TransitionTriple transition : transitions){
				minIntervalMap.put(transition, minInterval);
			}
		}

		return minIntervalMap;
	}

	@Override
	public IMDP<Double> buildPointIMDP(MDP<Double> mdp) {
		System.out.println("Building IMDP");
		int numStates = mdp.getNumStates();
		IMDPSimple<Double> imdp = new IMDPSimple<>(numStates);
		imdp.addInitialState(mdp.getFirstInitialState());
		imdp.setStatesList(mdp.getStatesList());
		imdp.setConstantValues(mdp.getConstantValues());
		imdp.setIntervalEvaluator(Evaluator.forDoubleInterval());

		tieParameters();

		Map<TransitionTriple, Interval<Double>> minIntervals = computeMinIntervals();

		for (int s = 0; s < numStates; s++) {
			int numChoices = mdp.getNumChoices(s);
			final int state = s;
			for (int i = 0 ; i < numChoices; i++) {
				final String action = getActionString(mdp, s, i);

				Distribution<Interval<Double>> distrNew = new Distribution<>(Evaluator.forDoubleInterval());
				mdp.forEachDoubleTransition(s, i, (int sFrom, int sTo, double p)->{
					TransitionTriple t = new TransitionTriple(state, action, sTo);
					Interval<Double> interval;
					if (!this.ex.optimizations) {
						if (0 < p && p < 1.0) {
							interval = getTransitionInterval(t);
							//System.out.println("Transition:" + t + " Naive Interval: " + interval + " New Interval: " + minIntervals.get(t));
							//System.out.println("Triple: " + t + " Interval: " + interval);
							distrNew.add(sTo, interval);
							this.intervalsMap.put(t, interval);
						} else if (p == 1.0) {
							interval = new Interval<Double>(p, p);
							distrNew.add(sTo, interval);
							this.intervalsMap.put(t, interval);
						}
					} else {
						if (!this.constantMap.containsKey(t)) {
							interval = minIntervals.get(t);
						} else {
							p = this.constantMap.get(t);
							interval = new Interval<Double>(p, p);
						}
						if (p > 0) {
							distrNew.add(sTo, interval);
							this.intervalsMap.put(t, interval);
						}
					}
				});
				imdp.addActionLabelledChoice(s, distrNew, getActionString(mdp, s, i));
			}
		}
		Map<String, BitSet> labels = mdp.getLabelToStatesMap();
		Iterator<Map.Entry<String, BitSet>> it = labels.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, BitSet> entry = it.next();
			imdp.addLabel(entry.getKey(), entry.getValue());
		}
		this.estimate = imdp;

		return imdp;
	}

	@Override
	public double averageDistanceToSUL() {
		double totalDist = 0.0;

		for (TransitionTriple t : super.trueProbabilitiesMap.keySet()) {
			Interval<Double> interval = this.intervalsMap.get(t);
			double p = super.trueProbabilitiesMap.get(t);
			double dist = maxIntervalPointDistance(interval, p);
			totalDist += dist;
		}

		double averageDist = totalDist / super.trueProbabilitiesMap.keySet().size();
		return averageDist;

	}

	protected Double confidenceInterval(TransitionTriple t) {
		return computePACBound(t);
	}

	private Double computePACBound(TransitionTriple t) {
		double alpha = error_tolerance; // probability of error (i.e. 1-alpha is probability of correctly specifying the interval)
		int m = this.getNumLearnableTransitions();
		//System.out.println("m = " + m);
		//int n = getStateActionCount(t.getStateAction());
		int n = tiedStateActionCounts.get(t);
		alpha = (error_tolerance*(1.0/(double) m));///((double) this.mdp.getNumChoices(t.getStateAction().getState())); // distribute error over all transitions

		double delta = Math.sqrt((Math.log(2 / alpha))/(2*n));
		return delta;
	}
}
