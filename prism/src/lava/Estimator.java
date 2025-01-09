package lava;

import common.Interval;
import explicit.IMDP;
import explicit.MDP;
import explicit.UMDPModelChecker;
import param.Function;
import parser.Values;
import parser.ast.Expression;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import prism.*;
import simulator.ModulesFileModelGenerator;
import strat.MDStrategy;
import strat.MRStrategy;
import strat.Strategy;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

interface EstimatorConstructor {
    Estimator get(Prism prism, Experiment ex);
}

public class Estimator {
    protected String name = "Base";
    protected Prism prism;

    protected Experiment ex;

    //  protected String prismFile;
    // protected String spec;
    //protected String robustSpec;
    // protected String dtmcSpec;
    // protected String target;
    // protected String type;

    protected HashMap<TransitionTriple, Integer> samplesMap;
    protected HashMap<StateActionPair, Integer> sampleSizeMap;
    protected HashMap<TransitionTriple, Interval<Double>> intervalsMap;
    protected Map<TransitionTriple, Double> constantMap;
    protected ModulesFile modulesFile;
    protected ModulesFile modulesFileIMDP;
    protected ModulesFile modulesFileMDP;
    protected MDP<Double> mdp;
    protected String SULoptimum;
    protected double sulOpt;
    protected HashSet<Integer> prob01States;
    protected HashSet<Integer> rew0InfStates;
    protected IMDP<Double> estimate;
    protected HashMap<TransitionTriple, Double> trueProbabilitiesMap;
    private Map<Function, List<TransitionTriple>> functionMap;
    // Contains lists of similar transitions, whose counts can be tied
    private List<List<TransitionTriple>> similarTransitions;
    private final HashSet<TransitionTriple> transitionsOfInterest;
    private int numLearnableTransitions = 0;

    private MRStrategy uniformStrat;

    Estimator(Prism prism, Experiment ex) {
        this.prism = prism;
        this.ex = ex;

        this.samplesMap = new HashMap<>();
        this.sampleSizeMap = new HashMap<>();
        this.intervalsMap = new HashMap<>();

        this.prob01States = new HashSet<>();
        this.rew0InfStates = new HashSet<>();

        this.numLearnableTransitions = 0;
        this.transitionsOfInterest = new HashSet<>();
        this.trueProbabilitiesMap = new HashMap<>();

        this.constantMap = new HashMap<>();

        this.buildModulesFiles();
        this.tryBuildSUL();
        //this.processTransitions();
    }

    public void set_experiment(Experiment ex) {
        this.ex = ex;
        this.buildModulesFiles();
        this.tryBuildSUL();

        if (this.ex.optimizations) {
            this.processTransitions();
        } else {
            this.processTransitionsNaive();
        }
    }

    public void set_experiment_naive(Experiment ex) {
        this.ex = ex;
        this.buildModulesFiles();
        this.tryBuildSUL();
        this.processTransitionsNaive();
    }

    private void buildModulesFiles() {
        try {
            this.modulesFile = this.prism.parseModelFile(new File(ex.modelFile));
            this.modulesFileIMDP = this.prism.parseModelFile(new File(ex.modelFile), ModelType.IMDP);
            this.modulesFileMDP = this.prism.parseModelFile(new File(ex.modelFile), ModelType.MDP);
        } catch (FileNotFoundException e) {
            System.out.println("Error file: " + e.getMessage());
            System.exit(1);
        } catch (PrismLangException e) {
            System.out.println("Error parsing file: " + e.getMessage());
            System.exit(1);
        } catch (NullPointerException e) {
            System.out.println("Error null: " + e.getMessage());
            System.exit(1);
        }
    }

    public Double round(double value) {
        return round(value, 8);
    }

    public Double round(double value, int precision) {
//        if (value > 1) {
//            value = 1.0;
//        }
        if (value == Double.POSITIVE_INFINITY) {
            return 1.0;
        }
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(precision, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }


    public void setSamplesMap(HashMap<TransitionTriple, Integer> samplesMap) {
        this.samplesMap = samplesMap;
    }

    public void setSampleSizeMap(HashMap<StateActionPair, Integer> sampleSizeMap) {
        this.sampleSizeMap = sampleSizeMap;
    }

    public void setObservationMaps(HashMap<TransitionTriple, Integer> samplesMap, HashMap<StateActionPair, Integer> sampleSizeMap) {
        setSampleSizeMap(sampleSizeMap);
        setSamplesMap(samplesMap);
    }

    public String getActionString(MDP<Double> mdp, int s, int i) {
        String action = (String) mdp.getAction(s, i);
        if (action == null) {
            action = "_empty";
        }
        return action;
    }

    private void tryBuildSUL() {
        try {
            this.buildSUL();
        } catch (PrismException e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private void tryBuildSUUncertain(Values values) {
        try {
            this.buildSULUncertain(values);
        } catch (PrismException e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    public void buildSULUncertain(Values values) throws PrismException {
        this.prism.loadPRISMModel(modulesFile);
        //this.prism.currentModelInfo.getConstantValues().setValues(values);
        this.prism.getPRISMModel().getConstantValues().setValues(values); // Set constant values to sampled parameters
        this.prism.setStoreVector(true);
        Result result = this.prism.modelCheck(ex.spec);
        //System.out.println(result);
        MDP<Double> mdp = (MDP<Double>) this.prism.getBuiltModelExplicit();
        //System.out.println("Model checking SUL:\n" + this.spec + " : " + result.getResultAndAccuracy());
        this.SULoptimum = result.getResultAndAccuracy();
        this.sulOpt = (Double) result.getResult();
        this.ex.setTrueOpt(this.sulOpt);
        this.mdp = mdp;
        ArrayList<Integer> initialStates = (ArrayList<Integer>) this.mdp.getInitialStates();
        if (ex.type == Experiment.Type.REACH) {
            computeProb01States(result);
            for (int s : initialStates) {
                if (this.prob01States.contains(s)) {
                    System.out.println("Initial state " + s + " is a 01 prob state. Exiting.");
                    System.exit(0);
                }
            }
        } else if (ex.type == Experiment.Type.REWARD) {
            computeRew0InfStates(result);
        } else {
            System.out.println("ERROR: unsupported type unknown " + ex.type);
            System.exit(1);
        }
    }


    public IMDP<Double> getEstimate() {
        return this.estimate;
    }

    /**
     * Build the SUL from the prism model file
     */
    public void buildSUL() throws PrismException {
        this.prism.loadPRISMModel(modulesFile);
        if (ex.values != null) {
            this.prism.setPRISMModelConstants(ex.values);
        }
        this.prism.setStoreVector(true);
        Result result = this.prism.modelCheck(ex.spec);
        //System.out.println(result);
        MDP<Double> mdp = (MDP<Double>) this.prism.getBuiltModelExplicit();
        //System.out.println("Model checking SUL:\n" + this.spec + " : " + result.getResultAndAccuracy());
        this.SULoptimum = result.getResultAndAccuracy();
        this.sulOpt = (Double) result.getResult();
        this.ex.setTrueOpt(this.sulOpt);
        this.mdp = mdp;
        ArrayList<Integer> initialStates = (ArrayList<Integer>) this.mdp.getInitialStates();
        if (ex.type == Experiment.Type.REACH) {
            computeProb01States(result);
            for (int s : initialStates) {
                if (this.prob01States.contains(s)) {
                    System.out.println("Initial state " + s + " is a 01 prob state. Exiting.");
                    System.exit(0);
                }
            }
        } else if (ex.type == Experiment.Type.REWARD) {
            computeRew0InfStates(result);
        } else {
            System.out.println("ERROR: unsupported type unknown " + ex.type);
            System.exit(1);
        }
    }

    public MDP<Double> getSUL() {
        return this.mdp;
    }

    public double getSulOpt() {
        return this.sulOpt;
    }


    public double averageDistanceToSUL() {
        return -1.0;
    }


    public String getModelStats() {
        String stats = "%------\n%Model stats\n%";
        stats += "  #States: " + this.mdp.getNumStates() + "\n%";
        stats += "  #transitions: " + this.mdp.getNumTransitions() + "  of which  " + this.numLearnableTransitions + "  p < 1\n%";
        stats += "  true MDP optimum for " + ex.spec + "  =  " + this.SULoptimum + "\n%";
        stats += "------";
        return stats;
    }


    public HashSet<Integer> getTerminatingStates() {
        if (ex.type == Experiment.Type.REACH)
            return this.prob01States;
        else {
            return this.rew0InfStates;
        }
    }

    public void processTransitionsNaive() {
        this.numLearnableTransitions = 0;
        this.transitionsOfInterest.clear();
        int numStates = this.mdp.getNumStates();
        //System.out.println("Processing Transitions");
        for (int s = 0; s < numStates; s++) {
            //System.out.println("State:" + s);
            int numChoices = this.mdp.getNumChoices(s);
            final int state = s;
            for (int i = 0; i < numChoices; i++) {
                //System.out.println("Choice:" + i);
                final String action = getActionString(this.mdp, s, i);
                //System.out.println("Action String:" + action);
                this.mdp.forEachDoubleTransition(s, i, (int sFrom, int sTo, double p) -> {
                    //System.out.println("State to:" + sTo + " with Prob: " + p);
                    if (0 < p && p < 1.0) {
                        this.numLearnableTransitions += 1;
                        this.transitionsOfInterest.add(new TransitionTriple(state, action, sTo));
                        this.trueProbabilitiesMap.put(new TransitionTriple(state, action, sTo), p);
                    }
                });
            }
        }
    }

    public void processTrueTransitions() {
        //this.numLearnableTransitions = 0;
        //this.transitionsOfInterest.clear();
        int numStates = this.mdp.getNumStates();
        //System.out.println("Processing Transitions");
        for (int s = 0; s < numStates; s++) {
            //System.out.println("State:" + s);
            int numChoices = this.mdp.getNumChoices(s);
            final int state = s;
            for (int i = 0; i < numChoices; i++) {
                //System.out.println("Choice:" + i);
                final String action = getActionString(this.mdp, s, i);
                //System.out.println("Action String:" + action);
                this.mdp.forEachDoubleTransition(s, i, (int sFrom, int sTo, double p) -> {
                    //System.out.println("State to:" + sTo + " with Prob: " + p);
                    if (0 < p && p < 1.0) {
                        //this.numLearnableTransitions += 1;
                        //this.transitionsOfInterest.add(new TransitionTriple(state, action, sTo));
                        this.trueProbabilitiesMap.put(new TransitionTriple(state, action, sTo), p);
                    }
                });
            }
        }
    }

    public void processTransitions() {
        this.numLearnableTransitions = 0;
        this.transitionsOfInterest.clear();

        for (Function function : this.functionMap.keySet()) {
            List<TransitionTriple> transitions = this.functionMap.get(function);
            if (function.isConstant()) {
                for (TransitionTriple transition : transitions) {
                    this.constantMap.put(transition, function.asBigRational().doubleValue());
                }
            } else {
                this.numLearnableTransitions += transitions.size();
                this.transitionsOfInterest.addAll(transitions);
            }
        }
        processTrueTransitions();
    }


    public double maxIntervalPointDistance(Interval<Double> interval, double p) {
        double lower = interval.getLower();
        double upper = interval.getUpper();
        double d1 = Math.abs(p - lower);
        double d2 = Math.abs(p - upper);
        double maxDist = Double.max(d1, d2);
        return maxDist;
    }


    public HashSet<TransitionTriple> getTransitionsOfInterest() {
        return this.transitionsOfInterest;
    }

    public int getNumLearnableTransitions() {
        return this.numLearnableTransitions;
    }

    public void computeProb01States(Result result) throws PrismException {
        //System.out.println("compute");
        StateVector vector = result.getVector();
        //System.out.println("vector = " + vector);
        int numStates = this.mdp.getNumStates();
        for (int s = 0; s < numStates; s++) {
            double value = (Double) vector.getValue(s);
            if (value == 0.0) {
                this.prob01States.add(s);
            } else if (value == 1.0) {
                this.prob01States.add(s);
            } else {
                continue;
            }
        }
    }

    public void computeRew0InfStates(Result result) throws PrismException {
        //System.out.println("compute");
        StateVector vector = result.getVector();
        //System.out.println("vector = " + vector);
        int numStates = this.mdp.getNumStates();
        for (int s = 0; s < numStates; s++) {
            double value = (Double) vector.getValue(s);
            if (value == 0.0) {
                this.rew0InfStates.add(s);
            } else if (value == Double.POSITIVE_INFINITY) {
                this.rew0InfStates.add(s);
            }
        }
    }

    public Strategy buildStrategy() throws PrismException {
        throw new UnsupportedOperationException("build strategy is undefined");
    }

    public Strategy buildUniformStrat() {
        if (this.uniformStrat == null) {
            MRStrategy strat = new MRStrategy(this.mdp);
            int numStates = this.mdp.getNumStates();
            for (int s = 0; s < numStates; s++) {
                int numChoices = this.mdp.getNumChoices(s);
                for (int i = 0; i < numChoices; i++) {
                    strat.setChoiceProbability(s, i, 1.0 / numChoices);
                }
            }
            this.uniformStrat = strat;
        }
        return this.uniformStrat;
    }


    public MDStrategy computeStrategyFromEstimate(IMDP<Double> estimate) throws PrismException {
        return this.computeStrategyFromEstimate(estimate, true);
    }

    public MDStrategy computeOptimisticStrategyFromEstimate(IMDP<Double> estimate) throws PrismException {
        return this.computeStrategyFromEstimate(estimate, false);
    }


    public Strategy buildWeightedOptimisticStrategy(IMDP<Double> estimate, double weight) throws PrismException {
        MDStrategy optStrat = computeOptimisticStrategyFromEstimate(estimate);
        Strategy uniformStrat = buildUniformStrat();
        MRStrategy strat = new MRStrategy(this.mdp);
        int numStates = this.mdp.getNumStates();
        for (int s = 0; s < numStates; s++) {
            int numChoices = this.mdp.getNumChoices(s);
            int optimisticChoice = optStrat.getChoiceIndex(s);
            if (optimisticChoice >= 0 && numChoices > 1) {
                for (int i = 0; i < numChoices; i++) {
                    if (i == optimisticChoice)
                        strat.setChoiceProbability(s, i, weight);
                    else
                        strat.setChoiceProbability(s, i, (1.0 - weight) / (numChoices - 1));
                }
            } else {
                for (int i = 0; i < numChoices; i++) {
                    strat.setChoiceProbability(s, i, 1.0 / numChoices);
                }
            }
        }
        return strat;
    }


    public MDStrategy computeStrategyFromEstimate(IMDP<Double> estimate, boolean robust) throws PrismException {
        UMDPModelChecker mc = new UMDPModelChecker(this.prism);
        mc.setGenStrat(true);
        mc.setPrecomp(true);
        mc.setErrorOnNonConverge(false);
        mc.setMaxIters(100000);

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

    public double[] getCurrentResults() throws PrismException {
        throw new UnsupportedOperationException("can't get results from estimator");
    }

    public double[] getInitialResults() throws PrismException {
        throw new UnsupportedOperationException("can't get results from estimator");
    }


    public List<Double> getLowerBounds() {
        // TODO: get bounds
        List<Double> lbs = new ArrayList<>();
        int numStates = this.mdp.getNumStates();
        for (int s = 0; s < numStates; s++) {
            int numChoices = this.mdp.getNumChoices(s);
            for (int i = 0; i < numChoices; i++) {
                StateActionPair sa = new StateActionPair(s, getActionString(this.mdp, s, i));
                for (int successor = 0; successor < this.mdp.getNumStates(); successor++) {
                    TransitionTriple t = new TransitionTriple(sa.getState(), sa.getAction(), successor);
                    if (this.trueProbabilitiesMap.containsKey(t)) {
                        if (this.trueProbabilitiesMap.get(t) == 1.0) {
                            break;
                        }
                        lbs.add(this.intervalsMap.get(t).getLower());
                    }
                }
            }
        }
        return lbs;
    }

    public List<Double> getUpperBounds() {
        List<Double> ubs = new ArrayList<>();
        int numStates = this.mdp.getNumStates();
        for (int s = 0; s < numStates; s++) {
            int numChoices = this.mdp.getNumChoices(s);
            for (int i = 0; i < numChoices; i++) {
                StateActionPair sa = new StateActionPair(s, getActionString(this.mdp, s, i));
                for (int successor = 0; successor < this.mdp.getNumStates(); successor++) {
                    TransitionTriple t = new TransitionTriple(sa.getState(), sa.getAction(), successor);
                    if (this.trueProbabilitiesMap.containsKey(t)) {
                        if (this.trueProbabilitiesMap.get(t) == 1.0) {
                            break;
                        }
                        ubs.add(this.intervalsMap.get(t).getUpper());
                    }
                }
            }
        }
        return ubs;
    }

    public String getName() {
        return this.name;
    }

    public Map<Function, List<TransitionTriple>> getFunctionMap() {
        return functionMap;
    }

    public void setFunctionMap(Map<Function, List<TransitionTriple>> functionMap) {
        this.functionMap = functionMap;
    }

    public List<List<TransitionTriple>> getSimilarTransitions() {
        return similarTransitions;
    }

    public void setSimilarTransitions(List<List<TransitionTriple>> similarTransitions) {
        this.similarTransitions = similarTransitions;
    }
}

