package lava;

import explicit.*;
import lava.Experiment.Model;
import org.apache.commons.rng.simple.RandomSource;
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

import org.apache.commons.statistics.distribution.*;
import org.apache.commons.statistics.distribution.ContinuousDistribution;

public class LearnVerify {

    private Prism prism;
    private String modelStats = null;


    private int seed = 1650280571;

    private boolean verbose = true;

    public MRStrategy rlStrat;

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
            for (String s : args) {
                try {
                    seed = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    System.out.println("skipping invalid seed " + s);
                    continue;
                }
                System.out.println("running with seed " + s);
                //LearnVerify l = new LearnVerify(seed);
                //l.basic();
                //l.switching_environment();
                //l.gridStrengthEval();x
                //l.evaluate_strength();
            }
        } else {
            System.out.println("Starting Learning and Verification");
            LearnVerify l = new LearnVerify();
            l.basic();
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
        int m = 1; // = 300;
        int n = 1; // = 200;

//        run_basic_algorithms(new Experiment(Model.CHAIN_SMALL).config(100, 1000, seed).info(id));
//        run_basic_algorithms(new Experiment(Model.LOOP).config(100, 1000, seed).info(id));

//       run_basic_algorithms(new Experiment(Model.AIRCRAFT).config(10, 100_000, seed, true, true,4).info(id));
//        run_basic_algorithms(new Experiment(Model.AIRCRAFT).config(100, 1_000_000, seed, true, false, 4).info(id));
//        run_basic_algorithms(new Experiment(Model.AIRCRAFT).config(100, 1_000_000, seed, false, false, 10).info(id));

//        run_basic_algorithms(new Experiment(Model.BRP).config(100, 1_00_000, seed, true, true, 10).info(id));
//        run_basic_algorithms(new Experiment(Model.BRP).config(100, 1_00_000, seed, true, false, 10).info(id));
//        run_basic_algorithms(new Experiment(Model.BRP).config(100, 1_00_000, seed, false, false, 50).info(id));

//        run_basic_algorithms(new Experiment(Model.NAND).config(50, 1_0_000, seed, false).info(id));
//        run_basic_algorithms(new Experiment(Model.DRONE).config(50, 1_000, seed, false).info(id));
//        run_basic_algorithms(new Experiment(Model.NAND).config(50, 1_000_000, seed, true).info(id));

//        run_basic_algorithms(new Experiment(Model.AIRCRAFT).config(102, 1_000_000, seed, false).info(id));

//        for (int seed : get_seeds(seed, 10)) {
//            run_basic_algorithms(new Experiment(Model.SAV2).config(50, 1_000_000, seed, true, true, m, n, 4).info(id));
//            //run_basic_algorithms_pac(new Experiment(Model.SAV2).config(50, 1_000_000, seed, true, false, m, n, 2).info(id));
//            //run_basic_algorithms_pac(new Experiment(Model.SAV2).config(50, 1_000_000, seed, false, false, m, n, 2).info(id));
//        }

//        for (int seed : get_seeds(seed, 1)) {
         //   run_basic_algorithms(new Experiment(Model.AIRCRAFT).config(10, 10_000, seed, true, true, m, n, 2).info(id));
 //           run_basic_algorithms_pac(new Experiment(Model.AIRCRAFT).config(10, 1_0_000, seed, true, true, m, n, 2).info(id));
//            //run_basic_algorithms_pac(new Experiment(Model.AIRCRAFT).config(10, 1_00000, seed, false, false, m, n, 2).info(id));
//        }

//        for (int seed : get_seeds(seed, 1)) {
//            //run_basic_algorithms(new Experiment(Model.DRONE_SINGLE).config(50, 1_000_000, seed, true, true, m, n, 4).info(id));
//            //run_basic_algorithms_pac(new Experiment(Model.DRONE_SINGLE).config(50, 1_0_000, seed, true, true, m, n, 2).info(id));
//            //run_basic_algorithms_pac(new Experiment(Model.DRONE_SINGE).config(10, 1_000_000, seed, false, false, m, n, 2).info(id));
//        }


//        for (int seed : get_seeds(seed, 10)) {
//            run_basic_algorithms(new Experiment(Model.CHAIN_LARGE).config(30, 1_000_000, seed, true, true, m, n, 4).info(id));
//            //run_basic_algorithms_pac(new Experiment(Model.CHAIN_LARGE).config(30, 1_000_000, seed, true, false, m, n, 2).info(id));
//            //run_basic_algorithms_pac(new Experiment(Model.CHAIN_LARGE).config(30, 1_000_000, seed, false, false, m, n, 2).info(id));
//        }
//
//        for (int seed : get_seeds(seed, 10)) {
            run_basic_algorithms(new Experiment(Model.BETTING_GAME_FAVOURABLE).config(15, 1_000_000, seed, true, true, m, n, 4).info(id));
//            run_basic_algorithms_pac(new Experiment(Model.BETTING_GAME_FAVOURABLE).config(15, 1_000_000, seed, true, false, m, n, 2).info(id));
//            run_basic_algorithms_pac(new Experiment(Model.BETTING_GAME_UNFAVOURABLE).config(15, 1_000_000, seed, false, false, m, n, 2).info(id));
//        }
    }

    private void run_basic_algorithms(Experiment ex) {
        String postfix = "";
        ex.setResultIterations(new ArrayList<>(Arrays.asList(1,2,3,4,5,6,7,8,9, 10,12, 15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80, 90, 100, 200, 300, 400, 500, 600, 700, 1000, 1200, 2000, 4000, 6000, 8000, 10000, 15000, 19000, 30000, 40000, 50000, 60000, 80000, 100000, 200000, 300000, 400000, 500000, 800000, 900000)));
        postfix += ex.tieParameters ? "_tied" : (ex.optimizations ? "_opt" : "_naive");

        runRobustPolicyComparisonForVis("LUI_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(true), BayesianEstimatorOptimistic::new);
       // runRobustPolicyComparisonForVis("PAC_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(false).setTieParamters(true), PACIntervalEstimatorOptimistic::new);
       // runRobustPolicyComparisonForVis("MAP_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(true).setTieParamters(false), MAPEstimator::new);
        //runRobustPolicyComparisonForVis("UCRL_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(true).setTieParamters(false), UCRL2IntervalEstimatorOptimistic::new);
    }


    private void run_basic_algorithms_pac(Experiment ex) {
        String postfix = "";String.format("_seed_%d", ex.seed);
        ex.setResultIterations(new ArrayList<>(Arrays.asList(1,2,3,4,5,6,7,8,9, 10,12, 15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80, 90, 100, 200, 300, 400, 500, 600, 700, 1000, 1200, 2000, 4000, 6000, 8000, 10000, 15000, 19000, 30000, 40000, 50000, 60000, 80000, 100000, 200000, 300000, 400000, 500000, 800000, 900000)));
        postfix += ex.tieParameters ? "_tied" : (ex.optimizations ? "_opt" : "_naive");
        if (!ex.optimizations){
            //runRobustPolicyComparisonForVis("LUI_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(true), BayesianEstimatorOptimistic::new);
        }
        runRobustPolicyComparisonForVis("PAC_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(false), PACIntervalEstimatorOptimistic::new);
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
        String outputPath = String.format("artifact_eval/results/%s/%s/Robust_Policies_WCC/%s/", ex.experimentInfo, ex.model.toString(), ex.seed);
        try {
            Files.createDirectories(Paths.get(outputPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputPath;
    }


    public void runRobustPolicyComparisonForVis(String label, Experiment ex, EstimatorConstructor estimatorConstructor) {
        try {
            System.out.println("Result iterations:" + ex.getResultIterations());
            System.out.println("Label: " + label);
            // Build pMDP model
            resetAll(ex.seed);
            MDP<Function> mdpParam = buildParamModel(ex);

            Iterator<Entry<Integer, Function>> iter = mdpParam.getTransitionsIterator(43,1);
            while(iter.hasNext()) {
                System.out.println("Iteration: " + iter.next().getValue().getClass());
            }

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
            int alpha = 20;
            int beta = 2;
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

            Values val = new Values();
            val.setValue("p",0.6849641245);
            verificationParams.add(val);
            trainingParams.add(val);

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
            double elapsedTimeTraining = elapsedTime / 1000.0;

            startTime = System.currentTimeMillis();

            // Get MDPs/IMDPs for verification
            Pair<List<List<IMDP<Double>>>, List<MDP<Double>>> verificationSet = getIMDPsForVis(label, ex, PACIntervalEstimator::new, mdpParam, verificationParams, true);

            endTime = System.currentTimeMillis();
            elapsedTime = endTime - startTime;
            double elapsedTimeVerification = elapsedTime / 1000.0;

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
                //System.out.println("Verification Results with IMDP strategy on IMDPs:" + robResultsI);
                System.out.println("IMDP Robust Guarantee with IMDP strategy: " + Collections.min(robResultsI));

                //System.out.println("Verification Results with RL strategy on IMDPs:" + robResultsIRL);
                System.out.println("IMDP Robust Guarantee with RL strategy: " + Collections.min(robResultsIRL));

                //System.out.println("Verification Results with MDP strategy on true MDPs:" + robResults);
                System.out.println("True MDP Robust Guarantee with true MDP strategy: " + Collections.min(robResults));

                //System.out.println("Verification Results with IMDP strategy on true MDPs:" + robResultsCross);
                System.out.println("True MDP Robust Guarantee with IMDP strategy: " + Collections.min(robResultsCross));

                //System.out.println("Verification Results with RL strategy on true MDPs:" + robResultsCrossRL);
                System.out.println("True MDP Robust Guarantee with RL strategy: " + Collections.min(robResultsCrossRL));

                //System.out.println("Existential Results on true MDPs:" + existentialLambdas);
                System.out.println("Existential Guarantee (Badings et al.): " + Collections.min(existentialLambdas));

                results.add(new DataPointRobust(plottedIterations.get(i), new double[]{Collections.min(robResultsI), Collections.min(robResults), Collections.min(robResultsCross), Collections.min(existentialLambdas), Collections.min(robResultsIRL), Collections.min(robResultsCrossRL)}));

                List<Double> evalResIMDP = List.of();
                List<Double> evalResRL = List.of();
                // Eval robust policy on fresh samples
                if (i == trainingSet.first.getFirst().size() - 1) {
                    System.out.println("\n" + "==============================");

                    // Evaluation on 200 fresh samples for RL and IMDP policy
                    evalResIMDP = evaluatePolicy(robstratI, 200, ex, false, plottedIterations.get(i));
                    evalResRL = evaluatePolicy(null, 200, ex, true, plottedIterations.get(i));

                    System.out.println("\n" + "=============================");
                    // Empirical Risk:
                    computeEmpiricalRisk(robstratI,Collections.min(robResultsI), 1000, ex);
                    computeEmpiricalRisk(robstratI,30.9, 1000 , ex);
                }


                DataProcessor dp = new DataProcessor();
                dp.dumpDataRobustPolicies(makeOutputDirectory(ex), label, results);
                dp.dumpResultList(makeOutputDirectory(ex), label, robResultsCross, existentialLambdas, evalResIMDP, evalResRL, elapsedTimeTraining, elapsedTimeVerification);
            }

            System.out.println("Total runtime: " + (elapsedTimeTraining + elapsedTimeVerification) + "sec" + ", per 10k trajectories: " + (elapsedTimeTraining + elapsedTimeVerification) / (ex.artifact_m + ex.artifact_n) / (ex.iterations / 10^4)+"sec");

            ex.dumpConfiguration(makeOutputDirectory(ex), label, "");

        } catch (PrismException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Double> evaluatePolicy(MDStrategy<Double> strat, int numEvalSamples, Experiment ex, boolean isRL, int iteration) throws PrismException {

        int alpha = 20;
        int beta = 2;
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

        List<Double> existentialLambdas = getExistentialGuarantee(prism, ex, evalMDPs);
        System.out.println("Eval Existential Lambdas: " + existentialLambdas);

        RobustPolicySynthesizerMDP robSynth = new RobustPolicySynthesizerMDP(null, ex);
        robSynth.setVerificationSet(evalMDPs);
        List<Double> robResultsCross = !isRL ?
                robSynth.checkVerificationSet(prism, strat, ex.dtmcSpec)
                :
                robSynth.checkVerificatonSetRLPolicy(prism, ex.dtmcSpec, iteration);


        System.out.println("Eval Results with robust strategy: " + robResultsCross);

        return robResultsCross;
    }

    private void computeEmpiricalRisk(MDStrategy<Double> strat, double guarantee, int numEvalSamples, Experiment ex) throws PrismException {
        int alpha = 20;
        int beta = 2;
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
        System.out.println("Parameters: " + evaluationParams);
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
        double pL = it.next();
        double pH = it.next();

        // For Chain Benchmark, only p's and q'
        /*
         * SAV2: pL -> pL, pH -> pH
         * Aircraft r -> pL, p -> 1 - pH
         * Drone Single p -> min(pH, 0.22), and pL commented out
         * Betting Game p -> pH
         * Chain Benchmark p -> ph  q -> 1 - p
         */
        v.addValue("r", pL);
        v.addValue("p", pH);
        params.add(v);
    }

    public List<Double> getExistentialGuarantee(Prism prism, Experiment ex, List<MDP<Double>> mdps) {
        try {
            ArrayList<Double> results = new ArrayList<>();
            MDPModelChecker mc = new MDPModelChecker(prism);
            mc.setErrorOnNonConverge(false);

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
            String[] paramNames = new String[]{"p"};
            String[] paramLowerBounds = new String[]{"0"};
            String[] paramUpperBounds = new String[]{"1"};
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
            for (Function f : functionMap.keySet()) {
                System.out.println("function:" + f + functionMap.get(f));
            }

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

                mdpParam.forEachTransition(s, i, (int sFrom, int sTo, Function p) -> {
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
                    if (this.verbose) System.out.println("Episode " + i + ".");
                    estimator.setObservationMaps(observationSampler.getSamplesMap(), observationSampler.getSampleSizeMap());
                    samplingStrategy = estimator.buildStrategy();
                    currentResults = estimator.getCurrentResults();
                    if (!ex.tieParameters || (!verficiation && ex.isBayesian())) {
                        observationSampler.resetObservationSequence();
                    } else {
                        observationSampler.incrementAccumulatedSamples();
                    }
                    if (this.verbose) System.out.println("Performance on MDPs (J): " + currentResults[1]);
                    if (this.verbose) System.out.println("Performance Guarantee on IMDPs (J̃): " + currentResults[0]);
                    if (this.verbose) System.out.println("");

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

}
