package lava;

import explicit.IMDP;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.MDPSimple;
import lava.Experiment.Model;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.distribution.BetaDistribution;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import param.BigRational;
import param.Function;
import param.Point;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

public class LearnVerifyParallel {

    private Prism prism;
    private String modelStats = null;


    private final int seed = 1650280571;
    private int samplingSeed;
    private boolean verbose = true;


    public LearnVerifyParallel() {

    }

    public LearnVerifyParallel(boolean verbose) {
        this.verbose = verbose;
    }

    public LearnVerifyParallel(int samplingSeed) {
        this.samplingSeed = samplingSeed;
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        if (false) {
            int seed;
            for (String s : args) {
                try {
                    seed = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    System.out.println("skipping invalid seed " + s);
                    continue;
                }
                System.out.println("running with seed " + s);
                LearnVerifyParallel l = new LearnVerifyParallel(seed);
                l.basic();
                l.switching_environment();
                l.gridStrengthEval();
                //l.evaluate_strength();
            }
        } else {
            System.out.println("running with default seed");
            LearnVerifyParallel l = new LearnVerifyParallel();
            l.basic();
//            l.switching_environment();
//            l.gridStrengthEval();
//            l.evaluate_strength();
        }
    }

    public List<Integer> get_seeds(int seed, int num) {
        ArrayList<Integer> seeds = new ArrayList<>();
        seeds.add(seed);
        Random r = new Random(seed);

        for (int i = 0; i < num - 1; i++) {
            seeds.add(r.nextInt(seed));
        }

        return seeds;
    }

    public void basic() {
        String id = "basic";

        int m = 10; // = 300;
        int n = 10; // = 200;

        //System.out.println("Running with seed: " + seed + " and sampling seed: " + samplingSeed);
        //run_basic_algorithms(new Experiment(Model.DRONE).config(100, 1_000_000, samplingSeed, true, true, m, n, 5).info(id));
        //run_basic_algorithms_pac(new Experiment(Model.SAV).config(50, 1_000_000, samplingSeed, true, false, 12, 8, 2).info(id));
        run_basic_algorithms_naive(new Experiment(Model.CHAIN_LARGE_TWO_ACTION).config(100, 1_000_000, samplingSeed, false, false, m, n, 3).info(id));
    }

    private void run_basic_algorithms(Experiment ex) {
        String postfix = "";//String.format("_seed_%d", ex.seed);
        //ex.setResultIterations(new ArrayList<>(Arrays.asList(1,2,3,4,5,6,7,8,9, 10,12, 15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80, 90, 100, 200, 300, 400, 500, 600, 700, 1000, 1200, 2000, 4000, 6000, 8000, 10000, 15000, 19000, 30000, 40000, 50000, 60000, 80000, 100000, 200000, 300000, 400000, 500000, 800000, 900000)));
        ex.setResultIterations(new ArrayList<>(Arrays.asList(1, 3, 5, 7, 9, 10, 15, 20, 30, 40, 50, 60, 80, 100, 300, 600, 700, 1000, 2000, 4000, 8000, 10000, 19000, 30000, 60000, 80000, 100000, 200000, 400000, 600000, 900000)));
        postfix += ex.tieParameters ? "_tied" : (ex.optimizations ? "_opt" : "_naive");

        //runRobustPolicyComparisonForVis("LUI_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(true).setMultiplier(2).setTieParamters(false).stratWeight(0.9), BayesianEstimatorOptimistic::new);
        runRobustPolicyComparisonForVis("PAC_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(false).setTieParamters(true).stratWeight(0.9), PACIntervalEstimatorOptimistic::new);
        //runRobustPolicyComparisonForVis("MAP_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(true).setTieParamters(false).stratWeight(0.9), MAPEstimator::new);
        //runRobustPolicyComparisonForVis("UCRL_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(true).setTieParamters(false).stratWeight(0.9), UCRL2IntervalEstimatorOptimistic::new);
    }

    private void run_basic_algorithms_pac(Experiment ex) {
        String postfix = "";//String.format("_seed_%d", ex.seed);
        ex.setResultIterations(new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80, 90, 100, 200, 300, 400, 500, 600, 700, 1000, 1200, 2000, 4000, 6000, 8000, 10000, 15000, 19000, 30000, 40000, 50000, 60000, 80000, 100000, 200000, 300000, 400000, 500000, 800000, 900000)));
        postfix += ex.tieParameters ? "_tied" : (ex.optimizations ? "_opt" : "_naive");
        if (!ex.optimizations) {
            runRobustPolicyComparisonForVis("LUI_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(true).stratWeight(0.85), BayesianEstimatorOptimistic::new);
        }
        runRobustPolicyComparisonForVis("PAC_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(false).stratWeight(0.85), PACIntervalEstimatorOptimistic::new);
    }

    private void run_basic_algorithms_naive(Experiment ex) {
        String postfix = "";//String.format("_seed_%d", ex.seed);
        ex.setResultIterations(new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80, 90, 100, 200, 300, 400, 500, 600, 700, 1000, 1200, 2000, 4000, 6000, 8000, 10000, 15000, 19000, 30000, 40000, 50000, 60000, 80000, 100000, 200000, 300000, 400000, 500000, 800000, 900000)));
        postfix += ex.tieParameters ? "_tied" : (ex.optimizations ? "_opt" : "_naive");
        //runRobustPolicyComparisonForVis("PAC_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(false).stratWeight(0.85).setMultiplier(3), PACIntervalEstimatorOptimistic::new);
        runRobustPolicyComparisonForVis("LUI_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(true).stratWeight(0.85).setMultiplier(3), BayesianEstimatorOptimistic::new);
        //runRobustPolicyComparisonForVis("MAP_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(false).stratWeight(0.85).setMultiplier(3), MAPEstimator::new);
        //runRobustPolicyComparisonForVis("UCRL_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(false).stratWeight(0.85).setMultiplier(3), UCRL2IntervalEstimatorOptimistic::new);
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
            String id = "switching-weight-" + x;
            List<Integer> switching_points = new ArrayList<>(List.of(100, 1000, 10000, 100000));
            for (Integer j : switching_points) {
                experiment_switching_environment(
                        new Experiment(Model.CHAIN_LARGE).config(100, j, seed).stratWeight(x).info(id),
                        new Experiment(Model.CHAIN_LARGE2).config(100, 1000000 - j, seed).stratWeight(x).info(id),
                        j
                );
            }
        }
        for (Double x : strategyWeights) {
            String id = "switching-weight-" + x;
            List<Integer> switching_points = new ArrayList<>(List.of(100, 1000, 10000, 100000));
            for (Integer j : switching_points) {
                experiment_switching_environment(
                        new Experiment(Model.BETTING_GAME_FAVOURABLE).config(7, j, seed).stratWeight(x).info(id),
                        new Experiment(Model.BETTING_GAME_UNFAVOURABLE).config(7, 1000000 - j, seed).stratWeight(x).info(id),
                        j
                );
            }
        }
    }

    private void experiment_switching_environment(Experiment ex1, Experiment ex2, int switching_point) {
//        String postfix = String.format("_switching_point_%d_seed_%d", switching_point, seed);
//        compareSamplingStrategies("UCRL2_unbounded" + postfix, ex1, UCRL2IntervalEstimatorOptimistic::new, ex2);
//        compareSamplingStrategies("MAP_unbounded" + postfix, ex1, MAPEstimator::new, ex2);
//        compareSamplingStrategies("LUI_unbounded" + postfix, ex1, BayesianEstimatorOptimistic::new, ex2);
//
//        ex1 = ex1.setStrengthBounds(200, 300, 300);
//        ex2 = ex2.setStrengthBounds(200, 300, 300);
//        compareSamplingStrategies("UCRL2_highbound" + postfix, ex1, UCRL2IntervalEstimatorOptimistic::new, ex2);
//        compareSamplingStrategies("MAP_highbound" + postfix, ex1, MAPEstimator::new, ex2);
//        compareSamplingStrategies("LUI_highbound" + postfix, ex1, BayesianEstimatorOptimistic::new, ex2);
//
//        ex1 = ex1.setStrengthBounds(20, 30, 30);
//        ex2 = ex2.setStrengthBounds(20, 30, 30);
//        compareSamplingStrategies("UCRL2_lowbound" + postfix, ex1, UCRL2IntervalEstimatorOptimistic::new, ex2);
//        compareSamplingStrategies("MAP_lowbound" + postfix, ex1, MAPEstimator::new, ex2);
//        compareSamplingStrategies("LUI_lowbound" + postfix, ex1, BayesianEstimatorOptimistic::new, ex2);

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

    public void resetAll(int seed) {
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
        String outputPath = String.format("results/%s/%s/Robust_Policies_WCC/%s/", ex.experimentInfo, ex.model.toString(), ex.seed);
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

    public void runRobustPolicyComparison(String label, Experiment ex, EstimatorConstructor estimatorConstructor) {
        try {
            // Build pMDP model
            resetAll(ex.seed);
            MDP<Function> mdpParam = buildParamModel(ex);

            // Generate sample training and verification MDP parameters
            double rangeMin1 = 0.75;
            double rangeMax1 = 0.95;
            List<Values> trainingParams = new ArrayList<>();
            List<Values> verificationParams = new ArrayList<>();
            //double rangeMean = rangeMin + (rangeMax - rangeMin) / 2;
            Random r = new Random(ex.seed);
            for (int i = 0; i < ex.numTrainingMDPs; i++) {
                constructValues(rangeMin1, rangeMax1, trainingParams, r);
            }
            for (int i = 0; i < ex.numVerificationMDPs; i++) {
                constructValues(rangeMin1, rangeMax1, verificationParams, r);
            }

            // Get MDPs/IMDPs for training and verification set
            Pair<List<IMDP<Double>>, List<MDP<Double>>> trainingSet = getIMDPs(label, ex, estimatorConstructor, mdpParam, trainingParams, false);
            Pair<List<IMDP<Double>>, List<MDP<Double>>> verificationSet = getIMDPs(label, ex, PACIntervalEstimatorOptimistic::new, mdpParam, verificationParams, true);

            // Build robust policy and derive PAC guarantee
            RobustPolicySynthesizerIMDP robSynthI = new RobustPolicySynthesizerIMDP(mdpParam, ex);
            RobustPolicySynthesizerMDP robSynth = new RobustPolicySynthesizerMDP(mdpParam, ex);

            // Construction and analysis over learned IMDPs
            robSynthI.addIMDPs(trainingSet.first);
            robSynthI.addVerificatonIMDPs(verificationSet.first);
            robSynthI.combineIMDPs();
            MDStrategy<Double> robstratI = robSynthI.getRobustStrategy(prism, ex.robustSpec);
            List<Double> robResultsI = robSynthI.checkVerificationSet(prism, robstratI, ex.idtmcRobustSpec);

            // Construction and analysis over true MDPs
            robSynth.addMDPs(trainingSet.second);
            robSynth.addVerificatonMDPs(verificationSet.second);
            robSynth.combineMDPs();
            MDStrategy<Double> robstrat = robSynth.getRobustStrategy(prism, ex.robustSpec);
            List<Double> robResults = robSynth.checkVerificationSet(prism, robstrat, ex.dtmcSpec);

            // Analyse robust policy obtained over IMDPs on the true MDPs
            List<Double> robResultsCross = robSynth.checkVerificationSet(prism, robstratI, ex.dtmcSpec);

            System.out.println("Verification Results with robust strategy (on IMDPs):" + robResultsI);
            System.out.println("IMDP Robust Guarantee: " + Collections.min(robResultsI));

            System.out.println("Verification Results with robust strategy (on true MDPs):" + robResults);
            System.out.println("True MDP Robust Guarantee: " + Collections.min(robResults));

            System.out.println("Verification Results with robust strategy from IMDPs on true MDPs:" + robResultsCross);
            System.out.println("True MDP Robust Guarantee with strategy from IMDPs: " + Collections.min(robResultsCross));
        } catch (PrismException e) {
            throw new RuntimeException(e);
        }
    }

    public void runRobustPolicyComparisonForVis(String label, Experiment ex, EstimatorConstructor estimatorConstructor) {
        try {
            System.out.println("Result iterations:" + ex.getResultIterations());
            System.out.println("Label: " + label);
            // Build pMDP model
            resetAll(ex.seed);
            MDP<Function> mdpParam = buildParamModel(ex);


//            // Generate uniform sample training and verification MDP parameters
//            /*
//             * Range for SAV2: [0.75, 0.95]
//             * Range for Aircraft: [0.7, 0.9]
//             */
//            double rangeMin1 = 0.75;
//            double rangeMax1 = 0.95;
//            List<Values> trainingParams = new ArrayList<>();
//            List<Values> verificationParams = new ArrayList<>();
//
//            //double rangeMean = rangeMin + (rangeMax - rangeMin) / 2;
//            Random r = new Random(seed);
//            for (int i = 0; i < ex.numTrainingMDPs; i++) {
//                constructValues(rangeMin1, rangeMax1, trainingParams, r);
//            }
//            for (int i = 0; i < ex.numVerificationMDPs; i++) {
//                constructValues(rangeMin1, rangeMax1, verificationParams, r);
//            }
//
//            for (Values value : trainingParams) {
//                System.out.println(value.getValues() + ",");
//            }
//            for (Values value : verificationParams) {
//                System.out.println(value.getValues() + ",");
//            }

            // Generate beta-distributed sample training and verification MDP parameters
            /*
             * Parameters for Aircraft: Alpha = 10, Beta = 2
             * Parameters for Drone Single: Alpha = 2, Beta = 10
             * Parameters for Betting Game: Alpha = 20, Beta = 2
             */
            int alpha = 2;
            int beta = 10;
            BetaDistribution betaDist = BetaDistribution.of(alpha, beta);
            ContinuousDistribution.Sampler sampler = betaDist.createSampler(RandomSource.JDK.create(seed));
            Iterator<Double> it = sampler.samples().iterator();

            List<Values> trainingParams = new ArrayList<>();
            List<Values> verificationParams = new ArrayList<>();

            for (int i = 0; i < ex.numTrainingMDPs; i++) {
                constructValuesBeta(trainingParams, it);
            }
            for (int i = 0; i < ex.numVerificationMDPs; i++) {
                constructValuesBeta(verificationParams, it);
            }

            Values v = new Values();
            v.addValue("fast", 0.109280077836);
            verificationParams.add(v);

            System.out.println("Training Parameters:");
            for (Values value : trainingParams) {
                System.out.println(value.getValues() + ",");
            }
            System.out.println("Verification Parameters:");
            for (Values value : verificationParams) {
                System.out.println(value.getValues() + ",");
            }


            long startTime = System.currentTimeMillis();

            // Get MDPs/IMDPs for training
            Pair<List<List<IMDP<Double>>>, List<MDP<Double>>> trainingSet = getIMDPsForVis(label, ex, estimatorConstructor, mdpParam, trainingParams, false);

            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            double elapsedTimeinSecondsTraining = elapsedTime / 1000.0;

            startTime = System.currentTimeMillis();

            // Get MDPs/IMDPs for verification
            Pair<List<List<IMDP<Double>>>, List<MDP<Double>>> verificationSet = getIMDPsForVis(label, ex, PACIntervalEstimator::new, mdpParam, verificationParams, true);

            endTime = System.currentTimeMillis();
            elapsedTime = endTime - startTime;
            double elapsedTimeinSecondsVerification = elapsedTime / 1000.0;

            // Build robust policy and derive PAC guarantee
            RobustPolicySynthesizerIMDP robSynthI = new RobustPolicySynthesizerIMDP(mdpParam, ex);
            RobustPolicySynthesizerMDP robSynth = new RobustPolicySynthesizerMDP(mdpParam, ex);

            // Construction and analysis over true MDPs for result iteration i
            robSynth.setMDPs(trainingSet.second);
            robSynth.setVerificationSet(verificationSet.second);
            robSynth.combineMDPs();
            MDStrategy<Double> robstrat = robSynth.getRobustStrategy(prism, ex.robustSpec);

            List<Double> robResults = robSynth.checkVerificationSet(prism, robstrat, ex.dtmcSpec);

            // Get Existential Guarantee over true MDPs
            List<Double> existentialLambdas = getExistentialGuarantee(prism, ex, verificationSet.second);

            ArrayList<DataPointRobust> results = new ArrayList<>();
            ArrayList<Integer> plottedIterations = new ArrayList<>(ex.getResultIterations());
            plottedIterations.add(0, 0);
            plottedIterations.add(ex.iterations - 1);

            for (int i = 0; i < trainingSet.first.get(0).size(); i++) {
                List<IMDP<Double>> subTrainSet = new ArrayList<>();
                List<IMDP<Double>> subVerificationSet = new ArrayList<>();
                for (List<IMDP<Double>> iterationList : trainingSet.first) {
                    subTrainSet.add(iterationList.get(i));
                }
                for (List<IMDP<Double>> iterationList : verificationSet.first) {
                    subVerificationSet.add(iterationList.get(i));
                }
                try {
                    System.out.println("Result iteration: " + ex.getResultIterations().get(i - 1));
                } catch (Exception ignored) {

                }
                // Construction and analysis over learned IMDPs for result iteration i
                robSynthI.setImdps(subTrainSet);
                robSynthI.setVerificationSet(subVerificationSet);
                robSynthI.combineIMDPs();

                MDStrategy<Double> robstratI = robSynthI.getRobustStrategy(prism, ex.robustSpec);

                List<Double> robResultsI = robSynthI.checkVerificationSet(prism, robstratI, ex.idtmcRobustSpec);
                List<Double> robResultsIRL = robSynthI.checkVerificatonSetRLPolicy(prism, ex.idtmcRobustSpec, plottedIterations.get(i));

                // Analyse robust policy obtained over IMDPs on the true MDPs
                List<Double> robResultsCross = robSynth.checkVerificationSet(prism, robstratI, ex.dtmcSpec);
                List<Double> robResultsCrossRL = robSynth.checkVerificatonSetRLPolicy(prism, ex.dtmcSpec, plottedIterations.get(i));

                System.out.println("\n" + "==============================");
                try {
                    System.out.println("Results after #Simulations: " + ex.getResultIterations().get(i - 1));
                } catch (Exception ignored) {

                }
                System.out.println("Verification Results with IMDP strategy on IMDPs:" + robResultsI);
                System.out.println("IMDP Robust Guarantee with IMDP strategy: " + Collections.min(robResultsI));

                System.out.println("Verification Results with RL strategy on IMDPs:" + robResultsIRL);
                System.out.println("IMDP Robust Guarantee with RL strategy: " + Collections.min(robResultsIRL));

                System.out.println("Verification Results with MDP strategy on true MDPs:" + robResults);
                System.out.println("True MDP Robust Guarantee with true MDP strategy: " + Collections.min(robResults));

                System.out.println("Verification Results with IMDP strategy on true MDPs:" + robResultsCross);
                System.out.println("True MDP Robust Guarantee with IMDP strategy: " + Collections.min(robResultsCross));

                System.out.println("Verification Results with RL strategy on true MDPs:" + robResultsCrossRL);
                System.out.println("True MDP Robust Guarantee with RL strategy: " + Collections.min(robResultsCrossRL));

                System.out.println("Existential Results on true MDPs:" + existentialLambdas);
                System.out.println("Existential Guarantee (Badings et al.): " + Collections.min(existentialLambdas));

                results.add(new DataPointRobust(plottedIterations.get(i), new double[]{Collections.min(robResultsI), Collections.min(robResults), Collections.min(robResultsCross), Collections.min(existentialLambdas), Collections.min(robResultsIRL), Collections.min(robResultsCrossRL)}));

                List<Double> evalResIMDP = List.of();
                List<Double> evalResRL = List.of();
                // Eval robust policy on fresh samples
                if (i == trainingSet.first.getFirst().size() - 1) {
                    System.out.println("\n" + "==============================");

                    // Evaluation on 200 fresh samples for RL and IMDP policy
                    evalResIMDP = List.of(0.0); //evaluatePolicy(robstratI, 1000, ex, false, plottedIterations.get(i));

                    PolicyLoader pol = new PolicyLoader();
                    MRStrategy rlStrat = pol.loadFirewirePolicy(String.format("policies/firewire/firewire_policies/policy_single_%d.json", (0)), robSynth.getVerificationSet().getFirst());
                    evalResRL = evaluatePolicy(null, 600, ex, true, plottedIterations.get(i));

                    // Empirical Risk:
                    //computeEmpiricalRisk(robstratI,Collections.min(robResultsI), 200, ex);
                    //computeEmpiricalRisk(robstratI,0.6551633157021766, 200 , ex);
                }

                DataProcessor dp = new DataProcessor();
                dp.dumpDataRobustPolicies(makeOutputDirectory(ex), label, results);
                dp.dumpResultList(makeOutputDirectory(ex), label, robResultsCross, existentialLambdas, evalResIMDP, evalResRL, elapsedTimeinSecondsTraining, elapsedTimeinSecondsVerification);
            }

            ex.dumpConfiguration(makeOutputDirectory(ex), label);

        } catch (PrismException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Double> evaluatePolicy(MDStrategy<Double> strat, int numEvalSamples, Experiment ex, boolean isRL, int iteration) throws PrismException {

        int alpha = 5;
        int beta = 5;
        BetaDistribution betaDist = BetaDistribution.of(alpha, beta);
        ContinuousDistribution.Sampler sampler = betaDist.createSampler(RandomSource.JDK.create(seed));
        Iterator<Double> it = sampler.samples().iterator();

        List<Values> evaluationParams = new ArrayList<>();

        for (int i = 0; i < numEvalSamples; i++) {
            constructValuesBeta(evaluationParams, it);
        }

        System.out.println("eval params:" + evaluationParams);

        List<Double> robResultsCross = new ArrayList<>();

        RobustPolicySynthesizerMDP robSynth = new RobustPolicySynthesizerMDP(null, ex);

        for (Values values : evaluationParams) {
            ex.values = values;
            EstimatorConstructor estimatorConstructor = PACIntervalEstimator::new;
            Estimator estimator = estimatorConstructor.get(this.prism, ex);
            estimator.set_experiment_naive(ex);

            // Iterate and run experiments for each of the sampled parameter vectors
            robSynth.setVerificationSet(List.of(estimator.getSUL()));
            double res;
            if (!isRL) {
                res = robSynth.checkVerificationSet(prism, strat, ex.dtmcSpec).getFirst();
            } else {
                res = robSynth.checkVerificatonSetRLPolicy(prism, ex.dtmcSpec, 0).getFirst();
            }
            robResultsCross.add(res);
            System.out.println("Solved MDP:" + values + "Res: " + res);
        }

        List<Double> existentialLambdas = List.of(0.0);//getExistentialGuarantee(prism, ex, evalMDPs);
        System.out.println("Eval Existential Lambdas: " + existentialLambdas);


//        List<Double> robResultsCross = !isRL ?
//             robSynth.checkVerificationSet(prism, strat, ex.dtmcSpec)
//        :
//             robSynth.checkVerificatonSetRLPolicy(prism, ex.dtmcSpec, iteration);

        double minres = 1.0;
        int ind = 0;
        for (int i = 0; i < 100; i++) {
            if (robResultsCross.get(i) < minres) {
                minres = robResultsCross.get(i);
                ind = i;
            }
        }
        System.out.println("min res:" + minres);
        System.out.println("min param" + evaluationParams.get(ind));
        System.out.println("Eval Parameters: " + evaluationParams.subList(0, 100));
        System.out.println("Eval Results with robust strategy: " + robResultsCross.subList(0, 100));

        return robResultsCross;
    }

    private void computeEmpiricalRisk(MDStrategy<Double> strat, double guarantee, int numEvalSamples, Experiment ex) throws PrismException {
        int alpha = 2;
        int beta = 20;
        BetaDistribution betaDist = BetaDistribution.of(alpha, beta);
        ContinuousDistribution.Sampler sampler = betaDist.createSampler(RandomSource.JDK.create(seed));
        Iterator<Double> it = sampler.samples().iterator();

        List<Values> evaluationParams = new ArrayList<>();

        for (int i = 0; i < numEvalSamples; i++) {
            constructValuesBeta(evaluationParams, it);
        }

        List<MDP<Double>> evalMDPs = new ArrayList<>();
        for (Values values : evaluationParams) {
            ex.values = values;
            EstimatorConstructor estimatorConstructor = PACIntervalEstimator::new;
            Estimator estimator = estimatorConstructor.get(this.prism, ex);
            estimator.set_experiment_naive(ex);

            // Iterate and run experiments for each of the sampled parameter vectors
            evalMDPs.add(estimator.getSUL());
        }

        RobustPolicySynthesizerMDP robSynth = new RobustPolicySynthesizerMDP(null, ex);
        robSynth.setVerificationSet(evalMDPs);
        List<Double> robResultsCross = robSynth.checkVerificationSet(prism, strat, ex.dtmcSpec);
        System.out.println("Results for Empirical Eval:" + robResultsCross);
        int numFail = 0;
        for (double res : robResultsCross) {
            if (res < guarantee) {
                numFail++;
            }
        }
        int N = robResultsCross.size();
        double empiricalRisk = (double) numFail / (double) N;
        System.out.println("Empirical Risk: " + (empiricalRisk) + " for N = " + N + " and guarantee " + guarantee);
    }

    private void constructValues(double rangeMin1, double rangeMax1, List<Values> params, Random r) {
        Values v = new Values();
        double pL = rangeMin1 + (rangeMax1 - rangeMin1) * r.nextDouble(); //r.nextGaussian();
        double pH = rangeMin1 + (rangeMax1 - rangeMin1) * r.nextDouble(); //r.nextGaussian();
        /*
         * SAV2: pL -> pL, pH -> pH
         * Aircraft r -> pL, p -> 1 - pH
         */
        v.addValue("pL", pL);
        v.addValue("pH", pH);
        params.add(v);
    }

    private void constructValuesBeta(List<Values> params, Iterator<Double> it) {
        Values v = new Values();
        //double pL = it.next();
        double pH = it.next();

//        while (1-pH >= 0.5){
//            pH = it.next();
//        }

        // For Chain Benchmark, only p's and q'
        /*
         * SAV2: pL -> pL, pH -> pH
         * Aircraft r -> pL, p -> 1 - pH
         * Drone Single p -> min(pH, 0.32), and pL commented out
         * Betting Game p -> pH, no commenting out
         * Chain Benchmark p -> ph  q -> 1 - p, no commenting out
         */

        //v.addValue("pL", pL);
        v.addValue("p", Math.min(pH, 0.32));
        params.add(v);
    }

    public List<Double> getExistentialGuarantee(Prism prism, Experiment ex, List<MDP<Double>> mdps) {
        try {
            ArrayList<Double> results = new ArrayList<>();
            MDPModelChecker mc = new MDPModelChecker(prism);
            //mc.setErrorOnNonConverge(false);

            ModulesFile modulesFileMDP = prism.parseModelFile(new File(ex.modelFile), ModelType.MDP);
            ModulesFileModelGenerator<?> modelGen = ModulesFileModelGenerator.create(modulesFileMDP, this.prism);


            PropertiesFile pf = prism.parsePropertiesString(ex.spec);
            Expression exprTarget = pf.getProperty(0);


            for (MDP<Double> mdp : mdps) {
                modelGen.setSomeUndefinedConstants(mdp.getConstantValues());
                mc.setModelCheckingInfo(modelGen, pf, modelGen);
                Result result = mc.check(mdp, exprTarget);
                results.add((double) result.getResult());
            }
            return results;
        } catch (PrismException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void compareSamplingStrategies(String label, Experiment ex, EstimatorConstructor estimatorConstructor) {

        List<Values> values = new ArrayList<>();

        double rangeMin1 = 0.75;
        double rangeMax1 = 0.95;

        //double rangeMean = rangeMin + (rangeMax - rangeMin) / 2;
        Random r = new Random(ex.seed);

        for (int i = 0; i < 10; i++) {
            constructValues(rangeMin1, rangeMax1, values, r);
        }
        resetAll(ex.seed);
        // MDP<Function> mdpParam = buildParamModel(ex);
        compareSamplingStrategiesUncertain(label, ex, estimatorConstructor, values);
//        Pair<List<IMDP<Double>>, List<MDP<Double>>> result = getIMDPs(label, ex, estimatorConstructor, mdpParam, values);
//        System.out.println("IMDPs: " + result.first);
//        System.out.println("MDPs: " + result.second);
    }

    public MDP<Function> buildParamModel(Experiment ex) {
        try {
            ModulesFile modulesFile = this.prism.parseModelFile(new File(ex.modelFile));
            prism.loadPRISMModel(modulesFile);

            // Temporarily get parametric model
            /*
             * SAV2: pL, pH
             * Aircraft: r, p
             * Drone Single: p
             * Betting Game: p
             * Chain Large: p, q
             */
            String[] paramNames = new String[]{"pL", "pH", "p"}; //, "q","r"};
            String[] paramLowerBounds = new String[]{"0", "0", "0"};
            String[] paramUpperBounds = new String[]{"1", "1", "1"};
            this.prism.setPRISMModelConstants(new Values(), true);
            this.prism.setParametric(paramNames, paramLowerBounds, paramUpperBounds);
            this.prism.buildModel();
            MDP<Function> model = (MDP<Function>) this.prism.getBuiltModelExplicit();
//            System.out.println("Model states values" + model.getStatesList().getFirst());
//            System.out.println("Action 0:" + model.getAction(0,0));
//            System.out.println("Action 1:" + model.getAction(0,1));
//            System.out.println("Action 2:" + model.getAction(0,2));

            return (MDP<Function>) this.prism.getBuiltModelExplicit();

        } catch (PrismException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Pair<List<IMDP<Double>>, List<MDP<Double>>> getIMDPs(String label, Experiment ex, EstimatorConstructor estimatorConstructor, MDP<Function> mdpParam, List<Values> uncertainParameters, boolean verification) {
        resetAll(ex.seed);
        System.out.println("\n\n\n\n%------\n%Compare sampling strategies on\n%  Model: " + ex.model + "\n%  max_episode_length: "
                + ex.max_episode_length + "\n%  iterations: " + ex.iterations + "\n%  Prior strength: ["
                + ex.initLowerStrength + ", " + ex.initUpperStrength + "]\n%------");

        if (verbose)
            System.out.printf("%s, seed %d\n", label, ex.seed);

        ArrayList<IMDP<Double>> learnedIMDPs = new ArrayList<>();
        ArrayList<MDP<Double>> mdps = new ArrayList<>();

        try {
            ModulesFile modulesFile = prism.parseModelFile(new File(ex.modelFile));
            prism.loadPRISMModel(modulesFile);
            List<List<TransitionTriple>> similarTransitions = getSimilarTransitions(mdpParam);
            Map<Function, List<TransitionTriple>> functionMap = getFunctionMap(mdpParam);
            for (Function f : functionMap.keySet()) {
                System.out.println("function:" + f + functionMap.get(f));
            }

            for (Values values : uncertainParameters) {
                ex.values = values;
                Estimator estimator = estimatorConstructor.get(this.prism, ex);

                estimator.setFunctionMap(functionMap);
                estimator.setSimilarTransitions(similarTransitions);
                estimator.set_experiment(ex);

                // Iterate and run experiments for each of the sampled parameter vectors
                Pair<ArrayList<DataPoint>, ArrayList<IMDP<Double>>> resIMDP = runSamplingStrategyDoublingEpoch(ex, estimator, verification);
                learnedIMDPs.add(resIMDP.second.get(resIMDP.second.size() - 1));
                mdps.add(estimator.getSUL());
            }

        } catch (PrismException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return new Pair<>(learnedIMDPs, mdps);
    }

    public Pair<List<List<IMDP<Double>>>, List<MDP<Double>>> getIMDPsForVis(String label, Experiment ex, EstimatorConstructor estimatorConstructor, MDP<Function> mdpParam, List<Values> uncertainParameters, boolean verification) {
        resetAll(ex.seed);
        System.out.println("\n\n\n\n%------\n%Compare sampling strategies on\n%  Model: " + ex.model + "\n%  max_episode_length: "
                + ex.max_episode_length + "\n%  iterations: " + ex.iterations + "\n%  Prior strength: ["
                + ex.initLowerStrength + ", " + ex.initUpperStrength + "]\n%------");

        if (verbose)
            System.out.printf("%s, seed %d\n", label, ex.seed);

        ArrayList<List<IMDP<Double>>> learnedIMDPs = new ArrayList<>();
        ArrayList<MDP<Double>> mdps = new ArrayList<>();

        try {
            ModulesFile modulesFile = prism.parseModelFile(new File(ex.modelFile));
            prism.loadPRISMModel(modulesFile);
            List<List<TransitionTriple>> similarTransitions = getSimilarTransitions(mdpParam);
            Map<Function, List<TransitionTriple>> functionMap = getFunctionMap(mdpParam);
//            for (Function f : functionMap.keySet()) {
//                //System.out.println("function:" + f + functionMap.get(f));
//            }

            for (Values values : uncertainParameters) {
                ex.values = values;
                Estimator estimator = estimatorConstructor.get(this.prism, ex);
                System.out.println("Constant Values:" + estimator.getSUL().getConstantValues());
                estimator.setFunctionMap(functionMap);
                estimator.setSimilarTransitions(similarTransitions);
                estimator.set_experiment(ex);

                // Iterate and run experiments for each of the sampled parameter vectors
                ex.setTieParamters(verification);
                Pair<ArrayList<DataPoint>, ArrayList<IMDP<Double>>> resIMDP = runSamplingStrategyDoublingEpoch(ex, estimator, verification);
                learnedIMDPs.add(resIMDP.second);
                mdps.add(estimator.getSUL());
            }

        } catch (PrismException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return new Pair<>(learnedIMDPs, mdps);
    }

//    public void compareSamplingStrategies(String label, Experiment ex, EstimatorConstructor estimatorConstructor, Experiment follow_up_ex) {
//        resetAll();
//        System.out.println("\n\n\n\n%------\n%Compare sampling strategies on\n%  Model: " + ex.model + "\n%  max_episode_length: "
//                + ex.max_episode_length + "\n%  iterations: " + ex.iterations + "\n%  Prior strength: ["
//                + ex.initLowerStrength + ", " + ex.initUpperStrength + "]\n%------");
//
//        if (verbose)
//            System.out.printf("%s, seed %d\n", label, ex.seed);
//
//        Estimator estimator = estimatorConstructor.get(this.prism, ex);
//        String directoryPath = makeOutputDirectory(ex);
//
//        String path = directoryPath + label +".csv";
//        if (Files.exists(Paths.get(path))) {
//            System.out.printf("File %s already exists.%n", path);
//            //return;
//        }
//
//        ex.dumpConfiguration(directoryPath, label, estimator.getName());
//
//        ArrayList<DataPoint> results = runSamplingStrategyDoublingEpoch(ex, estimator);
//        if (follow_up_ex != null) {
//            estimator.set_experiment(follow_up_ex);
//            results.addAll( runSamplingStrategyDoublingEpoch(follow_up_ex, estimator, ex.iterations));
//            follow_up_ex.dumpConfiguration(directoryPath, label + "_part_2", estimator.getName());
//        }
//
//        DataProcessor dp = new DataProcessor();
//        dp.dumpRawData(directoryPath, label, results, ex);
//    }

    public String getActionString(MDP<Function> mdp, int s, int i) {
        String action = (String) mdp.getAction(s, i);
        if (action == null) {
            action = "_empty";
        }
        return action;
    }

    public Map<Function, List<TransitionTriple>> getFunctionMap(MDP<Function> mdpParam) {
        Map<Function, List<TransitionTriple>> functionMap = new HashMap<>();

        for (int s = 0; s < mdpParam.getNumStates(); s++) {
            int numChoices = mdpParam.getNumChoices(s);
            for (int i = 0; i < numChoices; i++) {
                //String action = (String) mdpParam.getAction(s, i);
                String action = getActionString(mdpParam, s, i);
                mdpParam.forEachTransition(s, i, (int sFrom, int sTo, Function p) -> {
                    if (functionMap.containsKey(p)) {
                        functionMap.get(p).add(new TransitionTriple(sFrom, action, sTo));
                    } else {
                        functionMap.put(p, new ArrayList<>());
                        functionMap.get(p).add(new TransitionTriple(sFrom, action, sTo));
                    }
                });
            }
        }

        return functionMap;
    }

    public Set<Function> getTransitionStructure(MDP<Function> mdpParam, int s) {
        HashSet<Function> transitions = new HashSet<>();
        int numChoices = mdpParam.getNumChoices(s);
        for (int i = 0; i < numChoices; i++) {
            mdpParam.forEachTransition(s, i, (int sFrom, int sTo, Function p) -> {
                transitions.add(p);
            });
        }
        return transitions;
    }

    public Set<Function> getTransitionStructure(MDP<Function> mdpParam, int s, int a) {
        HashSet<Function> transitions = new HashSet<>();
        mdpParam.forEachTransition(s, a, (int sFrom, int sTo, Function p) -> {
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
            for (int i = 0; i < numChoices; i++) {
                Set<Function> transitionStructure = getTransitionStructure(mdpParam, s, i);
                if (!similarStateActionMap.containsKey(transitionStructure)) {
                    similarStateActionMap.put(transitionStructure, new ArrayList<>());
                }
                similarStateActionMap.get(transitionStructure).add(new Pair<>(s, i));
            }
        }
        //System.out.println("paramstatelsit" + mdpParam.getStatesList());

        //System.out.println("Similar state action map" + similarStateActionMap);
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

                mdpParam.forEachTransition(s, i, (int sFrom, int sTo, Function p) -> {
                    if (!transitions.containsKey(p)) {
                        transitions.put(p, new ArrayList<>());
                    }
                    transitions.get(p).add(new TransitionTriple(sFrom, action, sTo));
                });

            }

            similarTransitions.addAll(transitions.values());
        }
        //System.out.println("Similar Transitions: " + similarTransitions);
        return similarTransitions;
    }

    public void compareSamplingStrategiesUncertain(String label, Experiment ex, EstimatorConstructor estimatorConstructor, List<Values> uncertainParameters) {
        resetAll(ex.seed);
        System.out.println("\n\n\n\n%------\n%Compare sampling strategies on\n%  Model: " + ex.model + "\n%  max_episode_length: "
                + ex.max_episode_length + "\n%  iterations: " + ex.iterations + "\n%  Prior strength: ["
                + ex.initLowerStrength + ", " + ex.initUpperStrength + "]\n%------");

        if (verbose)
            System.out.printf("%s, seed %d\n", label, ex.seed);

        List<Double> robustValues = new ArrayList<>();
        List<Double> trueValues = new ArrayList<>();
        ArrayList<IMDP<Double>> learnedIMDPs = new ArrayList<>();

        try {
            ModulesFile modulesFile = prism.parseModelFile(new File(ex.modelFile));
            prism.loadPRISMModel(modulesFile);

            // Temporarily get parametric model
            String[] paramNames = new String[]{"pL", "pH"};
            String[] paramLowerBounds = new String[]{"0", "1"};
            String[] paramUpperBounds = new String[]{"0", "1"};
            this.prism.setPRISMModelConstants(new Values(), true);
            this.prism.setParametric(paramNames, paramLowerBounds, paramUpperBounds);
            this.prism.buildModel();
            MDP<Function> mdpParam = (MDP<Function>) this.prism.getBuiltModelExplicit();

            RobustPolicySynthesizerIMDP rsynth = new RobustPolicySynthesizerIMDP(mdpParam, ex);

            System.out.println("MDP Param:" + mdpParam);

            List<List<TransitionTriple>> similarTransitions = getSimilarTransitions(mdpParam);

            Map<Function, List<TransitionTriple>> functionMap = getFunctionMap(mdpParam);
            for (Function f : functionMap.keySet()) {
                System.out.println("function:" + f + functionMap.get(f));
            }

            // Instantiate parametric model
            Point paramValues = new Point(new BigRational[]{BigRational.from(0.5), BigRational.from(0.5)});
            MDP<Double> mdpInst = new MDPSimple<>(mdpParam, f -> f.evaluate(paramValues).doubleValue(), Evaluator.forDouble());
            System.out.println(mdpInst);

            // Then revert to original model
            this.prism.setParametricOff();

            for (Values values : uncertainParameters) {
                ex.values = values;
                Estimator estimator = estimatorConstructor.get(this.prism, ex);

                estimator.setFunctionMap(functionMap);
                estimator.setSimilarTransitions(similarTransitions);
                estimator.set_experiment(ex);

                String directoryPath = makeOutputDirectory(ex);
                System.out.println("Exp opt: " + ex.optimizations + ex.model);
                String path = directoryPath + label + ".csv";
                System.out.println("Path: " + path);
                if (Files.exists(Paths.get(path))) {
                    System.out.printf("File %s already exists.%n", path);
                    //return;
                }
                ex.dumpConfiguration(directoryPath, label);

                // Iterate and run experiments for each of the sampled parameter vectors
                System.out.println("Constant Values:" + estimator.getSUL().getConstantValues());
                Pair<ArrayList<DataPoint>, ArrayList<IMDP<Double>>> resIMDP = runSamplingStrategyDoublingEpoch(ex, estimator, false);
                learnedIMDPs.add(resIMDP.second.get(resIMDP.second.size() - 1));
                ArrayList<DataPoint> results = resIMDP.first;

                robustValues.add(results.get(results.size() - 1).getEstimatedValue());
                trueValues.add(estimator.sulOpt);

                //DataProcessor dp = new DataProcessor();
                //dp.dumpRawData(directoryPath, label, results, ex);
            }

            // Robust Policy Synthesis
            List<List<IMDP<Double>>> imdps = new ArrayList<>();
            imdps = partition(learnedIMDPs, (int) Math.round(learnedIMDPs.size() * 0.6));
            rsynth.addIMDPs(imdps.get(0));
            IMDP<Double> combinedIMDP = rsynth.combineIMDPs();
            rsynth.addVerificatonIMDPs(imdps.get(1));
            MDStrategy<Double> robstrat = rsynth.getRobustStrategy(prism, ex.robustSpec);
            List<Double> robResults = rsynth.checkVerificationSet(prism, robstrat, ex.idtmcRobustSpec);
            System.out.println("Verification Results with robust strategy (on IMDPs):" + robResults);
            System.out.println("IMDP Robust Guarantee: " + Collections.min(robResults));

            ArrayList<Double> results = new ArrayList<>();
            List<Values> partValues = uncertainParameters.subList((int) Math.round(uncertainParameters.size() * 0.6), uncertainParameters.size());
            for (Values values : partValues) {
                ex.values = values;
                Estimator estimator = estimatorConstructor.get(this.prism, ex);
                Result res = ((MAPEstimator) estimator).checkDTMC(robstrat);
                results.add((double) res.getResult());
            }

            System.out.println("Verification Results with robust strategy (on true MDPs):" + results);
            System.out.println("MDP Robust Guarantee: " + Collections.min(results));

        } catch (PrismException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Existential Robust Results (on IMDPs): " + robustValues);
        System.out.println("IMDP Existential Lambda:" + Collections.min(robustValues));
        System.out.println("Existential True Results (on MDPs)" + trueValues);
        System.out.println("MDP Existential Lambda:" + Collections.min(trueValues));


//        DataProcessor dp = new DataProcessor();
//        dp.dumpRawData(directoryPath, label, results, ex);
    }

    public Pair<ArrayList<DataPoint>, ArrayList<IMDP<Double>>> runSamplingStrategyDoublingEpoch(Experiment ex, Estimator estimator, boolean verifcation) {
        return runSamplingStrategyDoublingEpoch(ex, estimator, 0, verifcation);
    }

    public Pair<ArrayList<DataPoint>, ArrayList<IMDP<Double>>> runSamplingStrategyDoublingEpoch(Experiment ex, Estimator estimator, int past_iterations, boolean verficiation) {
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

            if (this.verbose)
                System.out.println("Episode " + past_iterations + ". " + "Performance " + currentResults[1]);

            ArrayList<DataPoint> results = new ArrayList<>();
            ArrayList<IMDP<Double>> estimates = new ArrayList<>();
            if (past_iterations == 0) {
                results.add(new DataPoint(0, past_iterations, currentResults));
                estimates.add(estimator.getEstimate());
            }
            int samples = 0;
            Strategy samplingStrategy = estimator.buildStrategy();
            for (int i = past_iterations; i < ex.iterations + past_iterations; i++) {
                int sampled = observationSampler.simulateEpisode(ex.max_episode_length, samplingStrategy);
                samples += sampled;
                //System.out.println("Samples Map:" + observationSampler.getSamplesMap());
                boolean last_iteration = i == ex.iterations + past_iterations - 1;
                if (observationSampler.collectedEnoughSamples() || last_iteration || ex.resultIteration(i)) {
                    if (this.verbose) System.out.println("Episode " + i + ". Recomputing sampling strategy.");
                    estimator.setObservationMaps(observationSampler.getSamplesMap(), observationSampler.getSampleSizeMap());
                    samplingStrategy = estimator.buildStrategy();
                    currentResults = estimator.getCurrentResults();
                    if (!ex.tieParameters || (!verficiation && ex.isBayesian())) {
                        observationSampler.resetObservationSequence();
                    } else {
                        observationSampler.incrementAccumulatedSamples();
                    }
                    if (this.verbose) System.out.println("New performance " + currentResults[1]);
                    if (this.verbose) System.out.println("New robust guarantee " + currentResults[0]);

                    if (last_iteration || ex.resultIteration(i)) {
                        results.add(new DataPoint(samples, i + 1, currentResults));
                        estimates.add(estimator.getEstimate());
                    }
                }
            }
            System.out.println("Robust" + currentResults[0]);
            System.out.println("Opimistic" + currentResults[5]);
            if (this.verbose) {
                System.out.println("DONE");
            }

            return new Pair(results, estimates);


        } catch (PrismException e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        }
        prism.closeDown();
        return null;
    }

    public List<List<IMDP<Double>>> partition(List<IMDP<Double>> list, int n) {
        List<List<IMDP<Double>>> resLists = new ArrayList<>();
        resLists.add(list.subList(0, n));
        resLists.add(list.subList(n, list.size()));
        return resLists;
    }

}
