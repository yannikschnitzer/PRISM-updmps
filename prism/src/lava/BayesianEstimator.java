package lava;

import common.Interval;
import explicit.*;
import explicit.rewards.MDPRewardsSimple;
import param.Function;
import parser.ast.Expression;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import prism.*;
import simulator.ModulesFileModelGenerator;
import strat.MDStrategy;
import strat.MRStrategy;
import strat.Strategy;

import java.util.*;
import java.util.Map.Entry;


public class BayesianEstimator extends Estimator {

    public boolean DEBUG = false;

    //private IMDP<Double> estimate;
    public double validityScalingFactor = 1.0 + 1e-4;
    public double validityPrecision = 1e-8;
    public int lowerStrengthBound = Integer.MAX_VALUE;
    public int upperStrengthBound = Integer.MAX_VALUE;
    protected HashMap<TransitionTriple, Integer> tiedTripleCounts = new HashMap<>();
    protected HashMap<StateActionPair, Integer> tiedStateActionCounts = new HashMap<>();
    private PrismLog mainLog;
    private final HashMap<TransitionTriple, Double> trueProbabilitiesMap;
    private HashMap<TransitionTriple, Interval<Integer>> strengthMap;
    private ArrayList<TransitionTriple> observations;
    private ArrayList<TransitionTriple> unusedObservations;
    private int conflicts = 0;
    private final long totalTime = 0;
    private final long iterTime = 0;

    public BayesianEstimator(Prism prism, Experiment ex) {
        super(prism, ex);

        this.trueProbabilitiesMap = new HashMap<>();
        this.strengthMap = new HashMap<>();

        this.buildNewIMDP();
        this.initializeStrengthMap(this.ex.initLowerStrength, this.ex.initUpperStrength);

        this.lowerStrengthBound = ex.lowerStrengthBound;
        this.upperStrengthBound = ex.upperStrengthBound;
        this.name = "LUI";
    }


    public Result iterateDTMC(boolean verbose) throws PrismException {
        //updateIntervals();
        updateIntervalsSAConflict();
        //HashMap<TransitionTriple, Interval<Double>> oldIntervals = checkValidity(validityScalingFactor, validityPrecision);
        updateIMDP();
        MDStrategy strat = computeStrategyFromEstimate(estimate);
        Result resultDTMC = checkDTMC(strat);
        //resetIntervals(oldIntervals);
        return resultDTMC;
    }


    public Result iterateIMDP() throws PrismException {
        //updateIntervals();
        updateIntervalsSAConflict();
        //HashMap<TransitionTriple, Interval<Double>> oldIntervals = checkValidity(validityScalingFactor, validityPrecision);
        updateIMDP();
        Result resultIMDP = modelCheckIMDP(true);
        //resetIntervals(oldIntervals);
        return resultIMDP;
    }

    public double[] iterateIMDPandDTMC() throws PrismException {
        //updateIntervals();

        updateIntervalsSAConflict();
        //HashMap<TransitionTriple, Interval<Double>> oldIntervals = checkValidity(validityScalingFactor, validityPrecision);
        updateIMDP();
        double resultRobustIMDP = round((Double) modelCheckIMDP(true).getResult());
        double resultOptimisticIMDP = round((Double) modelCheckIMDP(false).getResult());
        MDStrategy robustStrat = computeStrategyFromEstimate(estimate, true);
        MDStrategy optimisticStrat = computeStrategyFromEstimate(estimate, false);
        double resultRobustDTMC = round((Double) checkDTMC(robustStrat).getResult());
        double resultOptimisticDTMC = round((Double) checkDTMC(optimisticStrat).getResult());
        double dist = round(this.averageDistanceToSUL());
        List<Double> lbs = List.of(0.0);//this.getLowerBounds();
        List<Double> ubs = List.of(1.0);//this.getUpperBounds();
        //resetIntervals(oldIntervals);
        return new double[]{resultRobustIMDP, resultRobustDTMC, dist, lbs.get(0), ubs.get(0), resultOptimisticIMDP, resultOptimisticDTMC};
    }


    public double[] getInitialResults() throws PrismException {
        double resultRobustIMDP = round((Double) modelCheckIMDP(true).getResult());
        double resultOptimisticIMDP = round((Double) modelCheckIMDP(false).getResult());
        MDStrategy robustStrat = computeStrategyFromEstimate(estimate, true);
        MDStrategy optimisticStrat = computeStrategyFromEstimate(estimate, false);
        double resultRobustDTMC = round((Double) checkDTMC(robustStrat).getResult());
        double resultOptimisticDTMC = round((Double) checkDTMC(optimisticStrat).getResult());
        double dist = round(this.averageDistanceToSUL());
        List<Double> lbs = List.of(0.0);//this.getLowerBounds();
        List<Double> ubs = List.of(1.0);//this.getUpperBounds();
        return new double[]{resultRobustIMDP, resultRobustDTMC, dist, lbs.get(0), ubs.get(0), resultOptimisticIMDP, resultOptimisticDTMC};
    }


    public double[] getCurrentResults() throws PrismException {
        return this.iterateIMDPandDTMC();
    }


    public void debugIntervalCheck() {
        for (StateActionPair sa : this.sampleSizeMap.keySet()) {
            ArrayList<TransitionTriple> transitions = new ArrayList<>();
            double sum_lb = 0.0;
            double sum_ub = 0.0;
            for (int successor = 0; successor < this.mdp.getNumStates(); successor++) {
                TransitionTriple t = new TransitionTriple(sa.getState(), sa.getAction(), successor);

                if (this.intervalsMap.containsKey(t)) {
                    Interval i = this.intervalsMap.get(t);
                    double lower = (Double) i.getLower();
                    double upper = (Double) i.getUpper();
                    sum_lb += lower;
                    sum_ub += upper;
                    if (lower <= 0.0) {
                        System.out.println("Lower bound <= 0.0 at  " + t + "  :  " + i);
                    }
                    if (upper > 1.0) {
                        System.out.println("Upper bound >= 1.0 at  " + t + "  :  " + i);
                    }
                }
            }
            if (sum_ub < 1.0) {
                System.out.printf("sum upper bound < 1.0 at %s: %f %n", sa.toString(), sum_ub);
            }
            if (sum_lb > 1.0) {
                System.out.printf("sum lower bound > 1.0 at %s: %f %n", sa, sum_lb);
            }
        }
    }

    public void debugPrintIntervalSums() {
        for (StateActionPair sa : this.sampleSizeMap.keySet()) {
            String out = "\nIntervals for state-action pair " + sa.toString() + "\n";
            double lowerboundSum = 0.0;
            double upperboundSum = 0.0;
            ArrayList<TransitionTriple> transitions = new ArrayList<>();
            for (int successor = 0; successor < this.mdp.getNumStates(); successor++) {
                TransitionTriple t = new TransitionTriple(sa.getState(), sa.getAction(), successor);
                if (this.intervalsMap.containsKey(t)) {
                    Interval i = this.intervalsMap.get(t);
                    lowerboundSum += (Double) i.getLower();
                    upperboundSum += (Double) i.getUpper();
                    out += t + "  :  " + this.intervalsMap.get(t).toString() + "\n";

                }
            }
            if (lowerboundSum >= 1.0 - 1e-4 || upperboundSum <= 1.0 + 1e-4) {
                out += "Lowerbound sum = " + lowerboundSum + "\nUpperbound sum = " + upperboundSum + "\n";
                System.out.println(out);
            }
        }

    }

    public void debugPrintIntervalsMap() {
        System.out.println("\n\n\nDEBUG printing the current intervalsMap\n\n");

        int numStates = this.mdp.getNumStates();
        for (int s = 0; s < numStates; s++) {
            int numChoices = this.mdp.getNumChoices(s);
            for (int i = 0; i < numChoices; i++) {
                StateActionPair sa = new StateActionPair(s, getActionString(this.mdp, s, i));
                System.out.println("Intervals for state-action pair " + sa);
                for (int successor = 0; successor < this.mdp.getNumStates(); successor++) {
                    TransitionTriple t = new TransitionTriple(sa.getState(), sa.getAction(), successor);
                    if (this.intervalsMap.containsKey(t)) {
                        System.out.println(t + "  :  " + this.intervalsMap.get(t).toString());
                    }
                }

                System.out.println("\n");
            }
        }


    }

    public void fixLowerbound(TransitionTriple t, double epsilon) {
        Interval i = this.intervalsMap.get(t);
        double upperbound = (Double) i.getUpper();
        double lowerbound = epsilon;
        Interval newI = new Interval(lowerbound, upperbound);
        this.intervalsMap.put(t, newI);
    }

    public void resetIntervals(HashMap<TransitionTriple, Interval<Double>> oldIntervals) {
        for (TransitionTriple t : oldIntervals.keySet()) {
            this.intervalsMap.put(t, oldIntervals.get(t));
        }
    }

    public HashMap<TransitionTriple, Interval<Double>> checkValidity(double factor, double precision) {
        HashMap<TransitionTriple, Interval<Double>> oldIntervals = new HashMap<>();
        String DEBUG_OUT = "";


        int numStates = this.mdp.getNumStates();
        for (int s = 0; s < numStates; s++) {
            int numChoices = this.mdp.getNumChoices(s);
            for (int i = 0; i < numChoices; i++) {
                StateActionPair sa = new StateActionPair(s, getActionString(this.mdp, s, i));

                String out = "\nIntervals for state-action pair " + sa + "\n";
                boolean skip = false;
                double lowerboundSum = 0.0;
                double upperboundSum = 0.0;
                ArrayList<TransitionTriple> transitions = new ArrayList<>();
                for (int successor = 0; successor < this.mdp.getNumStates(); successor++) {
                    TransitionTriple t = new TransitionTriple(sa.getState(), sa.getAction(), successor);
                    if (this.intervalsMap.containsKey(t)) {
                        if (this.trueProbabilitiesMap.get(t) == 1.0) {
                            skip = true;
                            break;
                        }
                        Interval interval = this.intervalsMap.get(t);
                        oldIntervals.put(t, interval);
                        lowerboundSum += (Double) interval.getLower();
                        upperboundSum += (Double) interval.getUpper();
                        transitions.add(t);
                        out += t + "  :  " + interval + "\n";
                    }
                }

                if (!skip) {
                    if (upperboundSum <= 1.0 + precision) {
                        double scale = ((1.0 + precision) / upperboundSum);
                        for (TransitionTriple t : transitions) {
                            Interval interval = this.intervalsMap.get(t);
                            double newUpperbound = ((Double) interval.getUpper()) * scale;
                            Interval newI = new Interval(interval.getLower(), newUpperbound);
                            this.intervalsMap.put(t, newI);
                        }
                    }

                    if (lowerboundSum >= 1.0 - precision) {
                        double scale = ((1.0 - precision) / lowerboundSum);
                        for (TransitionTriple t : transitions) {
                            Interval interval = this.intervalsMap.get(t);
                            double newLowerbound = ((Double) interval.getLower()) * scale;
                            Interval newI = new Interval(newLowerbound, interval.getUpper());
                            this.intervalsMap.put(t, newI);
                        }
                    }

                }
            }

        }
        return oldIntervals;
    }


    public HashMap<TransitionTriple, Interval<Double>> getIntervalsMap() {
        return this.intervalsMap;
    }

    public void setIntervalsMap(HashMap<TransitionTriple, Interval<Double>> map) {
        this.intervalsMap = map;
    }

    public void setStrengthMap(HashMap<TransitionTriple, Interval<Integer>> map) {
        this.strengthMap = map;
    }


    public Result modelCheckIMDP(boolean robust) throws PrismException {
        return modelCheckIMDP(robust, false);
    }


    public Result modelCheckIMDP(boolean robust, boolean verbose) throws PrismException {
        UMDPModelChecker mc = new UMDPModelChecker(this.prism);
        mc.setErrorOnNonConverge(false);
        mc.setGenStrat(true);
        PropertiesFile pf = prism.parsePropertiesString(ex.robustSpec);
        if (robust)
            pf = prism.parsePropertiesString(ex.robustSpec);
        else
            pf = prism.parsePropertiesString(ex.optimisticSpec);
        ModulesFileModelGenerator<?> modelGen = ModulesFileModelGenerator.create(modulesFileIMDP, this.prism);
        modelGen.setSomeUndefinedConstants(this.estimate.getConstantValues());
        mc.setModelCheckingInfo(modelGen, pf, modelGen);
        Result result = mc.check(this.estimate, pf.getProperty(0));
        if (verbose) {
            System.out.println("\nModel checking IMDP:");
            System.out.println(ex.robustSpec + " : " + result.getResultAndAccuracy());
        }
        return result;
    }

    public Result modelCheckPointEstimate(IMDP<Double> pointEstimate) throws PrismException {
        UMDPModelChecker mc = new UMDPModelChecker(this.prism);
        mc.setErrorOnNonConverge(false);
        PropertiesFile pf = prism.parsePropertiesString(ex.robustSpec);
        ModulesFileModelGenerator<?> modelGen = ModulesFileModelGenerator.create(modulesFileIMDP, this.prism);
        modelGen.setSomeUndefinedConstants(pointEstimate.getConstantValues());
        mc.setModelCheckingInfo(modelGen, pf, modelGen);
        Result result = mc.check(pointEstimate, pf.getProperty(0));
        System.out.println("\nModel checking point estimate MDP:");
        System.out.println(ex.robustSpec + " : " + result.getResultAndAccuracy());
        return result;
    }

/*	public MDStrategy computeStrategyFromEstimate(IMDP<Double> estimate) throws PrismException{
		return this.computeStrategyFromEstimate(estimate, true);
	}

	public MDStrategy computeOptimisticStrategyFromEstimate(IMDP<Double> estimate) throws PrismException{
		return this.computeStrategyFromEstimate(estimate, false);
	}

	public MDStrategy computeStrategyFromEstimate(IMDP<Double> estimate, boolean robust) throws PrismException {
		UMDPModelChecker mc = new UMDPModelChecker(this.prism);
		mc.setGenStrat(true);
		mc.setErrorOnNonConverge(false);
		PropertiesFile pf = robust
			? prism.parsePropertiesString(ex.robustSpec)
			: prism.parsePropertiesString(ex.optimisticSpec);
		ModulesFileModelGenerator<?> modelGen = ModulesFileModelGenerator.create(modulesFileIMDP, this.prism);
		modelGen.setSomeUndefinedConstants(estimate.getConstantValues());
		mc.setModelCheckingInfo(modelGen, pf, modelGen);
		Expression exprTarget = pf.getProperty(0);
		Result result = mc.check(estimate, exprTarget);
		MDStrategy strat = (MDStrategy) result.getStrategy();
		//System.out.println("Strategy = " + strat);    // strat is null
		return strat;
	}
*/


    public Result checkDTMC(MDStrategy strat) throws PrismException {
        MDPExplicit<Double> mdp = (MDPExplicit<Double>) this.prism.getBuiltModelExplicit();
        DTMC<Double> dtmc = (DTMC<Double>) mdp.constructInducedModel(strat);
        DTMCModelChecker mc = new DTMCModelChecker(this.prism);
        mc.setErrorOnNonConverge(false);
        mc.setGenStrat(true);
        PropertiesFile pf = prism.parsePropertiesString(ex.dtmcSpec);
        ModulesFile modulesFileDTMC = (ModulesFile) modulesFileIMDP.deepCopy();
        modulesFileDTMC.setModelType(ModelType.DTMC);
        ModulesFileModelGenerator<?> modelGen = ModulesFileModelGenerator.create(modulesFileDTMC, this.prism);
        modelGen.setSomeUndefinedConstants(mdp.getConstantValues());
        RewardGeneratorMDStrat<?> rewGen = new RewardGeneratorMDStrat(modelGen, mdp, strat);
        mc.setModelCheckingInfo(modelGen, pf, rewGen);
        Result result = mc.check(dtmc, pf.getProperty(0));
        return result;
    }

    public void buildNewIMDP() {
        switch (ex.initialInterval) {
            case WIDE:
                this.buildWideIMDP(this.ex.initGraphEpsilon);
                break;
            case UNIFORM:
                this.buildUniformIMDP(this.ex.initGraphEpsilon);
                break;
        }
    }

    /**
     * Create IMDP from MDP where each transition 0 < p < 1 in the MDP gets interval [epsilon, 1-epsilon] in the IMDP
     *
     * @param epsilon double
     */
    public void buildWideIMDP(double epsilon) {
        int numStates = this.mdp.getNumStates();
        IMDPSimple<Double> imdp = new IMDPSimple<>(numStates);
        imdp.addInitialState(this.mdp.getFirstInitialState());
        imdp.setStatesList(this.mdp.getStatesList());
        imdp.setConstantValues(this.mdp.getConstantValues());
        imdp.setIntervalEvaluator(Evaluator.forDoubleInterval());

        for (int s = 0; s < numStates; s++) {
            int numChoices = this.mdp.getNumChoices(s);
            final int state = s;
            for (int i = 0; i < numChoices; i++) {
                final String action = getActionString(this.mdp, s, i);
                Distribution<Interval<Double>> distrNew = new Distribution<>(Evaluator.forDoubleInterval());
                this.mdp.forEachDoubleTransition(s, i, (int sFrom, int sTo, double p) -> {
                    this.trueProbabilitiesMap.put(new TransitionTriple(state, action, sTo), p);
                    if (0 < p && p < 1.0) {
                        distrNew.add(sTo, new Interval<Double>(epsilon, 1 - epsilon));
                        this.intervalsMap.put(new TransitionTriple(state, action, sTo), new Interval<Double>(epsilon, 1 - epsilon));
                    } else {
                        distrNew.add(sTo, new Interval<Double>(p, p));
                        //System.out.println("Prob 1 transition found");
                        this.intervalsMap.put(new TransitionTriple(state, action, sTo), new Interval<Double>(p, p));
                    }
                });
                imdp.addActionLabelledChoice(s, distrNew, getActionString(this.mdp, s, i));
            }
        }
        Map<String, BitSet> labels = this.mdp.getLabelToStatesMap();
        Iterator<Entry<String, BitSet>> it = labels.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, BitSet> entry = it.next();
            imdp.addLabel(entry.getKey(), entry.getValue());
        }
        this.estimate = imdp;
    }

    /**
     * Create IMDP from MDP where each transition 0 < p < 1 in the MDP gets interval [1/A - epsilon, 1/A+epsilon] in the IMDP
     *
     * @param epsilon double
     */
    public void buildUniformIMDP(double epsilon) {
        int numStates = this.mdp.getNumStates();
        IMDPSimple<Double> imdp = new IMDPSimple<>(numStates);
        imdp.addInitialState(this.mdp.getFirstInitialState());
        imdp.setStatesList(this.mdp.getStatesList());
        imdp.setConstantValues(this.mdp.getConstantValues());
        imdp.setIntervalEvaluator(Evaluator.forDoubleInterval());

        for (int s = 0; s < numStates; s++) {
            int numChoices = this.mdp.getNumChoices(s);
            final int state = s;
            for (int i = 0; i < numChoices; i++) {
                final String action = getActionString(this.mdp, s, i);
                Distribution<Interval<Double>> distrNew = new Distribution<>(Evaluator.forDoubleInterval());
                this.mdp.forEachDoubleTransition(s, i, (int sFrom, int sTo, double p) -> {
                    TransitionTriple t = new TransitionTriple(state, action, sTo);
                    Interval<Double> interval;
                    this.trueProbabilitiesMap.put(t, p);
                    if (0 < p && p < 1.0) {
                        double lower_bound = Math.max(1. / numChoices - epsilon, 1e-8);
                        double upper_bound = Math.min(1. / numChoices + epsilon, 1 - 1e-8);
                        interval = new Interval<>(lower_bound, upper_bound);
                        distrNew.add(sTo, interval);
                        this.intervalsMap.put(t, interval);
                    } else {
                        interval = new Interval<>(p, p);
                        distrNew.add(sTo, interval);
                        //System.out.println("Prob 1 transition found");
                        this.intervalsMap.put(t, interval);
                    }
                });
                imdp.addActionLabelledChoice(s, distrNew, getActionString(this.mdp, s, i));
            }
        }
        Map<String, BitSet> labels = this.mdp.getLabelToStatesMap();
        Iterator<Entry<String, BitSet>> it = labels.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, BitSet> entry = it.next();
            imdp.addLabel(entry.getKey(), entry.getValue());
        }
        this.estimate = imdp;
    }


    public void initializeStrengthMap(int lower, int upper) {
        Interval<Integer> strengthInterval = new Interval(lower, upper);
        for (TransitionTriple t : this.intervalsMap.keySet()) {
            this.strengthMap.put(t, strengthInterval);
        }
    }

    public Map<TransitionTriple, Interval<Double>> computeMinIntervals() {
        Map<Function, List<TransitionTriple>> functionMap = this.getFunctionMap();
        Map<TransitionTriple, Interval<Double>> minIntervalMap = new HashMap<>();

        for (Function func : functionMap.keySet()) {
            List<TransitionTriple> transitions = functionMap.get(func);
            List<Interval<Double>> intervals = new ArrayList<>();

            for (TransitionTriple transition : transitions) {
                intervals.add(intervalsMap.get(transition));
            }

            Interval<Double> minInterval = Collections.min(intervals, Comparator.comparingDouble(interval -> interval.getUpper() - interval.getLower()));

            for (TransitionTriple transition : transitions) {
                minIntervalMap.put(transition, minInterval);
            }
        }

        return minIntervalMap;
    }

    public IMDP<Double> updateIMDP() {
        int numStates = mdp.getNumStates();
        IMDPSimple<Double> imdp = new IMDPSimple<>(numStates);
        imdp.addInitialState(mdp.getFirstInitialState());
        imdp.setStatesList(mdp.getStatesList());
        imdp.setConstantValues(mdp.getConstantValues());
        imdp.setIntervalEvaluator(Evaluator.forDoubleInterval());

        Map<TransitionTriple, Interval<Double>> minIntervals = computeMinIntervals();

        for (int s = 0; s < numStates; s++) {
            int numChoices = mdp.getNumChoices(s);
            final int state = s;
            for (int i = 0; i < numChoices; i++) {
                final String action = getActionString(mdp, s, i);
                Distribution<Interval<Double>> distrNew = new Distribution<>(Evaluator.forDoubleInterval());
                mdp.forEachDoubleTransition(s, i, (int sFrom, int sTo, double p) -> {
                    final TransitionTriple t = new TransitionTriple(state, action, sTo);
                    Interval<Double> interval;
                    if (!this.constantMap.containsKey(t)) {
                        if (ex.optimizations) {
                            interval = minIntervals.get(t);
                        } else {
                            interval = intervalsMap.get(t);
                        }
                    } else {
                        p = this.constantMap.get(t);
                        interval = new Interval<Double>(p, p);
                    }
                    distrNew.add(sTo, interval);
                });
                imdp.addActionLabelledChoice(s, distrNew, action);
            }
        }
        Map<String, BitSet> labels = mdp.getLabelToStatesMap();
        Iterator<Entry<String, BitSet>> it = labels.entrySet().iterator();
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


    /**
     * Update an interval using robust bayesian estimation
     *
     * @param prior      prior probability interval (of doubles)
     * @param strength   prior strength interval (of ints)
     * @param samples    int number of observed samples (nr of transitions (s,a,s'))
     * @param sampleSize int total sample size (nr of times (s,a) was sampled)
     * @return interval of doubles with updated probabilities
     */
    public Interval updateInterval(Interval prior, Interval strength, int samples, int sampleSize) {
        double pointEstimate = samples / sampleSize;
        double priorLower = (Double) prior.getLower();
        double priorUpper = (Double) prior.getUpper();
        double newLower = 0.0;
        double newUpper = 1.0;
        int strengthLower = (Integer) strength.getLower();
        int strengthUpper = (Integer) strength.getUpper();

        // compute new lower
        if (pointEstimate >= priorLower) {
            // data agrees with prior
            newLower = (strengthUpper * priorLower + samples) / (strengthUpper + sampleSize);
        } else {
            // prior-data conflict
            this.conflicts += 1;
            newLower = (strengthLower * priorLower + samples) / (strengthLower + sampleSize);
        }

        // compute new upper
        if (pointEstimate <= priorUpper) {
            // data agrees with prior
            newUpper = (strengthUpper * priorUpper + samples) / (strengthUpper + sampleSize);
        } else {
            // prior-data conflict
            this.conflicts += 1;
            newUpper = (strengthLower * priorUpper + samples) / (strengthLower + sampleSize);
        }

        Interval posterior = new Interval(newLower, newUpper);
        return posterior;
    }

    public Interval updateIntervalSAConflict(Interval prior, Interval strength, int samples, int sampleSize, boolean lbConflict, boolean ubConflict) {
        double priorLower = (Double) prior.getLower();
        double priorUpper = (Double) prior.getUpper();
        double newLower = 0.0;
        double newUpper = 1.0;
        int strengthLower = (Integer) strength.getLower();
        int strengthUpper = (Integer) strength.getUpper();

        if (lbConflict) {
            newLower = (strengthLower * priorLower + samples) / (strengthLower + sampleSize);
        } else {
            newLower = (strengthUpper * priorLower + samples) / (strengthUpper + sampleSize);
        }

        if (ubConflict) {
            newUpper = (strengthLower * priorUpper + samples) / (strengthLower + sampleSize);
        } else {
            newUpper = (strengthUpper * priorUpper + samples) / (strengthUpper + sampleSize);
        }
        if ((newLower <= 0.0) || (newLower >= 1.0)) {
            //System.out.println("Invalid lower bound");
        }
        if ((newUpper <= 0.0) || (newUpper >= 1.0)) {
            //System.out.println("Invalid upper bound");
        }
        Interval posterior = new Interval(newLower, newUpper);
        return posterior;
    }

    /**
     * Updates the strength interval
     *
     * @param strength   current strength interval (of ints)
     * @param sampleSize number of times (s,a) was sampled
     * @return new interval with updated prior strength
     */
    public Interval updateStrength(Interval strength, int sampleSize) {
        int newLowerStrength = Integer.min((Integer) strength.getLower() + sampleSize, this.lowerStrengthBound);
        int newUpperStrength = Integer.min((Integer) strength.getUpper() + sampleSize, this.lowerStrengthBound);
        return new Interval(newLowerStrength, newUpperStrength);
    }


    public void updateIntervals() {
        int nrStates = this.mdp.getNumStates();
        for (int s = 0; s < nrStates; s++) {
            int numChoices = this.mdp.getNumChoices(s);
            for (int i = 0; i < numChoices; i++) {
                String action = getActionString(this.mdp, s, i);
                StateActionPair sa = new StateActionPair(s, action);
                for (int successor = 0; successor < nrStates; successor++) {
                    TransitionTriple t = new TransitionTriple(s, action, successor);
                    if (this.samplesMap.containsKey(t)) {
                        Interval prior = this.intervalsMap.get(t);
                        Interval strength = this.strengthMap.get(t);
                        int sampleSize = this.sampleSizeMap.get(sa);
                        int samples = 0;
                        if (this.samplesMap.containsKey(t)) {
                            samples = this.samplesMap.get(t);
                        }

                        Interval posterior = updateInterval(prior, strength, samples, sampleSize);
                        Interval postStrength = updateStrength(strength, sampleSize);
                        this.intervalsMap.put(t, posterior);
                        this.strengthMap.put(t, postStrength);
                    }
                }
            }
        }
    }

    public void tieParameters() {
        List<List<TransitionTriple>> similarTransitions = this.getSimilarTransitions();

        for (List<TransitionTriple> transitions : similarTransitions) {
            // Compute mode and count over all similar transitions
            int num = 0;
            int denum = 0;
            //System.out.println("Sample size map:" + samplesMap);
            for (TransitionTriple t : transitions) {
                StateActionPair sa = t.getStateAction();
                num += samplesMap.getOrDefault(t, 0);
                denum += sampleSizeMap.getOrDefault(sa, 0);
            }

            for (TransitionTriple t : transitions) {
                StateActionPair sa = t.getStateAction();
//				System.out.println("Transitions: " + t +" Old counts: " + samplesMap.getOrDefault(t, 0) +
//						" " + sampleSizeMap.getOrDefault(sa, 0) + " Tied Counts: " + num + " " + denum);
                tiedTripleCounts.put(t, num);
                tiedStateActionCounts.put(sa, denum);
            }
        }

    }

    public void updateIntervalsSAConflict() {
        int nrStates = this.mdp.getNumStates();
        tieParameters();
        for (int s = 0; s < nrStates; s++) {
            int numChoices = this.mdp.getNumChoices(s);
            for (int i = 0; i < numChoices; i++) {
                String action = getActionString(this.mdp, s, i);
                StateActionPair sa = new StateActionPair(s, action);
                //System.out.println("State action pair : " + sa);
                // do not update posterior of deterministic state-action pairs
                if (!sampleSizeMap.containsKey(sa)) {
                    continue;
                }

                double sum_lb = 0.0;
                double sum_ub = 0.0;

                boolean lbConflict = checkStateActionLBConflict(sa);
                boolean ubConflict = checkStateActionUBConflict(sa);
                //System.out.println("Pair: " + sa + " lbconclif:" + lbConflict + " ubconf " + ubConflict);

                int sampleSize = this.sampleSizeMap.get(sa); //ex.tieParameters ? tiedStateActionCounts.get(sa) :this.sampleSizeMap.get(sa);
                for (int successor = 0; successor < nrStates; successor++) {
                    TransitionTriple t = new TransitionTriple(s, action, successor);

                    // skip states that are not successor
                    if (!this.intervalsMap.containsKey(t))
                        continue;

                    int samples = 0;
                    if (this.samplesMap.containsKey(t)) {
                        samples = this.samplesMap.get(t); //ex.tieParameters ? this.tiedTripleCounts.get(t) : this.samplesMap.get(t);
                    }
                    Interval prior = this.intervalsMap.get(t);
                    Interval strength = this.strengthMap.get(t);

                    Interval posterior = updateIntervalSAConflict(prior, strength, samples, sampleSize, lbConflict, ubConflict);
                    Interval posterior2 = updateIntervalSAConflict(prior, strength, this.tiedTripleCounts.get(t), tiedStateActionCounts.get(sa), lbConflict, ubConflict);

                    Interval postStrength = updateStrength(strength, sampleSize);
                    this.intervalsMap.put(t, posterior);
                    this.strengthMap.put(t, postStrength);
                    //System.out.println("Triple: " + t + " old: " + (double) samples / (double) this.sampleSizeMap.get(sa) + " new " + (double) this.tiedTripleCounts.get(t) / (double) tiedStateActionCounts.get(sa));
                    //System.out.println("Triple: " + t + "OLD post:" + posterior + "strength" + strength + " new posterio" + posterior2);
                    sum_ub += (Double) posterior.getUpper();
                    sum_lb += (Double) posterior.getLower();
                }
                if (this.sampleSizeMap.containsKey(sa)) {
                    if (sum_ub < 1.0)
                        System.out.printf("sum upper bound < 1.0 at %s: %f %n", sa, sum_ub);
                    if (sum_lb > 1.0)
                        System.out.printf("sum lower bound > 1.0 at %s: %f %n", sa, sum_lb);
                }
            }
        }
    }


    public boolean checkStateActionLBConflict(StateActionPair sa) {
        int nrStates = this.mdp.getNumStates();
        for (int successor = 0; successor < nrStates; successor++) {
            TransitionTriple t = new TransitionTriple(sa.getState(), sa.getAction(), successor);
            if (this.samplesMap.containsKey(t)) {
                Interval prior = this.intervalsMap.get(t);
                int sampleSize = this.sampleSizeMap.get(sa);
                int samples = 0;
                if (this.samplesMap.containsKey(t))
                    samples = this.samplesMap.get(t);
                double pointEstimate = samples / sampleSize;
                double priorLower = (Double) prior.getLower();
                double priorUpper = (Double) prior.getUpper();
                if (pointEstimate < priorLower)
                    return true;
            }
        }
        return false;
    }


    public boolean checkStateActionUBConflict(StateActionPair sa) {
        int nrStates = this.mdp.getNumStates();
        for (int successor = 0; successor < nrStates; successor++) {
            TransitionTriple t = new TransitionTriple(sa.getState(), sa.getAction(), successor);
            if (this.samplesMap.containsKey(t)) {
                Interval prior = this.intervalsMap.get(t);
                int sampleSize = this.sampleSizeMap.get(sa);
                int samples = 0;
                if (this.samplesMap.containsKey(t))
                    samples = this.samplesMap.get(t);
                double pointEstimate = samples / sampleSize;
                double priorLower = (Double) prior.getLower();
                double priorUpper = (Double) prior.getUpper();
                if (pointEstimate > priorUpper)
                    return true;
            }
        }
        return false;
    }


    public void checkIntervals() {
        int outside = 0;
        int total = 0;
        int numStates = this.mdp.getNumStates();
        for (int s = 0; s < numStates; s++) {
            int numChoices = this.mdp.getNumChoices(s);
            for (int i = 0; i < numChoices; i++) {
                String action = getActionString(this.mdp, s, i);
                for (int successor = 0; successor < numStates; successor++) {
                    TransitionTriple t = new TransitionTriple(s, action, successor);
                    if (this.trueProbabilitiesMap.containsKey(t)) {
                        total += 1;
                        double p = this.trueProbabilitiesMap.get(t);
                        Interval<Double> interval = this.intervalsMap.get(t);
                        double lower = interval.getLower();
                        double upper = interval.getUpper();
                        if (p < lower || p > upper) {
                            outside++;
                        }
                    }
                }
            }
        }
        System.out.println("There are " + outside + " out of " + total + " wrong intervals");
        System.out.println("There were " + this.conflicts + " prior-data conflicts");
    }


    public IMDP<Double> getEstimate() {
        return this.estimate;
    }

    public ModulesFile getModulesFileMDP() {
        return this.modulesFileMDP;
    }

    public ModulesFile getModulesFileIMDP() {
        return this.modulesFileIMDP;
    }


    public Strategy buildRankedStrat() throws PrismException {
        MRStrategy strat = new MRStrategy(this.mdp);
        int numStates = this.mdp.getNumStates();
        for (int s = 0; s < numStates; s++) {
            int numChoices = this.mdp.getNumChoices(s);
            double[] rank = new double[numChoices];
            double totalrank = 0;
            for (int i = 0; i < numChoices; i++) {
                String action = getActionString(this.mdp, s, i);
                int count = 0;
                double sum = 0;
                for (int successor = 0; successor < numStates; successor++) {
                    Interval<Double> interval = intervalsMap.get(new TransitionTriple(s, action, successor));
                    if (interval != null) {
                        count += 1;
                        double width = interval.getUpper() - interval.getLower();
                        sum += width;
                    }
                }
                rank[i] = sum / count;
                totalrank += rank[i];
            }
            for (int i = 0; i < numChoices; i++) {
                if (totalrank > 0) {
                    strat.setChoiceProbability(s, i, rank[i] / totalrank);
                } else {
                    strat.setChoiceProbability(s, i, 1. / numChoices);
                }
            }
        }
        return strat;
    }

    public Strategy buildRewardStrat(MinMax minMax) throws PrismException {
        if (this.DEBUG) {
            System.out.println("Build reward strat start");
        }
        int numStates = this.mdp.getNumStates();
        MDPRewardsSimple<Double> rewards = new MDPRewardsSimple<>(numStates);
        for (int s = 0; s < numStates; s++) {
            int numChoices = this.mdp.getNumChoices(s);
            for (int i = 0; i < numChoices; i++) {
                String action = getActionString(this.mdp, s, i);
                int count = 0;
                double sum = 0;
                for (int successor = 0; successor < numStates; successor++) {
                    Interval<Double> interval = intervalsMap.get(new TransitionTriple(s, action, successor));
                    if (interval != null) {
                        count += 1;
                        double width = interval.getUpper() - interval.getLower();
                        sum += width;
                    }
                }
                double rank = sum / count;
                rewards.addToTransitionReward(s, i, rank);
            }
        }
        if (this.DEBUG) {
            System.out.println("Build rewards");
        }

        UMDPModelChecker mc = new UMDPModelChecker(this.prism);
        mc.setGenStrat(true);
        mc.setErrorOnNonConverge(false);
        Expression exprTarget = this.prism.parsePropertiesString(ex.goal).getProperty(0);
        if (this.DEBUG) {
            System.out.println("got exprTarget");
        }

        BitSet target = mc.checkExpression(estimate, exprTarget, null).getBitSet();
        if (this.DEBUG) {
            System.out.println("Build rewards");
        }

        if (this.DEBUG) {
            System.out.println("Getting reward strat");
        }
        ModelCheckerResult result = mc.computeReachRewards(estimate, rewards, target, minMax);
        MDStrategy strat = (MDStrategy) result.strat;
        return strat;
    }

}

class BayesianEstimatorUniform extends BayesianEstimator {

    public BayesianEstimatorUniform(Prism prism, Experiment ex) {
        super(prism, ex);
    }

    public Strategy buildStrategy() throws PrismException {
        return this.buildUniformStrat();
    }
}

class BayesianEstimatorRank extends BayesianEstimator {
    public BayesianEstimatorRank(Prism prism, Experiment ex) {
        super(prism, ex);
    }

    public Strategy buildStrategy() throws PrismException {
        return this.buildRankedStrat();
    }
}

class BayesianEstimatorReward extends BayesianEstimator {
    public BayesianEstimatorReward(Prism prism, Experiment ex) {
        super(prism, ex);
    }

    public Strategy buildStrategy() throws PrismException {

        return this.buildRewardStrat(MinMax.max().setMinUnc(false));
    }
}

class BayesianEstimatorOptimistic extends BayesianEstimator {
    public BayesianEstimatorOptimistic(Prism prism, Experiment ex) {
        super(prism, ex);
    }

    public Strategy buildStrategy() throws PrismException {
        return this.buildWeightedOptimisticStrategy(this.getEstimate(), this.ex.strategyWeight);
    }
}