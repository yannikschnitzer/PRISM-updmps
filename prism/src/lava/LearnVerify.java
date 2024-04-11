package lava;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

import explicit.MDP;
import explicit.MDPSimple;
import param.BigRational;
import param.Function;
import param.Point;
import parser.Values;
import parser.ast.ModulesFile;
import prism.Evaluator;
import prism.Pair;
import prism.Prism;
import prism.PrismDevNullLog;
import prism.PrismException;
import prism.PrismLangException;
import strat.Strategy;

import lava.Experiment.Model;

public class LearnVerify {

    private Prism prism;
    private String modelStats = null;

    private int seed = 1650280571;

    private boolean verbose = true;


    public LearnVerify() {
    }

    public LearnVerify(boolean verbose) {
        this.verbose = verbose;
    }

    public LearnVerify(int seed) {
        this.seed = seed;
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        if (args.length > 0) {
            int seed;
            for (String s: args) {
                try {
                    seed = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    System.out.println("skipping invalid seed " + s);
                    continue;
                }
                System.out.println("running with seed " + s);
                LearnVerify l = new LearnVerify(seed);
                l.basic();
                l.switching_environment();
                l.gridStrengthEval();
                //l.evaluate_strength();
            }
        }
        else {
            System.out.println("running with default seed");
            LearnVerify l = new LearnVerify();
            l.basic();
//            l.switching_environment();
//            l.gridStrengthEval();
//            l.evaluate_strength();
        }
    }

    public void basic() {
        String id = "basic";
        //run_basic_algorithms(new Experiment(Model.CHAIN_SMALL).config(100, 1000, seed).info(id));
        //run_basic_algorithms(new Experiment(Model.LOOP).config(100, 1000, seed).info(id));
        run_basic_algorithms(new Experiment(Model.AIRCRAFT).config(100, 1_000_000, seed, true, true,4).info(id));
//        run_basic_algorithms(new Experiment(Model.AIRCRAFT).config(100, 1_000_000, seed, true, false, 4).info(id));
//        run_basic_algorithms(new Experiment(Model.AIRCRAFT).config(100, 1_000_000, seed, false, false, 10).info(id));
        //run_basic_algorithms(new Experiment(Model.BRP).config(100, 1_00_000, seed, true, true, 10).info(id));
//        run_basic_algorithms(new Experiment(Model.BRP).config(100, 1_00_000, seed, true, false, 10).info(id));
//        run_basic_algorithms(new Experiment(Model.BRP).config(100, 1_00_000, seed, false, false, 50).info(id));
        //run_basic_algorithms(new Experiment(Model.NAND).config(50, 1_0_000, seed, false).info(id));
        //run_basic_algorithms(new Experiment(Model.DRONE).config(50, 1_000, seed, false).info(id));
        // run_basic_algorithms(new Experiment(Model.NAND).config(50, 1_000_000, seed, true).info(id));
        //run_basic_algorithms(new Experiment(Model.AIRCRAFT).config(102, 1_000_000, seed, false).info(id));
        //run_basic_algorithms(new Experiment(Model.SAV2).config(100, 1_000_000, seed, true, true,5).info(id));
        //run_basic_algorithms(new Experiment(Model.SAV2).config(100, 1_000_000, seed, true, false,5).info(id));
        //run_basic_algorithms(new Experiment(Model.SAV2).config(100, 1_000_000, seed, false, false, 50).info(id));
        //run_basic_algorithms(new Experiment(Model.CONSENSUS2).config(20, 1_000_000, seed, false).info(id));
        //run_basic_algorithms(new Experiment(Model.CONSENSUS2).config(100, 1_000_000, seed, true).info(id));
        //run_basic_algorithms(new Experiment(Model.CONSENSUS4).config(100, 1_000_000, seed, false).info(id));
        //run_basic_algorithms(new Experiment(Model.CONSENSUS4).config(100, 1_000_000, seed, true).info(id));
//        run_basic_algorithms(new Experiment(Model.CROWD).config(100, 1_000_000, seed, true, true,5).info(id));
//        run_basic_algorithms(new Experiment(Model.CROWD).config(100, 1_000_000, seed, true, false, 5).info(id));
//        run_basic_algorithms(new Experiment(Model.CROWD).config(100, 1_000_000, seed, false, false, 5).info(id));

        //run_basic_algorithms(new Experiment(Model.BANDIT).config(100, 1000000, seed).stratWeight(0.9).info(id));
//        run_basic_algorithms(new Experiment(Model.BETTING_GAME_FAVOURABLE).config(30, 1_000_000, seed, true, true, 2).info(id));
//        run_basic_algorithms(new Experiment(Model.BETTING_GAME_FAVOURABLE).config(30, 1_000_000, seed, true, false, 2).info(id));
 //       run_basic_algorithms(new Experiment(Model.BETTING_GAME_FAVOURABLE).config(30, 1_000_000, seed, false, false,10).info(id));
        //run_basic_algorithms(new Experiment(Model.BETTING_GAME_UNFAVOURABLE).config(7, 1000000, seed).info(id));
        //run_basic_algorithms(new Experiment(Model.TINY).config(2, 50000, seed).info(id));
        //run_basic_algorithms(new Experiment(Model.TINY2).config(2, 50000, seed).info(id));
//        run_basic_algorithms(new Experiment(Model.CHAIN_LARGE).config(100, 1_000_000, seed, true, true, 1).info(id));
//        run_basic_algorithms(new Experiment(Model.CHAIN_LARGE).config(100, 1_000_000, seed, true, false, 1).info(id));
//        run_basic_algorithms(new Experiment(Model.CHAIN_LARGE).config(100, 1_000_000, seed, false, false, 4).info(id));
        //run_basic_algorithms(new Experiment(Model.GRID).config(200, 1000000, seed, 20, 30).info(id));
    }

    private void run_basic_algorithms(Experiment ex) {
        String postfix = String.format("_seed_%d", ex.seed);
        postfix += ex.tieParameters ? "_tied" : (ex.optimizations ? "_opt" : "_naive");
//        compareSamplingStrategies("UCRL2" + postfix, ex.setErrorTol(0.01), UCRL2IntervalEstimatorOptimistic::new);
        compareSamplingStrategies("PAC" + postfix, ex.setErrorTol(0.01), PACIntervalEstimatorOptimistic::new);
//        compareSamplingStrategies("MAP_uni" + postfix, ex, MAPEstimator::new);
//        compareSamplingStrategies("LUI" + postfix, ex, BayesianEstimatorOptimistic::new);
        //ex.initialInterval = Experiment.InitialInterval.UNIFORM;
        //new LearnVerify(ex.seed).compareSamplingStrategies("Bayes(uniform prior)", ex, BayesianEstimatorOptimistic::new);
    }

    public void gridStrengthEval() {
        String id = "grid-strength-eval";
        String postfix = String.format("_seed_%d", seed);
        int iterations = 1000000;

        Experiment ex1 = new Experiment(Model.GRID).config(200, iterations, seed, 5, 15).info(id);
        String label_format = "LUIstr%d-%d" + postfix;
        String label = String.format(label_format, ex1.initLowerStrength, ex1.initUpperStrength);
        compareSamplingStrategies(label, ex1, BayesianEstimatorOptimistic::new);

        Experiment ex2 = new Experiment(Model.GRID).config(200, iterations, seed, 10, 15).info(id);
        label = String.format(label_format, ex2.initLowerStrength, ex2.initUpperStrength);
        compareSamplingStrategies(label, ex2, BayesianEstimatorOptimistic::new);

        Experiment ex3 = new Experiment(Model.GRID).config(200, iterations, seed, 15, 20).info(id);
        label = String.format(label_format, ex3.initLowerStrength, ex3.initUpperStrength);
        compareSamplingStrategies(label, ex3, BayesianEstimatorOptimistic::new);

        Experiment ex4 = new Experiment(Model.GRID).config(200, iterations, seed, 20, 30).info(id);
        label = String.format(label_format, ex4.initLowerStrength, ex4.initUpperStrength);
        compareSamplingStrategies(label, ex4, BayesianEstimatorOptimistic::new);

        Experiment ex5 = new Experiment(Model.GRID).config(200, iterations, seed, 30, 40).info(id);
        label = String.format(label_format, ex5.initLowerStrength, ex5.initUpperStrength);
        compareSamplingStrategies(label, ex5, BayesianEstimatorOptimistic::new);

        Experiment ex6 = new Experiment(Model.GRID).config(200, iterations, seed, 40, 50).info(id);
        label = String.format(label_format, ex6.initLowerStrength, ex6.initUpperStrength);
        compareSamplingStrategies(label, ex6, BayesianEstimatorOptimistic::new);
    }

    public void evaluate_strength() {
        String id = "evaluate_strength";
        strength_evaluation(new Experiment(Model.TINY).config(2, 500, seed).info(id));
        strength_evaluation(new Experiment(Model.TINY2).config(2, 500, seed).info(id));
    }

    private void strength_evaluation(Experiment ex) {
        List<Pair<Integer, Integer>> strengths = new ArrayList<>();
        strengths.add(new Pair<>(0, 1));
        strengths.add(new Pair<>(1, 2));
        strengths.add(new Pair<>(2, 3));
        strengths.add(new Pair<>(3, 4));
        strengths.add(new Pair<>(5, 10));
        strengths.add(new Pair<>(15, 20));
        strengths.add(new Pair<>(25, 30));
        strengths.add(new Pair<>(35, 40));
        strengths.add(new Pair<>(45, 50));
        for (Entry<Integer, Integer> entry : strengths) {
            int lowerStrength = entry.getKey();
            int upperStrength = entry.getValue();
            ex.initialInterval = Experiment.InitialInterval.WIDE;
            ex.setPriors(1e-4, lowerStrength, upperStrength);
            String label = String.format("uMDP $\\underline{n}$=%d $\\overline{n}$=%d", ex.initLowerStrength, ex.initUpperStrength);
            compareSamplingStrategies(label, ex, BayesianEstimatorOptimistic::new);
        }
        ex.alpha = 2;
        compareSamplingStrategies(String.format("MAP(%d)", ex.alpha), ex, MAPEstimator::new);
    }

    public void switching_environment() {
        List<Double> strategyWeights = new ArrayList<>(List.of(1.0, 0.9, 0.8));
        for (Double x : strategyWeights) {
            String id = "switching-weight-"+x;
            List<Integer> switching_points = new ArrayList<>(List.of(100, 1000, 10000, 100000)); 
            for (Integer j : switching_points) {
                experiment_switching_environment(
                        new Experiment(Model.CHAIN_LARGE).config(100, j, seed).stratWeight(x).info(id),
                        new Experiment(Model.CHAIN_LARGE2).config(100, 1000000-j, seed).stratWeight(x).info(id),
                        j
                );
            }
        }
        for (Double x : strategyWeights) {
            String id = "switching-weight-"+x;
            List<Integer> switching_points = new ArrayList<>(List.of(100, 1000, 10000, 100000)); 
            for (Integer j : switching_points) {
                experiment_switching_environment(
                        new Experiment(Model.BETTING_GAME_FAVOURABLE).config(7, j, seed).stratWeight(x).info(id),
                        new Experiment(Model.BETTING_GAME_UNFAVOURABLE).config(7, 1000000-j, seed).stratWeight(x).info(id),
                        j
                );
            }
        }
    }

    private void experiment_switching_environment(Experiment ex1, Experiment ex2, int switching_point) {
        String postfix = String.format("_switching_point_%d_seed_%d", switching_point, seed);
        compareSamplingStrategies("UCRL2_unbounded" + postfix, ex1, UCRL2IntervalEstimatorOptimistic::new, ex2);
        compareSamplingStrategies("MAP_unbounded" + postfix, ex1, MAPEstimator::new, ex2);
        compareSamplingStrategies("LUI_unbounded" + postfix, ex1, BayesianEstimatorOptimistic::new, ex2);

        ex1 = ex1.setStrengthBounds(200, 300, 300);
        ex2 = ex2.setStrengthBounds(200, 300, 300);
        compareSamplingStrategies("UCRL2_highbound" + postfix, ex1, UCRL2IntervalEstimatorOptimistic::new, ex2);
        compareSamplingStrategies("MAP_highbound" + postfix, ex1, MAPEstimator::new, ex2);
        compareSamplingStrategies("LUI_highbound" + postfix, ex1, BayesianEstimatorOptimistic::new, ex2);

        ex1 = ex1.setStrengthBounds(20, 30, 30);
        ex2 = ex2.setStrengthBounds(20, 30, 30);
        compareSamplingStrategies("UCRL2_lowbound" + postfix, ex1, UCRL2IntervalEstimatorOptimistic::new, ex2);
        compareSamplingStrategies("MAP_lowbound" + postfix, ex1, MAPEstimator::new, ex2);
        compareSamplingStrategies("LUI_lowbound" + postfix, ex1, BayesianEstimatorOptimistic::new, ex2);

        //new LearnVerify(true).compareSamplingStrategies("Bayes-SW-small" + postfix, ex1.setStrengthBounds(10, 20), BayesianEstimatorWeightedOptimistic::new, ex2.setStrengthBounds(10, 20));
        //new LearnVerify(true).compareSamplingStrategies("Bayes-SW-large" + postfix, ex1.setStrengthBounds(100, 200), BayesianEstimatorWeightedOptimistic::new, ex2.setStrengthBounds(100, 200));
        //new LearnVerify(true).compareSamplingStrategies("Bayes-uni" + postfix, ex1, BayesianEstimatorUniform::new, ex2);
        //new LearnVerify(true).compareSamplingStrategies("Bayes-uni-SW-small" + postfix, ex1.setStrengthBounds(10, 20), BayesianEstimatorUniform::new, ex2.setStrengthBounds(10, 20));
        //new LearnVerify(true).compareSamplingStrategies("Bayes-uni-SW-large" + postfix, ex1.setStrengthBounds(100, 200), BayesianEstimatorUniform::new, ex2.setStrengthBounds(100, 200));
        //ex1.initialInterval = Experiment.InitialInterval.UNIFORM;
        //new LearnVerify(true).compareSamplingStrategies("Bayes(uniform prior)" + postfix, ex1, BayesianEstimatorOptimistic::new, ex2);
    }

    @SuppressWarnings("unchecked")
    public void initializePrism() throws PrismException {
        this.prism = new Prism(new PrismDevNullLog());
        this.prism.initialise();
        this.prism.setEngine(Prism.EXPLICIT);
        this.prism.setGenStrat(true);
    }

    public void resetAll() {
        try {
            this.modelStats = null;
            initializePrism();
            this.prism.setSimulatorSeed(seed);
        } catch (PrismException e) {
            System.out.println("PrismException in LearnVerify.resetAll()  :  " + e.getMessage());
            System.exit(1);
        }
    }

    public String makeOutputDirectory(Experiment ex) {
        String outputPath = String.format("results/%s/%s/", ex.experimentInfo, ex.model.toString());
        try {
            Files.createDirectories(Paths.get(outputPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputPath;
    }


//    public void compareSamplingStrategies(String label, Experiment ex, EstimatorConstructor estimatorConstructor){
//        compareSamplingStrategies(label, ex, estimatorConstructor, null);
//    }

    public void compareSamplingStrategies(String label, Experiment ex, EstimatorConstructor estimatorConstructor){

        List<Values> values = new ArrayList<>();

        double rangeMin1 = 0.5;
        double rangeMax1 = 0.7;

        //double rangeMean = rangeMin + (rangeMax - rangeMin) / 2;
        Random r = new Random(5599);

        for (int i = 0; i < 1; i++){
            Values v = new Values();
            double p = rangeMin1 + (rangeMax1 - rangeMin1) * r.nextDouble(); //r.nextGaussian();
            v.addValue("r", p);
            values.add(v);
        }

        compareSamplingStrategiesUncertain(label, ex, estimatorConstructor, values);
    }

    public void compareSamplingStrategies(String label, Experiment ex, EstimatorConstructor estimatorConstructor, Experiment follow_up_ex) {
        resetAll();
        System.out.println("\n\n\n\n%------\n%Compare sampling strategies on\n%  Model: " + ex.model + "\n%  max_episode_length: "
                + ex.max_episode_length + "\n%  iterations: " + ex.iterations + "\n%  Prior strength: ["
                + ex.initLowerStrength + ", " + ex.initUpperStrength + "]\n%------");

        if (verbose)
            System.out.printf("%s, seed %d\n", label, ex.seed);

        Estimator estimator = estimatorConstructor.get(this.prism, ex);
        String directoryPath = makeOutputDirectory(ex);

        String path = directoryPath + label +".csv";
        if (Files.exists(Paths.get(path))) {
            System.out.printf("File %s already exists.%n", path);
            //return;
        }

        ex.dumpConfiguration(directoryPath, label, estimator.getName());

        ArrayList<DataPoint> results = runSamplingStrategyDoublingEpoch(ex, estimator);
        if (follow_up_ex != null) {
            estimator.set_experiment(follow_up_ex);
            results.addAll( runSamplingStrategyDoublingEpoch(follow_up_ex, estimator, ex.iterations));
            follow_up_ex.dumpConfiguration(directoryPath, label + "_part_2", estimator.getName());
        }

        DataProcessor dp = new DataProcessor();
        dp.dumpRawData(directoryPath, label, results, ex);
    }

    public String getActionString(MDP<Function> mdp, int s, int i) {
        String action = (String) mdp.getAction(s,i);
        if (action == null) {
            action = "_empty";
        }
        return action;
    }

    public Map<Function, List<TransitionTriple>> getFunctionMap(MDP<Function> mdpParam){
        Map<Function, List<TransitionTriple>> functionMap = new HashMap<>();

        for (int s = 0; s < mdpParam.getNumStates(); s++) {
            int numChoices = mdpParam.getNumChoices(s);
            for (int i = 0 ; i < numChoices; i++) {
                //String action = (String) mdpParam.getAction(s, i);
                String action = getActionString(mdpParam, s, i);
                mdpParam.forEachTransition(s, i, (int sFrom, int sTo, Function p)->{
                    if (functionMap.containsKey(p)) {
                        functionMap.get(p).add(new TransitionTriple(sFrom,  action, sTo));
                    } else {
                        functionMap.put(p, new ArrayList<>());
                        functionMap.get(p).add(new TransitionTriple(sFrom,  action, sTo));
                    }
                });
            }
        }

        return functionMap;
    }

    public Set<Function> getTransitionStructure(MDP<Function> mdpParam, int s) {
        HashSet<Function> transitions = new HashSet<>();
        int numChoices = mdpParam.getNumChoices(s);
        for (int i = 0 ; i < numChoices; i++) {
            mdpParam.forEachTransition(s, i, (int sFrom, int sTo, Function p)->{
                transitions.add(p);
            });
        }
        return transitions;
    }

    public Set<Function> getTransitionStructure(MDP<Function> mdpParam, int s, int a) {
        HashSet<Function> transitions = new HashSet<>();
        mdpParam.forEachTransition(s, a, (int sFrom, int sTo, Function p)->{
            transitions.add(p);
        });
        return transitions;
    }


    public Map<Set<Function>, List<Integer>> getSimilarStateMap(MDP<Function> mdpParam) {
        HashMap<Set<Function>, List<Integer>> similarStateMap = new HashMap<>();

        for (int s = 0; s < mdpParam.getNumStates(); s++) {
            Set<Function> transitionStructure = getTransitionStructure(mdpParam, s);
            if (!similarStateMap.containsKey(transitionStructure)) {
                similarStateMap.put(transitionStructure, new ArrayList<>());
            }
            similarStateMap.get(transitionStructure).add(s);
        }
        System.out.println("Similar state map" + similarStateMap);
        return similarStateMap;
    }

    public Map<Set<Function>, List<Pair<Integer, Integer>>> getSimilarStateActionMap(MDP<Function> mdpParam) {
        HashMap<Set<Function>, List<Pair<Integer, Integer>>> similarStateActionMap = new HashMap<>();

        for (int s = 0; s < mdpParam.getNumStates(); s++) {
            int numChoices = mdpParam.getNumChoices(s);
            for (int i = 0 ; i < numChoices; i++) {
                Set<Function> transitionStructure = getTransitionStructure(mdpParam, s, i);
                if (!similarStateActionMap.containsKey(transitionStructure)) {
                    similarStateActionMap.put(transitionStructure, new ArrayList<>());
                }
                similarStateActionMap.get(transitionStructure).add(new Pair<>(s, i));
            }
        }
        System.out.println("Similar state action map" + similarStateActionMap);
        return similarStateActionMap;
    }

    public List<List<TransitionTriple>> getSimilarTransitions(MDP<Function> mdpParam) {
        Map<Set<Function>, List<Pair<Integer, Integer>>> similarStateMap = getSimilarStateActionMap(mdpParam);
        List<List<TransitionTriple>> similarTransitions = new ArrayList<>();

        for (List<Pair<Integer, Integer>> similarStateActions : similarStateMap.values()) {
            Map<Function, List<TransitionTriple>> transitions = new HashMap<>();

            for (Pair<Integer, Integer> sa : similarStateActions) {
                int s = sa.first;
                int i = sa.second;

                String action = getActionString(mdpParam, s, i);

                mdpParam.forEachTransition(s, i, (int sFrom, int sTo, Function p)->{
                    if (!transitions.containsKey(p)) {
                        transitions.put(p, new ArrayList<>());
                    }
                    transitions.get(p).add(new TransitionTriple(sFrom, action, sTo));
                });

            }

            similarTransitions.addAll(transitions.values());
        }
        System.out.println("Similar Transitions: " + similarTransitions);
        return similarTransitions;
    }

    public void compareSamplingStrategiesUncertain(String label, Experiment ex, EstimatorConstructor estimatorConstructor, List<Values> uncertainParameters)
    {
        resetAll();
        System.out.println("\n\n\n\n%------\n%Compare sampling strategies on\n%  Model: " + ex.model + "\n%  max_episode_length: "
                + ex.max_episode_length + "\n%  iterations: " + ex.iterations + "\n%  Prior strength: ["
                + ex.initLowerStrength + ", " + ex.initUpperStrength + "]\n%------");

        if (verbose)
            System.out.printf("%s, seed %d\n", label, ex.seed);

        List<Double> robustValues = new ArrayList<>();
        List<Double> trueValues = new ArrayList<>();

        try {
            ModulesFile modulesFile = prism.parseModelFile(new File(ex.modelFile));
            prism.loadPRISMModel(modulesFile);

            // Temporarily get parametric model
            String[] paramNames = new String[]{"r"};
            String[] paramLowerBounds = new String[]{};
            String[] paramUpperBounds = new String[]{};
            this.prism.setPRISMModelConstants(new Values(), true);
            this.prism.setParametric(paramNames, paramLowerBounds, paramUpperBounds);
            this.prism.buildModel();
            MDP<Function> mdpParam = (MDP<Function>) this.prism.getBuiltModelExplicit();

            System.out.println("MDP Param:" + mdpParam);

            List<List<TransitionTriple>> similarTransitions = getSimilarTransitions(mdpParam);

            Map<Function, List<TransitionTriple>> functionMap = getFunctionMap(mdpParam);
            for (Function f : functionMap.keySet()){
                System.out.println("function:" + f + functionMap.get(f));
            }

            // Instantiate parametric model
            Point paramValues = new Point(new BigRational[]{ BigRational.from(0.5) });
            MDP<Double> mdpInst = new MDPSimple<>(mdpParam, f -> f.evaluate(paramValues).doubleValue(), Evaluator.forDouble());
            System.out.println(mdpInst);

            // Then revert to original model
            this.prism.setParametricOff();



        for (Values values : uncertainParameters){
            ex.values = values;
            Estimator estimator = estimatorConstructor.get(this.prism, ex);
            estimator.setFunctionMap(functionMap);
            estimator.setSimilarTransitions(similarTransitions);
            estimator.set_experiment(ex);

            String directoryPath = makeOutputDirectory(ex);
            System.out.println("Exp opt: " + ex.optimizations + ex.model);
            String path = directoryPath + label + ".csv";
            System.out.println("Path: "+ path);
            if (Files.exists(Paths.get(path))) {
                System.out.printf("File %s already exists.%n", path);
                //return;
            }
            ex.dumpConfiguration(directoryPath, label, estimator.getName());

            // Iterate and run experiments for each of the sampled parameter vectors
            System.out.println("Constant Values:" + estimator.getSUL().getConstantValues());
            ArrayList<DataPoint> results = runSamplingStrategyDoublingEpoch(ex, estimator);

            robustValues.add(results.get(results.size() - 1).getEstimatedValue());
            trueValues.add(estimator.sulOpt);

            DataProcessor dp = new DataProcessor();
            dp.dumpRawData(directoryPath, label, results, ex);
        }

        } catch (PrismException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Results: " + robustValues);
        System.out.println("Robust Lambda:" + Collections.min(robustValues));
        System.out.println("True Results" + trueValues);
        System.out.println("True Lambda:" + Collections.min(trueValues));
//        DataProcessor dp = new DataProcessor();
//        //dp.dumpRawData(directoryPath, label, results, ex);
    }

    public ArrayList<DataPoint> runSamplingStrategyDoublingEpoch(Experiment ex, Estimator estimator) {
        return runSamplingStrategyDoublingEpoch(ex, estimator, 0);
    }

    public ArrayList<DataPoint> runSamplingStrategyDoublingEpoch(Experiment ex, Estimator estimator, int past_iterations) {
        try {
            MDP<Double> SUL = estimator.getSUL();

            if (true/*this.modelStats == null*/) {
                this.modelStats = estimator.getModelStats();
                System.out.println(this.modelStats);
            }

            ObservationSampler observationSampler = new ObservationSampler(this.prism, SUL, estimator.getTerminatingStates());
            observationSampler.setTransitionsOfInterest(estimator.getTransitionsOfInterest());
            observationSampler.setTiedParameters(ex.tieParameters);
            observationSampler.setMultiplier(ex.multiplier);

            double[] currentResults = estimator.getInitialResults();

            if (this.verbose) System.out.println("Episode " + past_iterations + ". " + "Performance " + currentResults[1]);

            ArrayList<DataPoint> results = new ArrayList<>();
            if (past_iterations == 0) {
                results.add(new DataPoint(0, past_iterations, currentResults));
            }
            int samples = 0;
            Strategy samplingStrategy = estimator.buildStrategy();
            for (int i = past_iterations; i < ex.iterations + past_iterations; i++) {
                int sampled = observationSampler.simulateEpisode(ex.max_episode_length, samplingStrategy);
                samples += sampled;
                //System.out.println("Samples Map:" + observationSampler.getSamplesMap());
                boolean last_iteration = i == ex.iterations + past_iterations - 1;
                if (observationSampler.collectedEnoughSamples() || last_iteration) {
                    if (this.verbose) System.out.println("Episode " + i + ". Recomputing sampling strategy.");
                    estimator.setObservationMaps(observationSampler.getSamplesMap(), observationSampler.getSampleSizeMap());
                    samplingStrategy = estimator.buildStrategy();
                    currentResults = estimator.getCurrentResults();
                    if (!ex.tieParameters) {
                        observationSampler.resetObservationSequence();
                    } else {
                        observationSampler.incrementAccumulatedSamples();
                    }
                    if (this.verbose) System.out.println("New performance " + currentResults[1]);
                    if (this.verbose) System.out.println("New robust guarantee " + currentResults[0]);
                    results.add(new DataPoint(samples, i+1, currentResults));
                }
            }
            System.out.println("Robust" + currentResults[0]);
            System.out.println("Opimistic" + currentResults[5]);
            if (this.verbose) {
                System.out.println("DONE");
            }

            return results;


        } catch (PrismException e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        }
        prism.closeDown();
        return null;
    }


}
