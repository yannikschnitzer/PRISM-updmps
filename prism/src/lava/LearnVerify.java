package lava;

import explicit.IMDP;
import explicit.MDP;
import explicit.MDPModelChecker;
import lava.Experiment.Model;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.distribution.BetaDistribution;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import static lava.Experiment.Model.*;

@CommandLine.Command(mixinStandardHelpOptions = true, version = "Artifact-Eval 0.1", description = "Artifact evaluation code base for \"Certiifiably Robust Policies for Uncertain Parametric Environments\" ")
public class LearnVerify implements Callable<Integer> {

    public MRStrategy rlStrat;
    private Prism prism;
    private String modelStats = null;
    private boolean verbose = true;

    double rangeMin1 = 0.55;
    double rangeMax1 = 0.85;
    double rangeMin2 = 0.718;
    double rangeMax2 = 0.98;

    public LearnVerify() {
    }
    public LearnVerify(boolean verbose) {
        this.verbose = verbose;
    }
    @CommandLine.Option(names = {"-c", "--casestudy"}, description = "Run a specific case study - \"aircraft\", \"betting\", \"sav\", \"chain\", \"drone\", \"firewire\"")
    private String casestudy = "drone";

    @CommandLine.Option(names = {"-a", "--algorithm"}, description = "Run a specific IMDP learning algorhtm - \"LUI\", \"PAC\", \"MAP\", \"UCRL\"")
    private String algorithm = "all";

    @CommandLine.Option(names = {"-no-opt", "--without-optimisations"}, description = "Run without optimisations, i.e., without parameter-tying.")
    private boolean no_optimisations = false;

    @CommandLine.Option(names = {"-seed"}, description = "Sampling seed.")
    private int seed = 1650280571;

    @CommandLine.Option(names = {"-smoke"}, description = "Run smoke test.")
    private boolean is_smoke_test = false;

    public LearnVerify(int seed) {
        this.seed = seed;
    }


    @Override
    public Integer call() throws Exception {
        if (is_smoke_test) {
            System.out.println("Starting Smoke Test.");
            Experiment experiment = new Experiment(AIRCRAFT).config(12, 1_000, seed, true, true, 3);
            run_basic_algorithms(experiment.setSeed(seed));
            System.out.println("Smoke Test finished successfully :)");
        } else {
            basic();
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        if (args.length > 0) {
                int exitCode = new CommandLine(new LearnVerify()).execute(args);
                System.exit(exitCode);
        } else {
            System.out.println("Starting Learning and Verification");;
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
        int m = 10; // = 300;
        int n = 10; // = 200;

        System.out.println(get_seeds(seed, 5));

        String algorithm = "LUI";
        boolean parameter_tying = !no_optimisations;
        System.out.println("invoked with: " + casestudy);

        Experiment experiment;
        switch (casestudy) {
            case "aircraft" -> {
                experiment = new Experiment(AIRCRAFT).config(12, 1_000_000, seed, parameter_tying, parameter_tying, 3).info(id);
            }
            case "betting" -> {
                experiment = new Experiment(BETTING_GAME_FAVOURABLE).config(15, 1_000_000, seed, parameter_tying, parameter_tying, 4).info(id);
            }
            case "sav" -> {
                experiment = new Experiment(SAV2).config(50, 1_000_000, seed, parameter_tying, parameter_tying,  2).info(id);
            }
            case "chain" -> {
                experiment = new Experiment(CHAIN_LARGE_TWO_ACTION).config(100, 1_000_000, seed, parameter_tying, parameter_tying,3).setMaximisation(false).info(id);
            }
            case "drone" -> {
                experiment = new Experiment(DRONE).config(100, 1_000_000, seed, parameter_tying, parameter_tying, 5).info(id);
            }
            default -> throw new RuntimeException("Unknown model type.");
        }

        System.out.println("Running with seeds: " + get_seeds(seed, experiment.numSeeds));
        System.out.println("Running n = " + experiment.numTrainingMDPs+" and m= " + experiment.numVerificationMDPs);
        for (int seed : get_seeds(seed, experiment.numSeeds)) {
            run_basic_algorithms(experiment.setSeed(seed));
        }
    }

    private void run_basic_algorithms(Experiment ex) {
        String postfix = "";
        ex.setResultIterations(new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80, 90, 100, 200, 300, 400, 500, 600, 700, 1000, 1200, 2000, 4000, 6000, 8000, 10000, 15000, 19000, 30000, 40000, 50000, 60000, 80000, 100000, 200000, 300000, 400000, 500000, 800000, 900000)));
        postfix += ex.tieParameters ? "_tied" : (ex.optimizations ? "_opt" : "_naive");

        switch (algorithm) {
            case "all" -> {
                runRobustPolicyComparisonForVis("LUI_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(true).stratWeight(0.9), BayesianEstimatorOptimistic::new);
                runRobustPolicyComparisonForVis("PAC_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(false).stratWeight(0.9), PACIntervalEstimatorOptimistic::new);
                runRobustPolicyComparisonForVis("MAP_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(true).stratWeight(0.9), MAPEstimator::new);
                runRobustPolicyComparisonForVis("UCRL_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(true).stratWeight(0.9), UCRL2IntervalEstimatorOptimistic::new);
            }
            case "LUI" -> {
                runRobustPolicyComparisonForVis("LUI_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(true).stratWeight(0.9), BayesianEstimatorOptimistic::new);
            }
            case "PAC" -> {
                runRobustPolicyComparisonForVis("PAC_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(false).stratWeight(0.9), PACIntervalEstimatorOptimistic::new);
            }
            case "MAP" -> {
                runRobustPolicyComparisonForVis("MAP_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(true).stratWeight(0.9), MAPEstimator::new);
            }
            case "UCRL" -> {
                runRobustPolicyComparisonForVis("UCRL_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(true).stratWeight(0.9), UCRL2IntervalEstimatorOptimistic::new);
            }
            default -> throw new RuntimeException("Unknown algorithm: " + algorithm);
        }
    }


//    private void run_basic_algorithms_pac(Experiment ex) {
//        String postfix = "";
//        String.format("_seed_%d", ex.seed);
//        ex.setResultIterations(new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80, 90, 100, 200, 300, 400, 500, 600, 700, 1000, 1200, 2000, 4000, 6000, 8000, 10000, 15000, 19000, 30000, 40000, 50000, 60000, 80000, 100000, 200000, 300000, 400000, 500000, 800000, 900000)));
//        postfix += ex.tieParameters ? "_tied" : (ex.optimizations ? "_opt" : "_naive");
//        if (!ex.optimizations) {
//            //runRobustPolicyComparisonForVis("LUI_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(true), BayesianEstimatorOptimistic::new);
//        }
//        runRobustPolicyComparisonForVis("PAC_rpol" + postfix, ex.setErrorTol(0.001).setBayesian(false), PACIntervalEstimatorOptimistic::new);
//    }

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
            //System.out.println("Result iterations:" + ex.getResultIterations());
            System.out.println("Label: " + label);
            System.out.println("Running with optimisations: " + (ex.optimizations && ex.tieParameters));
            // Build pMDP model
            resetAll(ex.seed);
            MDP<Function> mdpParam = buildParamModel(ex);

            List<Values> trainingParams = new ArrayList<>();
            List<Values> verificationParams = new ArrayList<>();
            if (ex.model == SAV2) {
                // Generate uniform sample training and verification MDP parameters
                /*
                 * Range for SAV2: [0.75, 0.95] and [0.55, 0.85]
                 */
                //double rangeMean = rangeMin + (rangeMax - rangeMin) / 2;
                Random r = new Random(seed);
                for (int i = 0; i < ex.numTrainingMDPs; i++) {
                    constructValues(rangeMin1, rangeMax1, rangeMin2, rangeMax2, trainingParams, r);
                }
                for (int i = 0; i < ex.numVerificationMDPs; i++) {
                    constructValues(rangeMin1, rangeMax1, rangeMin2, rangeMax2, verificationParams, r);
                }

                trainingParams.addAll(ex.presetValuesTrain);
                verificationParams.addAll(ex.presetValuesVer);

//                System.out.println("Training Parameters:");
//                for (Values value : trainingParams) {
//                    System.out.println(value.getValues() + ",");
//                }
//                System.out.println("Verification Parameters:");
//                for (Values value : verificationParams) {
//                    System.out.println(value.getValues() + ",");
//                }
            } else {

                // Generate beta-distributed sample training and verification MDP parameters
                /*
                 * Parameters for Aircraft: Alpha = 10, Beta = 2
                 * Parameters for Drone Single: Alpha = 2, Beta = 10
                 * Parameters for Betting Game: Alpha = 20, Beta = 2
                 */
                int alpha = switch (ex.model) {
                    case BETTING_GAME_FAVOURABLE -> 20;
                    case AIRCRAFT -> 10;
                    case CHAIN_LARGE_TWO_ACTION -> 5;
                    case DRONE -> 2;
                    default -> throw new PrismException("Unsupported model type");
                };
                int beta = switch (ex.model) {
                    case BETTING_GAME_FAVOURABLE -> 2;
                    case AIRCRAFT -> 2;
                    case CHAIN_LARGE_TWO_ACTION -> 5;
                    case DRONE -> 20;
                    default -> throw new PrismException("Unsupported model type");
                };
                BetaDistribution betaDist = BetaDistribution.of(alpha, beta);
                ContinuousDistribution.Sampler sampler = betaDist.createSampler(RandomSource.JDK.create(seed));
                Iterator<Double> it = sampler.samples().iterator();

                for (int i = 0; i < ex.numTrainingMDPs; i++) {
                    constructValuesBeta(trainingParams, it, ex);
                }
                for (int i = 0; i < ex.numVerificationMDPs; i++) {
                    constructValuesBeta(verificationParams, it, ex);
                }

                trainingParams.addAll(ex.presetValuesTrain);
                verificationParams.addAll(ex.presetValuesVer);

//                System.out.println("Training Parameters:");
//                for (Values value : trainingParams) {
//                    System.out.println(value.getValues() + ",");
//                }
//                System.out.println("Verification Parameters:");
//                for (Values value : verificationParams) {
//                    System.out.println(value.getValues() + ",");
//                }
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

                // Calculate minimum values and store them in auxiliary variables
                double minIMDPRobustGuarantee = ex.maximisation ? Collections.min(robResultsI) : Collections.max(robResultsI); // Minimum IMDP Robust Guarantee with IMDP strategy
                double minIMDPRobustGuaranteeRL = ex.maximisation ? Collections.min(robResultsIRL) : Collections.max(robResultsIRL); // Minimum IMDP Robust Guarantee with RL strategy
                double minTrueMDPRobustGuarantee = ex.maximisation ? Collections.min(robResults) : Collections.max(robResults) ; // Minimum True MDP Robust Guarantee with true MDP strategy
                double minTrueMDPRobustGuaranteeIMDPStrategy = ex.maximisation ? Collections.min(robResultsCross): Collections.max(robResultsCross); // Minimum True MDP Robust Guarantee with IMDP strategy
                double minTrueMDPRobustGuaranteeRL = ex.maximisation ? Collections.min(robResultsCrossRL): Collections.max(robResultsCrossRL); // Minimum True MDP Robust Guarantee with RL strategy
                double minExistentialGuarantee = ex.maximisation ? Collections.min(existentialLambdas) : Collections.max(existentialLambdas); // Minimum Existential Guarantee (Badings et al.)

                // System.out.println("Verification Results with IMDP strategy on IMDPs:" + robResultsI);
                System.out.println("IMDP Robust Guarantee with IMDP strategy (J̃): " + minIMDPRobustGuarantee);

                // System.out.println("Verification Results with RL strategy on IMDPs:" + robResultsIRL);
                System.out.println("IMDP Robust Guarantee with RL strategy (J̃): " + minIMDPRobustGuaranteeRL);

                // System.out.println("Verification Results with MDP strategy on true MDPs:" + robResults);
//                System.out.println("True (hidden) MDP Robust Guarantee with true MDP strategy: " + minTrueMDPRobustGuarantee);

                // System.out.println("Verification Results with IMDP strategy on true MDPs:" + robResultsCross);
                System.out.println("True (hidden) MDP Robust Guarantee with IMDP strategy (J): " + minTrueMDPRobustGuaranteeIMDPStrategy);

                // System.out.println("Verification Results with RL strategy on true MDPs:" + robResultsCrossRL);
                System.out.println("True (hidden) MDP Robust Guarantee with RL strategy (J): " + minTrueMDPRobustGuaranteeRL);

                // System.out.println("Existential Results on true MDPs:" + existentialLambdas);
                System.out.println("Existential Guarantee (Badings et al.): " + minExistentialGuarantee);

                // Add data point to results using auxiliary variables
                results.add(new DataPointRobust(
                        plottedIterations.get(i),
                        new double[]{
                                minIMDPRobustGuarantee,
                                minTrueMDPRobustGuarantee,
                                minTrueMDPRobustGuaranteeIMDPStrategy,
                                minExistentialGuarantee,
                                minIMDPRobustGuaranteeRL,
                                minTrueMDPRobustGuaranteeRL
                        }
                ));

                // Eval robust policy on fresh samples
                if (i == trainingSet.first.getFirst().size() - 1) {
                    System.out.println("\n" + "=============================");
                    // Empirical Risk:
                    computeEmpiricalRisk(robstratI, ex.maximisation ? Collections.min(robResultsI) : Collections.max(robResultsI), 1000, ex);
                    computeEmpiricalRisk(robstratI, 0.7854, 1000, ex);

                    double totalRuntime = (elapsedTimeTraining + elapsedTimeVerification);
                    double runtimePer10k = totalRuntime / (ex.numTrainingMDPs + ex.numVerificationMDPs) / (ex.iterations / 10_000.0);
                    System.out.println("Total runtime: " + totalRuntime + "sec" + ", per 10k trajectories: " + runtimePer10k + "sec");

                    ex.dumpConfiguration(makeOutputDirectory(ex), label, minIMDPRobustGuarantee, minTrueMDPRobustGuarantee, minExistentialGuarantee, minIMDPRobustGuaranteeRL, minTrueMDPRobustGuaranteeRL, totalRuntime, runtimePer10k);
                }

                DataProcessor dp = new DataProcessor();
                dp.dumpDataRobustPolicies(makeOutputDirectory(ex), label, results);
            }


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
            constructValuesBeta(evaluationParams, it, ex);
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
        List<Values> evaluationParams = new ArrayList<>();
        if (ex.model == SAV2) {
            // Generate uniform sample training and verification MDP parameters
            /*
             * Range for SAV2: [0.75, 0.95] and [0.55, 0.85]
             */

            //double rangeMean = rangeMin + (rangeMax - rangeMin) / 2;
            Random b = new Random(seed);
            for (int i = 0; i < numEvalSamples; i++) {
                constructValues(rangeMin1, rangeMax1, rangeMin2, rangeMax2, evaluationParams, b);
            }
        } else {
            int alpha = switch (ex.model) {
                case BETTING_GAME_FAVOURABLE -> 20;
                case AIRCRAFT -> 10;
                case CHAIN_LARGE_TWO_ACTION -> 5;
                case DRONE -> 2;
                default -> throw new PrismException("Unsupported model type");
            };
            int beta = switch (ex.model) {
                case BETTING_GAME_FAVOURABLE -> 2;
                case AIRCRAFT -> 2;
                case CHAIN_LARGE_TWO_ACTION -> 5;
                case DRONE -> 20;
                default -> throw new PrismException("Unsupported model type");
            };
            BetaDistribution betaDist = BetaDistribution.of(alpha, beta);
            ContinuousDistribution.Sampler sampler = betaDist.createSampler(RandomSource.JDK.create(seed));
            Iterator<Double> it = sampler.samples().iterator();

            for (int i = 0; i < numEvalSamples; i++) {
                constructValuesBeta(evaluationParams, it, ex);
            }
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
        //System.out.println("Results for Empirical Eval:" + robResultsCross);
        //System.out.println("Parameters: " + evaluationParams);
        int numFail = 0;
        for (double res : robResultsCross) {
            if (ex.maximisation) {
                if (res < guarantee) {
                    numFail++;
                }
            } else {
                if (res > guarantee) {
                    numFail++;
                }
            }
        }
        int N = robResultsCross.size();
        double empiricalRisk = (double) numFail / (double) N;
        System.out.println("Empirical Risk: " + (empiricalRisk) + " for N = " + N + " and guarantee " + guarantee);
    }

    private void constructValues(double rangeMin1, double rangeMax1,double rangeMin2, double rangeMax2, List<Values> params, Random r) {
        Values v = new Values();
        double pL = rangeMin1 + (rangeMax1 - rangeMin1) * r.nextDouble(); //r.nextGaussian();
        double pH = rangeMin2 + (rangeMax2 - rangeMin2) * r.nextDouble(); //r.nextGaussian();
        /*
         * SAV2: pL -> pL, pH -> pH
         */
        v.addValue("pL", pL);
        v.addValue("pH", pH);
        params.add(v);
    }

    private void constructValuesBeta(List<Values> params, Iterator<Double> it, Experiment ex) throws PrismException {
        Values v = new Values();
        double pL = (ex.model != DRONE) ?  it.next() : 0.0;
        double pH = it.next();

        // For Chain Benchmark, only p's and q'
        /*
         * SAV2: pL -> pL, pH -> pH
         * Aircraft r -> pL, p -> 1 - pH
         * Drone Single p -> min(pH, 0.22), and pL commented out
         * Betting Game p -> pH, no commenting out
         * Chain Benchmark p -> ph  q -> 1 - p, no commenting out
         */
        switch (ex.model) {
            case BETTING_GAME_FAVOURABLE -> {
                v.addValue("r", pL);
                v.addValue("p", pH);
            }
            case AIRCRAFT -> {
                while (1-pH >= 0.5){
                    // need to ensure that 2p <= 1 for valid probability distribution in the model
                    pH = it.next();
                }
                v.addValue("r", pL);
                v.addValue("p", 1 - pH);
            }
            case CHAIN_LARGE_TWO_ACTION -> {
                v.addValue("p", pH);
                v.addValue("q", 1-pH);
            }
            case DRONE -> {
                v.addValue("p", Math.min(pH, 0.32)); // Ensure valid probability distributions
            }
            default -> {
                throw new PrismException("Unsupported model type");
            }
        }
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
            String[] paramNames = new String[]{"p","r","pH","pL","q"};
            String[] paramLowerBounds = new String[]{"0","0","0","0","0"};
            String[] paramUpperBounds = new String[]{"1","1","1","1","1"};
            this.prism.setPRISMModelConstants(new Values(), true);
            this.prism.setParametric(paramNames, paramLowerBounds, paramUpperBounds);
            this.prism.buildModel();

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

            for (Values values : uncertainParameters) {
                ex.values = values;
                Estimator estimator = estimatorConstructor.get(this.prism, ex);
                //System.out.println("Constant Values:" + estimator.getSUL().getConstantValues());
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
        //System.out.println("Similar state map" + similarStateMap);
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

                boolean last_iteration = i == ex.iterations + past_iterations - 1;
                if (observationSampler.collectedEnoughSamples() || last_iteration || ex.resultIteration(i)) {
                    estimator.setObservationMaps(observationSampler.getSamplesMap(), observationSampler.getSampleSizeMap());
                    samplingStrategy = estimator.buildStrategy();
                    currentResults = estimator.getCurrentResults();

                    if (!ex.tieParameters || (!verficiation && ex.isBayesian())) {
                        observationSampler.resetObservationSequence();
                    } else {
                        observationSampler.incrementAccumulatedSamples();
                    }

                    if (this.verbose) System.out.println("Episode " + i + ".");
                    if (this.verbose) System.out.println("Performance on MDPs (J): " + currentResults[1]);
                    if (this.verbose) System.out.println("Performance Guarantee on IMDPs (J̃): " + currentResults[0]);
                    if (this.verbose) System.out.println();

                    if (last_iteration || ex.resultIteration(i)) {
                        results.add(new DataPoint(samples, i + 1, currentResults));
                        estimates.add(estimator.getEstimate());
                    }
                }
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
