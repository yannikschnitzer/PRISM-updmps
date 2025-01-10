package lava;

import parser.Values;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Experiment {


    public Model model;
    public Type type;
    public String goal;
    public String spec;
    public String robustSpec;
    public String optimisticSpec;
    public String modelFile;
    public String dtmcSpec;
    public String idtmcRobustSpec;
    public Values values;
    // Turn model-based optimizations on / off
    public boolean optimizations = false;
    // Turn parameter tying on / off
    public boolean tieParameters = false;
    public int multiplier = 1;
    public String experimentInfo = "basic";
    public int seed = 1;
    public int initLowerStrength = 5;
    public int initUpperStrength = 10;
    public int lowerStrengthBound = Integer.MAX_VALUE;
    public int upperStrengthBound = Integer.MAX_VALUE;
    public int maxMAPStrength = Integer.MAX_VALUE;
    public double initGraphEpsilon = 1e-4;
    public int iterations = 10;
    public int max_episode_length = 100;
    public int alpha = 10;
    public double error_tolerance = 0.01; // 99% correctness guarantee
    public double strategyWeight = 1.0;
    public int numTrainingMDPs;
    public int numVerificationMDPs;
    // Additional information for artifact evaluation, leads to identical results but faster running times for evaluation.
    public Values val;
    public List<Values> presetValuesVer = new ArrayList<>();
    public List<Values> presetValuesTrain = new ArrayList<>();
    public double zeroGuarantee;
    public double fiveGuarantee;
    public double tenGuarantee;
    public int artifact_n;
    public int artifact_m;
    public int standard_n;
    public int standard_m;
    public int numSeeds = 1;
    public double k0perf;
    public double k5perf;
    public double k10perf;

    public InitialInterval initialInterval = InitialInterval.WIDE;
    public double trueOpt = Double.NaN;
    private String modelInfo = "";
    private boolean bayesian;
    // Iterations at which results must be obtained (for visualization purposes)
    private List<Integer> resultIterations = new ArrayList<>();
    public Experiment(Model model) {
        setModel(model);
    }

    public boolean maximisation = true;

    public List<Integer> getResultIterations() {
        return resultIterations;
    }

    public void setResultIterations(List<Integer> resultIterations) {
        this.resultIterations = resultIterations;
    }

    public boolean isBayesian() {
        return bayesian;
    }

    public Experiment setBayesian(boolean bayesian) {
        this.bayesian = bayesian;
        return this;
    }

    public Experiment setMaximisation(boolean maximisation) {
        this.maximisation = maximisation;
        return this;
    }

    private void setModel(Model model) {
        this.model = model;
        switch (model) {
            case CHAIN_SMALL:
                this.goal = "\"goal\"";
                this.spec = "Rmin=? [F \"goal\"]";
                this.robustSpec = "Rminmax=? [F \"goal\"]";
                this.optimisticSpec = "Rminmin=? [F \"goal\"]";
                this.dtmcSpec = "R=? [F \"goal\"]";
                this.modelFile = "models/chain.prism";
                this.type = Type.REWARD;
                break;
            case CHAIN_SMALL2:
                this.goal = "\"goal\"";
                this.spec = "Rmin=? [F \"goal\"]";
                this.robustSpec = "Rminmax=? [F \"goal\"]";
                this.optimisticSpec = "Rminmin=? [F \"goal\"]";
                this.dtmcSpec = "R=? [F \"goal\"]";
                this.modelFile = "models/chain_2.prism";
                this.type = Type.REWARD;
                break;
            case CHAIN2:
                this.goal = "\"goal\"";
                this.spec = "Pmax=? [F<=5 \"goal\"]";
                this.robustSpec = "Pmaxmin=? [F<=5 \"goal\"]";
                this.optimisticSpec = "Pmaxmax=? [F<=5 \"goal\"]";
                this.dtmcSpec = "P=? [F<=5 \"goal\"]";
                this.modelFile = "models/chain.prism";
                this.type = Type.REACH;
                break;
            case CHAIN_LARGE:
                this.goal = "\"goal\"";
                this.spec = "Rmin=? [F \"goal\"]";
                this.idtmcRobustSpec = "Rmax=? [F \"goal\"]";
                this.robustSpec = "Rminmax=? [F \"goal\"]";
                this.optimisticSpec = "Rminmin=? [F \"goal\"]";
                this.dtmcSpec = "R=? [F \"goal\"]";
                this.modelFile = "models/chain_large.prism";
                this.type = Type.REWARD;
                this.optimizations = true;
                break;
            case CHAIN_LARGE_TWO_ACTION:
                this.goal = "\"goal\"";
                this.spec = "Rmin=? [F \"goal\"]";
                this.idtmcRobustSpec = "Rmax=? [F \"goal\"]";
                this.robustSpec = "Rminmax=? [F \"goal\"]";
                this.optimisticSpec = "Rminmin=? [F \"goal\"]";
                this.dtmcSpec = "R=? [F \"goal\"]";
                this.modelFile = "models/chain_large_twoaction.prism";
                this.type = Type.REWARD;
                this.optimizations = true;

                // Preset values for artifact reproducibility in feasible runtime
                this.numVerificationMDPs = 10;
                this.numTrainingMDPs = 10;
                this.numSeeds = 5;

                this.k0perf = 38279.7;
                this.k5perf = 29729.0;
                this.k10perf = 6873.94;

                break;
            case CHAIN_LARGE2:
                this.goal = "\"goal\"";
                this.spec = "Rmin=? [F \"goal\"]";
                this.robustSpec = "Rminmax=? [F \"goal\"]";
                this.optimisticSpec = "Rminmin=? [F \"goal\"]";
                this.dtmcSpec = "R=? [F \"goal\"]";
                this.modelFile = "models/chain_large2.prism";
                this.type = Type.REWARD;
                break;
            case SAV2:
                this.goal = "\"Target\"";
                this.spec = "Pmax=? [!(\"Crash\") U (\"Target\")]";
                this.idtmcRobustSpec = "Pmin=? [!(\"Crash\") U (\"Target\")]";
                this.robustSpec = "Pmaxmin=? [!(\"Crash\") U (\"Target\")]";
                this.optimisticSpec = "Pmaxmax=? [!(\"Crash\") U (\"Target\")]";
                this.dtmcSpec = "P=? [!(\"Crash\") U (\"Target\")]";
                this.modelFile = "models/sav.prism";
                this.type = Type.REACH;
                this.optimizations = false;

                // Preset values for artifact reproducibility in feasible runtime
                val = new Values();
                val.addValue("pL", 0.637039567623);
                val.addValue("pH", 0.719682412046);
                this.presetValuesVer.add(val);
                this.numVerificationMDPs = 1;
                this.numTrainingMDPs = 12;
                this.numSeeds = 3;

                this.k0perf = 0.7855;
                this.k5perf = 0.7883;
                this.k10perf = 0.7946;
                break;
            case BRP:
                this.goal = "\"Target\"";
                this.spec = "Pmax=? [ true U (s=5 & T) ]";
                this.robustSpec = "Pmaxmin=? [ true U (s=5 & T) ]";
                this.optimisticSpec = "Pmaxmax=? [ true U (s=5 & T) ]";
                this.dtmcSpec = "P=? [ true U (s=5 & T) ]";
                this.modelFile = "models/brp.prism";
                this.type = Type.REACH;
                this.optimizations = false;
                break;
            case CROWD:
                this.goal = "\"Target\"";
                this.spec = "Pmax=? [F \"observe0Greater1\" ]";
                this.robustSpec = "Pmaxmax=? [F \"observe0Greater1\" ]";
                this.optimisticSpec = "Pmaxmax=? [F \"observe0Greater1\" ]";
                this.dtmcSpec = "P=? [F \"observe0Greater1\" ]";
                this.modelFile = "models/crowd.prism";
                this.type = Type.REACH;
                this.optimizations = false;
                break;
            case NAND:
                this.goal = "\"Target\"";
                this.spec = "Pmax=? [F \"target\" ]";
                this.robustSpec = "Pmaxmin=? [F \"target\" ]";
                this.optimisticSpec = "Pmaxmax=? [F \"target\" ]";
                this.dtmcSpec = "P=? [F \"target\" ]";
                this.modelFile = "models/nand.prism";
                this.type = Type.REACH;
                this.optimizations = false;
                break;
            case DRONE_OLD:
                this.goal = "\"Target\"";
                this.spec = "Pmax=? [F attarget ]";
                this.robustSpec = "Pmaxmin=? [F attarget ]";
                this.optimisticSpec = "Pmaxmax=? [F attarget ]";
                this.dtmcSpec = "P=? [F attarget ]";
                this.modelFile = "models/drone.prism";
                this.type = Type.REACH;
                this.optimizations = false;
                break;
            case DRONE:
                this.goal = "\"target\"";
                this.spec = "Pmax=? [!crash U target]";
                this.idtmcRobustSpec = "Pmin=? [!crash U target]";
                this.robustSpec = "Pmaxmin=? [!crash U target]";
                this.optimisticSpec = "Pmaxmax=? [!crash U target]";
                this.dtmcSpec = "P=? [!crash U target]";
                this.modelFile = "models/drone_single_param.prism";
                this.type = Type.REACH;
                this.optimizations = false;

                // Preset values for artifact reproducibility in feasible runtime
                val = new Values();
                val.setValue("p", 0.3150358755);
                this.presetValuesVer.add(val);
                this.numVerificationMDPs = 4;
                this.numTrainingMDPs = 8;
                this.numSeeds = 3;


                this.k0perf = 0.7274;
                this.k5perf = 0.8487;
                this.k10perf = 0.8957;

                break;
            case FIREWIRE:
                this.goal = "dead";
                this.spec = "Pmin=? [F \"dead\" ]";
                this.idtmcRobustSpec = "Pmax=? [F \"dead\" ]";
                this.robustSpec = "Pminmax=? [F \"dead\" ]";
                this.optimisticSpec = "Pminmin=? [F \"dead\" ]";
                this.dtmcSpec = "P=? [F \"dead\" ]";
                this.modelFile = "models/firewire.prism";
                this.type = Type.REACH;
                this.optimizations = false;

                // Preset values for artifact reproducibility in feasible runtime
                val = new Values();
                val.setValue("fast", 0.109280077836);
                this.presetValuesVer.add(val);
                this.numVerificationMDPs = 0;
                this.numTrainingMDPs = 5;
                this.numSeeds = 1;

                this.k0perf = 0.7274;
                this.k5perf = 0.8487;
                this.k10perf = 0.8957;

                break;
            case CONSENSUS2:
                this.goal = "\"finished\"&\"all_coins_equal_1\"";
                this.spec = "Pmin=? [ F \"finished\"&\"all_coins_equal_1\" ]";
                this.robustSpec = "Pminmax=? [ F \"finished\"&\"all_coins_equal_1\" ]";
                this.optimisticSpec = "Pminmin=? [ F \"finished\"&\"all_coins_equal_1\" ]";
                this.dtmcSpec = "P=? [ F \"finished\"&\"all_coins_equal_1\" ]";
                this.modelFile = "models/consensus3.prism";
                this.type = Type.REACH;
                this.optimizations = false;
                break;
            case CONSENSUS4:
                this.goal = "\"finished\"&\"all_coins_equal_1\"";
                this.spec = "Pmin=? [ F \"finished\"&\"all_coins_equal_1\" ]";
                this.robustSpec = "Pminmin=? [ F \"finished\"&\"all_coins_equal_1\" ]";
                this.optimisticSpec = "Pminmin=? [ F \"finished\"&\"all_coins_equal_1\" ]";
                this.dtmcSpec = "P=? [ F \"finished\"&\"all_coins_equal_1\" ]";
                this.modelFile = "models/consensus4.prism";
                this.type = Type.REACH;
                this.optimizations = false;
                break;
            case TINY:
                this.goal = "\"goal\"";
                this.spec = "Rmin=? [F \"goal\"]";
                this.robustSpec = "Rminmax=? [F \"goal\"]";
                this.optimisticSpec = "Rminmin=? [F \"goal\"]";
                this.dtmcSpec = "R=? [F \"goal\"]";
                this.modelFile = "models/tiny.prism";
                this.type = Type.REWARD;
                break;
            case TINY2:
                this.goal = "\"goal\"";
                this.spec = "Rmin=? [F \"goal\"]";
                this.robustSpec = "Rminmax=? [F \"goal\"]";
                this.optimisticSpec = "Rminmin=? [F \"goal\"]";
                this.dtmcSpec = "R=? [F \"goal\"]";
                this.modelFile = "models/tiny2.prism";
                this.type = Type.REWARD;
                break;
            case GRID:
                this.goal = "\"goal_NE\"";
                this.spec = "Pmax=? [!(\"trap\") U (\"goal_NE\")]";
                this.robustSpec = "Pmaxmin=? [!(\"trap\") U (\"goal_NE\") ]";
                this.optimisticSpec = "Pmaxmax=? [!(\"trap\") U (\"goal_NE\") ]";
                this.dtmcSpec = "P=? [!(\"trap\") U (\"goal_NE\")]";
                this.modelFile = "models/grid.prism";
                this.type = Type.REACH;
                break;
            case GRID_SAFE_SE:
                this.goal = "\"goal_SE\"";
                this.spec = "Pmax=? [!(\"trap\") U (\"goal_SE\")]";
                this.robustSpec = "Pmaxmin=? [!(\"trap\") U (\"goal_SE\") ]";
                this.optimisticSpec = "Pmaxmax=? [!(\"trap\") U (\"goal_SE\") ]";
                this.dtmcSpec = "P=? [!(\"trap\") U (\"goal_SE\")]";
                this.modelFile = "models/grid.prism";
                this.type = Type.REACH;
                break;
            case CHAIN_test:
                this.goal = "\"goal\"";
                this.spec = "Pmax=? [F<=100 \"goal\"]";
                this.robustSpec = "Pmaxmin=? [F<=100 \"goal\"]";
                this.optimisticSpec = "Pmaxmax=? [F<=100 \"goal\"]";
                this.dtmcSpec = "P=? [F<=100 \"goal\"]";
                this.modelFile = "models/chain_test.prism";
                this.type = Type.REACH;
                break;
            case LOOP:
                this.goal = "\"goal\"";
                this.spec = "Rmax=? [F \"goal\"]";
                this.robustSpec = "Rmaxmin=? [F \"goal\"]";
                this.optimisticSpec = "Rmaxmax=? [F \"goal\"]";
                this.dtmcSpec = "R=? [F \"goal\"]";
                this.modelFile = "models/loop.prism";
                this.type = Type.REWARD;
                break;
            case AIRCRAFT:
                this.goal = "\"goal\"";
                this.spec = "Pmax=? [!collision U \"goal\"]";
                this.robustSpec = "Pmaxmin=? [!collision U \"goal\"]";
                this.optimisticSpec = "Pmaxmax=? [!collision U \"goal\"]";
                this.idtmcRobustSpec = "Pmin=? [!collision U \"goal\"]";
                this.dtmcSpec = "P=?  [!collision U \"goal\"]";
                this.modelFile = "models/aircraft_tiny.prism";
                this.type = Type.REACH;
                this.optimizations = true;

                // Preset values for artifact reproducibility in feasible runtime
                val = new Values();
                val.addValue("p", 0.175920355159);
                val.addValue("r", 0.464605015264);
                this.presetValuesVer.add(val);
                this.numVerificationMDPs = 10; // = 10
                this.numTrainingMDPs = 10;
                this.numSeeds = 4;

                // Preset values for
                this.k0perf = 0.6076;
                this.k5perf = 0.6590;
                this.k10perf = 0.7199;

                break;
            case BANDIT:
                this.goal = "\"goal\"";
                this.spec = "Rmax=? [F \"goal\"]";
                this.robustSpec = "Rmaxmin=? [F \"goal\"]";
                this.optimisticSpec = "Rmaxmax=? [F \"goal\"]";
                this.dtmcSpec = "R=?  [F \"goal\"]";
                this.modelFile = "models/bandit.prism";
                this.type = Type.REACH; // TODO: change to REWARD?
                break;
            case BETTING_GAME_FAVOURABLE:
                this.goal = "\"done\"";
                this.spec = "Rmax=? [F \"done\"]";
                this.idtmcRobustSpec = "Rmin=? [F \"done\"]";
                this.robustSpec = "Rmaxmin=? [F \"done\"]";
                this.optimisticSpec = "Rmaxmax=? [F \"done\"]";
                this.dtmcSpec = "R=?  [F \"done\"]";
                this.modelFile = "models/bet_fav.prism";
                this.type = Type.REWARD;
                this.optimizations = true;

                // Preset values for artifact reproducibility in feasible runtime
                val = new Values();
                val.setValue("p", 0.6849641245);
                this.presetValuesVer.add(val);
                this.numVerificationMDPs = 1;
                this.numTrainingMDPs = 5;
                this.numSeeds = 5;

                this.k0perf = 32.08;
                this.k5perf = 39.91;
                this.k10perf = 42.50;

                break;
            case BETTING_GAME_UNFAVOURABLE:
                this.goal = "\"done\"";
                this.spec = "Rmax=? [F \"done\"]";
                this.robustSpec = "Rmaxmin=? [F \"done\"]";
                this.optimisticSpec = "Rmaxmax=? [F \"done\"]";
                this.dtmcSpec = "R=?  [F \"done\"]";
                this.modelFile = "models/bet_unfav.prism";
                this.type = Type.REWARD;
                break;
        }
    }

    public void setPriors(double epsilon, int lower, int upper) {
        this.initGraphEpsilon = epsilon;
        this.initLowerStrength = lower;
        this.initUpperStrength = upper;
    }

    public Experiment config(int max_episode_length, int iterations, int repetitions) {
        this.max_episode_length = max_episode_length;
        this.iterations = iterations;
        this.seed = repetitions;
        return this;
    }

    public Experiment config(int max_episode_length, int iterations, int repetitions, boolean optimizations) {
        this.max_episode_length = max_episode_length;
        this.iterations = iterations;
        this.seed = repetitions;
        this.optimizations = optimizations;
        return this;
    }

    public Experiment config(int max_episode_length, int iterations, int repetitions, int multiplier) {
        this.max_episode_length = max_episode_length;
        this.iterations = iterations;
        this.seed = repetitions;
        this.multiplier = multiplier;
        return this;
    }

    public Experiment config(int max_episode_length, int iterations, int repetitions, boolean optimizations, boolean tieParameters) {
        this.max_episode_length = max_episode_length;
        this.iterations = iterations;
        this.seed = repetitions;
        this.optimizations = optimizations;
        this.tieParameters = tieParameters;
        return this;
    }

    public Experiment config(int max_episode_length, int iterations, int repetitions, boolean optimizations, boolean tieParameters, int multiplier) {
        this.max_episode_length = max_episode_length;
        this.iterations = iterations;
        this.seed = repetitions;
        this.optimizations = optimizations;
        this.tieParameters = tieParameters;
        this.multiplier = multiplier;
        return this;
    }

    public Experiment config(int max_episode_length, int iterations, int repetitions, boolean optimizations, boolean tieParameters, int numTrainingMDPs, int numVerificationMDPs, int multiplier) {
        this.max_episode_length = max_episode_length;
        this.iterations = iterations;
        this.seed = repetitions;
        this.optimizations = optimizations;
        this.tieParameters = tieParameters;
        this.multiplier = multiplier;
        this.numTrainingMDPs = numTrainingMDPs;
        this.numVerificationMDPs = numVerificationMDPs;
        return this;
    }

    public Experiment config(int max_episode_length, int iterations, int repetitions, int lowerStrength, int upperStrength) {
        this.max_episode_length = max_episode_length;
        this.iterations = iterations;
        this.seed = repetitions;
        this.initLowerStrength = lowerStrength;
        this.initUpperStrength = upperStrength;
        return this;
    }

    public void setModelInfo(String modelInfo) {
        this.modelInfo = modelInfo;
    }

    public Experiment info(String experimentInfo) {
        this.experimentInfo = experimentInfo;
        return this;
    }

    public Experiment setStrengthBounds(int lowerStrengthBound, int upperStrengthBound, int maxMAPStrength) {
        this.lowerStrengthBound = lowerStrengthBound;
        this.upperStrengthBound = upperStrengthBound;
        this.maxMAPStrength = maxMAPStrength;
        return this;
    }

    public Experiment stratWeight(double weight) {
        this.strategyWeight = weight;
        return this;
    }

    public Experiment setErrorTol(double errorTolerance) {
        this.error_tolerance = errorTolerance;
        return this;
    }

    public Experiment setMultiplier(int multiplier) {
        this.multiplier = multiplier;
        return this;
    }

    public Experiment setSeed(int seed) {
        this.seed = seed;
        return this;
    }

    public void setTrueOpt(double opt) {
        this.trueOpt = opt;
    }

    public Experiment setTieParamters(boolean tieParameters) {
        this.tieParameters = tieParameters;
        return this;
    }

    public boolean resultIteration(int i) {
        return this.resultIterations.contains(i);
    }

    public void dumpConfiguration(String pathPrefix, String file_name) {

        try {
            FileWriter writer = new FileWriter(pathPrefix + file_name + ".yaml");
            writer.write("modelInfo: " + modelInfo + "\n");
            writer.write("model: " + model + "\n");
            writer.write("type: " + type + "\n");
            writer.write("goal: " + goal + "\n");
            writer.write("spec: " + spec + "\n");
            writer.write("robustSpec: " + robustSpec + "\n");
            writer.write("modelFile: " + modelFile + "\n");
            writer.write("dtmcSpec: " + dtmcSpec + "\n");
            writer.write("experimentInfo: " + experimentInfo + "\n");
            writer.write("seed: " + seed + "\n");
            writer.write("strategyWeight: " + strategyWeight + "\n");
            writer.write("iterations: " + iterations + "\n");
            writer.write("max_episode_length: " + max_episode_length + "\n");
            writer.write("error_tolerance: " + error_tolerance + "\n");
            writer.write("prefix: " + file_name + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Dump experiment setting to " + pathPrefix);
    }

    public void dumpConfiguration(String pathPrefix,
                                  String file_name,
                                  double minIMDP,
                                  double minMDP,
                                  double existential,
                                  double minRLIMDP,
                                  double minRLMDP,
                                  double totruntime,
                                  double runtimeper10k,
                                  double k0risk,
                                  double k5risk,
                                  double k10risk) {

        try {
            FileWriter writer = new FileWriter(pathPrefix + file_name + ".yaml");
            writer.write("modelInfo: " + modelInfo + "\n");
            writer.write("model: " + model + "\n");
            writer.write("type: " + type + "\n");
            writer.write("goal: " + goal + "\n");
            writer.write("spec: " + spec + "\n");
            writer.write("robustSpec: " + robustSpec + "\n");
            writer.write("modelFile: " + modelFile + "\n");
            writer.write("dtmcSpec: " + dtmcSpec + "\n");
            writer.write("experimentInfo: " + experimentInfo + "\n");
            writer.write("seed: " + seed + "\n");
            writer.write("strategyWeight: " + strategyWeight + "\n");
            writer.write("iterations: " + iterations + "\n");
            writer.write("max_episode_length: " + max_episode_length + "\n");
            writer.write("error_tolerance: " + error_tolerance + "\n");
            writer.write("prefix: " + file_name + "\n");
            writer.write("IMDP policy performance on true MDPs (J): " + minMDP + "\n");
            writer.write("IMDP policy performance on IMDPs (J̃): " + minIMDP + "\n");
            writer.write("RL policy performance on true MDPs (J): " + minRLMDP + "\n");
            writer.write("RL policy performance on IMDPs (J̃): " + minRLIMDP + "\n");
            writer.write("existential guarantee: " + existential + "\n");
            writer.write("empirical risk for k = 0: " + k0risk + "\n");
            writer.write("empirical risk for k = 5: " + k5risk + "\n");
            writer.write("empirical risk for k = 10: " + k10risk + "\n");
            writer.write("total runtime: " + totruntime + "sec\n");
            writer.write("runtime per 10k trajectories: " + runtimeper10k + "sec\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Dump experiment setting to " + pathPrefix);
    }

    public void dumpConfiguration(String pathPrefix, String file_name, String algorithm, Values values) {

        try {
            FileWriter writer = new FileWriter(pathPrefix + "/" + file_name + ".yaml");
            writer.write("modelInfo: " + modelInfo + "\n");
            writer.write("model: " + model + "\n");
            writer.write("type: " + type + "\n");
            writer.write("goal: " + goal + "\n");
            writer.write("spec: " + spec + "\n");
            writer.write("robustSpec: " + robustSpec + "\n");
            writer.write("modelFile: " + modelFile + "\n");
            writer.write("dtmcSpec: " + dtmcSpec + "\n");
            writer.write("experimentInfo: " + experimentInfo + "\n");
            writer.write("seed: " + seed + "\n");
            writer.write("initLowerStrength: " + initLowerStrength + "\n");
            writer.write("initUpperStrength: " + initUpperStrength + "\n");
            writer.write("lowerStrengthBound: " + lowerStrengthBound + "\n");
            writer.write("upperStrengthBound: " + upperStrengthBound + "\n");
            writer.write("strategyWeight: " + strategyWeight + "\n");
            writer.write("initGraphEpsilon: " + initGraphEpsilon + "\n");
            writer.write("iterations: " + iterations + "\n");
            writer.write("max_episode_length: " + max_episode_length + "\n");
            writer.write("alpha: " + alpha + "\n");
            writer.write("error_tolerance: " + error_tolerance + "\n");
            writer.write("trueOpt: " + trueOpt + "\n");
            writer.write("prefix: " + file_name + "\n");
            writer.write("algorithm: " + algorithm + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Dump experiment setting to " + pathPrefix);
    }

    public enum Type {
        REACH,
        REWARD
    }

    public enum Model {
        BETTING_GAME_FAVOURABLE,
        BETTING_GAME_UNFAVOURABLE,
        CHAIN_SMALL,
        CHAIN_SMALL2,
        CHAIN2,
        CHAIN_LARGE,
        CHAIN_LARGE_TWO_ACTION,
        CHAIN_LARGE2,
        CHAIN_test,
        GRID,
        GRID_SAFE_SE,
        TINY,
        TINY2,
        LOOP,
        AIRCRAFT,
        BANDIT,
        SAV2,
        CONSENSUS2,
        CONSENSUS4,
        BRP,
        NAND,
        DRONE_OLD,
        CROWD,
        DRONE,
        FIREWIRE
    }

    public enum InitialInterval {
        WIDE,
        UNIFORM,
    }


}
