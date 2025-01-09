package lava;

import common.Interval;
import explicit.Model;
import explicit.*;
import param.Function;
import parser.ast.Expression;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import prism.*;
import simulator.ModulesFileModelGenerator;
import strat.MDStrategy;
import strat.MRStrategy;
import strat.StrategyExportOptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import static lava.Experiment.Model.BETTING_GAME_FAVOURABLE;

public class RobustPolicySynthesizerIMDP {

    protected ModulesFile modulesFileIMDP;
    protected ModulesFile modulesFileMDP;
    // Parametric MDP defining the structure of the underlying problem
    private MDP<Function> mdpParam;
    private IMDP<Double> combinedIMDP;
    // List of IMDPs to extract the robust policy for
    private List<IMDP<Double>> imdps = new ArrayList<>();
    // Subset of imdps used for verification
    private List<IMDP<Double>> verificationSet = new ArrayList<>();
    private final Experiment experiment;

    public RobustPolicySynthesizerIMDP(MDP<Function> mdpParam, Experiment ex) {
        this.mdpParam = mdpParam;
        this.experiment = ex;
    }

    private void buildModulesFiles(Prism prism) {
        try {
            this.modulesFileIMDP = prism.parseModelFile(new File(this.experiment.modelFile), ModelType.IMDP);
            this.modulesFileMDP = prism.parseModelFile(new File(experiment.modelFile), ModelType.MDP);
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

    public IMDP<Double> combineIMDPs() {
        int numStates = this.mdpParam.getNumStates();
        IMDPSimple<Double> combinedIMPD = new IMDPSimple<>(numStates);
        combinedIMPD.addInitialState(mdpParam.getFirstInitialState());
        combinedIMPD.setStatesList(mdpParam.getStatesList());
        combinedIMPD.setConstantValues(mdpParam.getConstantValues());
        combinedIMPD.setIntervalEvaluator(Evaluator.forDoubleInterval());

        // Iterate over learned IMDPs to build intervals for each transition
        Map<TransitionTriple, Interval<Double>> transitionMap = new HashMap<>();

        for (IMDP<Double> imdp : this.imdps) {
            for (int s = 0; s < numStates; s++) {
                int numChoices = imdp.getNumChoices(s);
                for (int i = 0; i < numChoices; i++) {
                    final String action = getActionString(imdp, s, i);
                    imdp.forEachDoubleIntervalTransition(s, i, (int sFrom, int sTo, Interval<Double> j) -> {
                        TransitionTriple t = new TransitionTriple(sFrom, action, sTo);
                        if (transitionMap.containsKey(t)) {
                            transitionMap.put(t, mergeIntervals(transitionMap.get(t), j));
                        } else {
                            transitionMap.put(t, j);
                        }
                    });
                }
            }
        }

        // Build a single IMDP containing all learned IMDPs
        for (int s = 0; s < numStates; s++) {
            int numChoices = mdpParam.getNumChoices(s);
            final int state = s;
            for (int i = 0; i < numChoices; i++) {
                final String action = getActionString(mdpParam, s, i);
                Distribution<Interval<Double>> distrNew = new Distribution<>(Evaluator.forDoubleInterval());

                mdpParam.forEachTransition(s, i, (int sFrom, int sTo, Function p) -> {
                    TransitionTriple t = new TransitionTriple(state, action, sTo);
                    distrNew.add(sTo, transitionMap.get(t));
                });

                combinedIMPD.addActionLabelledChoice(s, distrNew, action);
            }
        }

        Map<String, BitSet> labels = mdpParam.getLabelToStatesMap();
        for (Map.Entry<String, BitSet> entry : labels.entrySet()) {
            combinedIMPD.addLabel(entry.getKey(), entry.getValue());
        }

        this.combinedIMDP = combinedIMPD;
        return combinedIMPD;
    }

    public MDStrategy<Double> getRobustStrategy(Prism prism, String spec) throws PrismException {
        UMDPModelChecker mc = new UMDPModelChecker(prism);
        mc.setGenStrat(true);
        mc.setErrorOnNonConverge(false);
        mc.setPrecomp(true);
        mc.setMaxIters(100000);

        buildModulesFiles(prism);
        ModulesFileModelGenerator<?> modelGen = ModulesFileModelGenerator.create(modulesFileIMDP, prism);
        modelGen.setSomeUndefinedConstants(this.mdpParam.getConstantValues());

        PropertiesFile pf = prism.parsePropertiesString(spec);
        Expression exprTarget = pf.getProperty(0);

        mc.setModelCheckingInfo(modelGen, pf, modelGen);

        Result result = mc.check(this.combinedIMDP, exprTarget);
        //System.out.println("Result: " + result.getResultString());

        MDStrategy<Double> strat = (MDStrategy<Double>) result.getStrategy();
//        System.out.println("Robust Strategy IMDPs:" + strat);
//        System.out.println("Robust Performance IMDPs:" + result.getResult());

        return strat;
    }

    public List<Double> checkVerificationSet(Prism prism, MDStrategy<Double> strategy, String spec) throws PrismException {
        List<Double> results = new ArrayList<>();
        for (IMDP<Double> imdp : this.verificationSet) {
            IDTMC<Double> inducedIDTMC = imdp.constructInducedIDTMC(strategy);
            IDTMCModelChecker mc = new IDTMCModelChecker(prism);

            mc.setErrorOnNonConverge(false);
            mc.setGenStrat(true);
            mc.setPrecomp(false);
            PropertiesFile pf = prism.parsePropertiesString(spec);

            buildModulesFiles(prism);
            ModulesFile modulesFileIDTMC = (ModulesFile) modulesFileIMDP.deepCopy();
            modulesFileIDTMC.setModelType(ModelType.IDTMC);
            ModulesFileModelGenerator<?> modelGen = ModulesFileModelGenerator.create(modulesFileIDTMC, prism);
            modelGen.setSomeUndefinedConstants(imdp.getConstantValues());
            RewardGeneratorMDStrat<?> rewGen = new RewardGeneratorMDStrat(modelGen, imdp, strategy);

            mc.setModelCheckingInfo(modelGen, pf, rewGen);

            Result result = mc.check(inducedIDTMC, pf.getProperty(0));
            results.add((double) result.getResult());
        }

        return results;
    }

    public List<Double> checkVerificatonSetRLPolicy(Prism prism, String spec, int iteration) throws PrismException {
        List<Double> results = new ArrayList<>();
        for (IMDP<Double> imdp : this.verificationSet) {
            PolicyLoader p = new PolicyLoader();

            MRStrategy rlStrat;
            rlStrat = switch (experiment.model) {
                case BETTING_GAME_FAVOURABLE ->
                        p.loadBettingPolicy(String.format("policies/betting/betting_policies_onemore/policy_single_%d.json", (iteration)), imdp);
                case AIRCRAFT ->
                        p.loadAircraftPolicy(String.format("policies/aircraft/aircraft_policies/policy_single_%d.json",(iteration)),imdp);
                case SAV2 ->
                        p.loadSavPolicy(null, imdp); // SAV Policy was uniform after RL training
                case CHAIN_LARGE_TWO_ACTION ->
                        p.loadChainPolicy(String.format("policies/chain_twoact/chain_policies_extended/policy_single_%d.json",(iteration)), imdp);
                case DRONE ->
                        p.loadDronePolicy(String.format("policies/drone/drone_policies/policy_single_%d.json",(iteration)), imdp);
                default -> throw new PrismException("Unsupported model type: " + experiment.model);
            };
//            if (experiment.model == Experiment.Model.BETTING_GAME_FAVOURABLE) {
//                //MRStrategy rlStrat = p.loadAircraftPolicy("policies/aircraft/policy.json", imdp);
//                //MRStrategy rlStrat = p.loadAircraftPolicy(String.format("policies/aircraft/aircraft_policies/policy_single_%d.json",(iteration)),imdp);
//                //MRStrategy rlStrat = p.loadChainPolicy(String.format("policies/chain/chain_policies_three_extended/policy_single_%d.json",(iteration)), imdp);
//                //MRStrategy rlStrat = p.loadChainPolicy(String.format("policies/chain_twoact/chain_policies_extended/policy_single_%d.json",(iteration)), imdp);
//                //MRStrategy rlStrat = p.loadDronePolicy(String.format("policies/drone/drone_policies/policy_single_%d.json",(iteration)), imdp);
//                //MRStrategy rlStrat = p.loadFirewirePolicy(String.format("policies/firewire/firewire_policies/policy_single_%d.json",(iteration)), imdp);
//            }

            StrategyExportOptions options = new StrategyExportOptions();
            options.setMode(StrategyExportOptions.InducedModelMode.REDUCE);
            Model<Double> inducedIDTMC = rlStrat.constructInducedModel(options);

            IDTMCModelChecker mc = new IDTMCModelChecker(prism);
            mc.setErrorOnNonConverge(false);
            mc.setGenStrat(true);
            PropertiesFile pf = prism.parsePropertiesString(spec);

            buildModulesFiles(prism);
            ModulesFile modulesFileIDTMC = (ModulesFile) modulesFileIMDP.deepCopy();
            modulesFileIDTMC.setModelType(ModelType.IDTMC);
            ModulesFileModelGenerator<Double> modelGen = (ModulesFileModelGenerator<Double>) ModulesFileModelGenerator.create(modulesFileIDTMC, prism);
            modelGen.setSomeUndefinedConstants(imdp.getConstantValues());
            RewardGeneratorMRStrat<Double> rewGen = new RewardGeneratorMRStrat<>(modelGen, imdp, rlStrat);

            mc.setModelCheckingInfo(modelGen, pf, rewGen);

            Result result = mc.check(inducedIDTMC, pf.getProperty(0));
            results.add((double) result.getResult());

            //System.out.println("Induced IDTMC Model: " + rlStrat.constructInducedModel(options));
        }

        return results;
    }

    private Interval<Double> mergeIntervals(Interval<Double> i1, Interval<Double> i2) {
        return new Interval<>(Math.min(i1.getLower(), i2.getLower()), Math.max(i1.getUpper(), i2.getUpper()));
    }

    public void addIMDP(IMDP<Double> imdp) {
        this.imdps.add(imdp);
    }

    public void addIMDPs(List<IMDP<Double>> imdps) {
        this.imdps.addAll(imdps);
    }

    public void addVerificationIMDP(IMDP<Double> imdp) {
        this.verificationSet.add(imdp);
    }

    public void addVerificatonIMDPs(List<IMDP<Double>> imdps) {
        this.verificationSet.addAll(imdps);
    }

    public List<IMDP<Double>> getVerificationSet() {
        return this.verificationSet;
    }

    public void setVerificationSet(List<IMDP<Double>> verificationSet) {
        this.verificationSet = verificationSet;
    }

    public List<IMDP<Double>> getImdps() {
        return imdps;
    }

    public void setImdps(List<IMDP<Double>> imdps) {
        this.imdps = imdps;
    }

    public MDP<Function> getMdpParam() {
        return mdpParam;
    }

    public void setMdpParam(MDP<Function> mdpParam) {
        this.mdpParam = mdpParam;
    }

    public String getActionString(IMDP<Double> imdp, int s, int i) {
        String action = (String) imdp.getAction(s, i);
        if (action == null) {
            action = "_empty";
        }
        return action;
    }

    public String getActionString(MDP<Function> mdp, int s, int i) {
        String action = (String) mdp.getAction(s, i);
        if (action == null) {
            action = "_empty";
        }
        return action;
    }

    public IMDP<Double> getCombinedIMDP() {
        return combinedIMDP;
    }

    public void setCombinedIMDP(IMDP<Double> combinedIMDP) {
        this.combinedIMDP = combinedIMDP;
    }
}
