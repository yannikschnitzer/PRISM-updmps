package lava;

import explicit.MDP;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.distribution.BetaDistribution;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import param.Function;
import parser.Values;
import parser.ast.ModulesFile;
import prism.*;
import lava.Experiment.Model;
import strat.Strategy;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;


public class AdaptiveLUI {
    private Prism prism;

    private static final int MC_INIT = -1;

    // Prism objects for different models (MC states) to be used in the simulator
    private final List<Prism> simPrismEnvs = new ArrayList<>();

    public AdaptiveLUI() {}

    public static void main(String[] args) {
        int seed = 551999;
        AdaptiveLUI adLUI = new AdaptiveLUI();
        String id = "basic";

        //TODO: currently running everything without model based optimizations, can later decide to change that for some experiments, needs to be implemented
        Experiment ex = new Experiment(Model.CHAIN_LARGE).config(200, 2000, seed, false, false, 3, 2, 2).info(id);

        // Test Parameters, TODO: sub for proper experiment generating
        int alpha = 1;
        int beta = 1;
        BetaDistribution betaDist = BetaDistribution.of(alpha, beta);
        ContinuousDistribution.Sampler sampler = betaDist.createSampler(RandomSource.JDK.create(seed));
        Iterator<Double> it = sampler.samples().iterator();
        List<Values> testParams = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            adLUI.constructValuesBeta(testParams, it);
        }
        System.out.println("Model Parameters:" + testParams);
        // ------------------------

        Pair<List<MDP<Double>>, Estimator> me = adLUI.buildModels(ex, testParams);
        List<MDP<Double>> models = me.first;
        Estimator initialEstimator = me.second;

        adLUI.runAdaptiveLUI(ex, models, initialEstimator);

        System.out.println("Done");
    }

    /*
        Kick-off full experiment procedure.
     */
    public void runAdaptiveLUI(Experiment ex, List<MDP<Double>> models, Estimator initialEstimator){
        try {
            //TODO: implement switching MC
            int MCState = switchingMC(0, -1, models);
            MDP<Double> currModel = models.get(MCState);
            Prism currSimPrism = simPrismEnvs.get(MCState);

            AdaptiveEstimator adaptiveEstimator = new AdaptiveEstimator(initialEstimator, ex, this.prism);
            Estimator activeEstimator = adaptiveEstimator.getActiveEstimate();

            ObservationSampler observationSampler = getObservationsSampler(currModel, currSimPrism, activeEstimator, ex);

            // Simulate trajectories and move generating model with switching MC
            int samples = 0;
            Strategy samplingStrategy = activeEstimator.buildUniformStrat(); // This is an experiment setup, depends on whether we assume random trajectories, or active sampling access to the models
            for (int i = 0; i < ex.iterations; i++) {
                int sampled = observationSampler.simulateEpisode(ex.max_episode_length, samplingStrategy);
                samples += sampled;

                //TODO: Do computations in adaptiveEstimator
                adaptiveEstimator.addTrajectory(observationSampler.getSamplesMap(), observationSampler.getSampleSizeMap());

                if (i % 50 == 0) {
                    System.out.println("Iteration " + i + " of " + ex.iterations);
                    adaptiveEstimator.checkBufferAgreement();
                    adaptiveEstimator.clearObservationMaps();
                    System.out.println("Generating trajectories from model:" + MCState);
                    //System.out.println("Samples Map: " + observationSampler.getSamplesMap());
                    //System.out.println("Sample Size Map: " +  observationSampler.getSampleSizeMap());
                    System.out.println("-----------");
                }

                // Progress MC
                MCState = switchingMC(i, MCState, models);

                // Set new generating model for next iteration
                currModel = models.get(MCState);
                currSimPrism = simPrismEnvs.get(MCState);
                observationSampler = getObservationsSampler(currModel, currSimPrism, activeEstimator, ex);
            }

            for (Estimator e : adaptiveEstimator.getEstimators()) {
                System.out.println("Model Estimate: " + e.getEstimate());
            }

        } catch (PrismException e) {
            throw new RuntimeException(e);
        }


    }

    public int switchingMC(int iterations, int MCState, List<MDP<Double>> models) {
        // Initial State
        if (MCState == MC_INIT) {
            return 0;
        }

        // Transition Probability Matrix
        if (iterations < 1000) {
            return 0;
        } else {
            return 1;
        }
    }

    public ObservationSampler getObservationsSampler(MDP<Double> model, Prism simPrism, Estimator activeEstimator, Experiment ex) throws PrismException {
        ObservationSampler observationSampler = new ObservationSampler(simPrism, model, activeEstimator.getTerminatingStates());
        observationSampler.setTransitionsOfInterest(activeEstimator.getTransitionsOfInterest());
        observationSampler.setTiedParameters(false); //TODO: for later
        observationSampler.setMultiplier(ex.multiplier);
        return observationSampler;
    }

    /*
     Construct generating models and LUI estimators for given experiment and parameters.
     */
    public Pair<List<MDP<Double>>, Estimator> buildModels(Experiment ex, List<Values> parameters) {
        EstimatorConstructor estimatorConstructor = BayesianEstimatorOptimistic::new;
        resetAll(ex.seed);
        List<MDP<Double>> models = new ArrayList<>();
        MDP<Function> mdpParam = buildParamModel(ex); // Needed if we want to do model based opts
        List<List<TransitionTriple>> similarTransitions = getSimilarTransitions(mdpParam);
        Map<Function, List<TransitionTriple>> functionMap = getFunctionMap(mdpParam);

        Estimator initialEstmator = null;

        try {
            resetAll(ex.seed);
            ModulesFile modulesFile = prism.parseModelFile(new File(ex.modelFile));
            prism.loadPRISMModel(modulesFile);

            for (Values values : parameters) {
                ex.values = values;

                Estimator estimator = estimatorConstructor.get(this.prism, ex);
                estimator.setFunctionMap(functionMap);
                estimator.setSimilarTransitions(similarTransitions);
                estimator.set_experiment(ex);

                models.add(estimator.getSUL());
                initialEstmator = estimator;

                // Only for initializing the Prism objects needed for simulation
                Prism simPrism = initializeSimPrism(ex.seed);
                ModulesFile simModulesFile = simPrism.parseModelFile(new File(ex.modelFile));
                simPrism.loadPRISMModel(simModulesFile);
                estimatorConstructor.get(simPrism, ex);
                estimator.set_experiment(ex);
                this.simPrismEnvs.add(simPrism);
            }
            System.out.println(models.size());

        } catch (FileNotFoundException | PrismException e) {
            throw new RuntimeException(e);
        }
        return new Pair<>(models, initialEstmator);
    }

    /*
        Construct parametric model for given experiment.
     */
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
            String[] paramNames = new String[]{"p", "q", "r"};
            String[] paramLowerBounds = new String[]{"0","0","0"};
            String[] paramUpperBounds = new String[]{"1","1","1"};
            this.prism.setPRISMModelConstants(new Values(), true);
            this.prism.setParametric(paramNames, paramLowerBounds, paramUpperBounds);
            this.prism.buildModel();
            return (MDP<Function>) this.prism.getBuiltModelExplicit();

        } catch (PrismException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }
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
        v.addValue("p", pH);
        v.addValue("q", 1 - pH);
        params.add(v);
    }

    public void initializePrism() throws PrismException {
        this.prism = new Prism(new PrismDevNullLog());
        this.prism.initialise();
        this.prism.setEngine(Prism.EXPLICIT);
        this.prism.setGenStrat(true);
    }

    public Prism initializeSimPrism(int seed) throws PrismException {
        Prism simPrism = new Prism(new PrismDevNullLog());
        simPrism.initialise();
        simPrism.setEngine(Prism.EXPLICIT);
        simPrism.setGenStrat(true);
        simPrism.setSimulatorSeed(seed);

        return simPrism;
    }

    public void resetAll(int seed) {
        try {
            initializePrism();
            this.prism.setSimulatorSeed(seed);
        } catch (PrismException e) {
            System.out.println("PrismException in LearnVerify.resetAll()  :  " + e.getMessage());
            System.exit(1);
        }
    }

    // Optimization Stuff copied from LearnVerify, not used yet
    public Set<Function> getTransitionStructure(MDP<Function> mdpParam, int s, int a) {
        HashSet<Function> transitions = new HashSet<>();
        mdpParam.forEachTransition(s, a, (int sFrom, int sTo, Function p) -> {
            transitions.add(p);
        });
        return transitions;
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

    public String getActionString(MDP<Function> mdp, int s, int i) {
        String action = (String) mdp.getAction(s, i);
        if (action == null) {
            action = "_empty";
        }
        return action;
    }
}

