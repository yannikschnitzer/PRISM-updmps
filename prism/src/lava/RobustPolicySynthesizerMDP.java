package lava;

import common.Interval;
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

public class RobustPolicySynthesizerMDP {

    protected ModulesFile modulesFileIMDP;
    protected ModulesFile modulesFileMDP;
    // Parametric MDP defining the structure of the underlying problem
    private MDP<Function> mdpParam;
    private IMDP<Double> combinedIMDP;
    // List of MDPs to extract the robust policy for
    private List<MDP<Double>> mdps = new ArrayList<>();
    // Subset of imdps used for verification
    private List<MDP<Double>> verificationSet = new ArrayList<>();
    private Experiment experiment;

    public RobustPolicySynthesizerMDP(MDP<Function> mdpParam, Experiment ex) {
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

    public IMDP<Double> combineMDPs() {
        int numStates = this.mdpParam.getNumStates();
        IMDPSimple<Double> combinedIMPD = new IMDPSimple<>(numStates);
        combinedIMPD.addInitialState(mdpParam.getFirstInitialState());
        combinedIMPD.setStatesList(mdpParam.getStatesList());
        combinedIMPD.setConstantValues(mdpParam.getConstantValues());
        combinedIMPD.setIntervalEvaluator(Evaluator.forDoubleInterval());

        // Iterate over learned IMDPs to build intervals for each transition
        Map<TransitionTriple, Interval<Double>> transitionMap = new HashMap<>();

        for (MDP<Double> mdp : this.mdps) {
            for (int s = 0; s < numStates; s++) {
                int numChoices = mdp.getNumChoices(s);
                for (int i = 0; i < numChoices; i++) {
                    final String action = getActionString(mdp, s, i);
                    mdp.forEachDoubleTransition(s, i, (int sFrom, int sTo, double j) -> {
                        TransitionTriple t = new TransitionTriple(sFrom, action, sTo);
                        if (transitionMap.containsKey(t)) {
                            transitionMap.put(t, mergeIntervals(transitionMap.get(t), new Interval<>(j, j)));
                        } else {
                            transitionMap.put(t, new Interval<>(j, j));
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
        mc.setMaxIters(100000);

        buildModulesFiles(prism);
        ModulesFileModelGenerator<?> modelGen = ModulesFileModelGenerator.create(modulesFileIMDP, prism);
        modelGen.setSomeUndefinedConstants(this.mdpParam.getConstantValues());

        PropertiesFile pf = prism.parsePropertiesString(spec);
        Expression exprTarget = pf.getProperty(0);

        mc.setModelCheckingInfo(modelGen, pf, modelGen);

        Result result = mc.check(this.combinedIMDP, exprTarget);

        MDStrategy<Double> strat = (MDStrategy<Double>) result.getStrategy();
        //System.out.println("Robust Strategy MDPs:" + strat);
        //System.out.println("Robust Performance MDPs:" + result.getResult());

        return strat;
    }

    public List<Double> checkVerificationSet(Prism prism, MDStrategy<Double> strategy, String spec) throws PrismException {
        List<Double> results = new ArrayList<>();
        for (MDP<Double> mdp : this.verificationSet) {
            DTMC<Double> inducedDTMC = (DTMC<Double>) mdp.constructInducedModel(strategy);
            DTMCModelChecker mc = new DTMCModelChecker(prism);

            mc.setErrorOnNonConverge(false);
            mc.setGenStrat(true);
            mc.setPrecomp(true);
            PropertiesFile pf = prism.parsePropertiesString(spec);

            buildModulesFiles(prism);
            ModulesFile modulesFileDTMC = (ModulesFile) modulesFileIMDP.deepCopy();
            modulesFileDTMC.setModelType(ModelType.DTMC);
            ModulesFileModelGenerator<?> modelGen = ModulesFileModelGenerator.create(modulesFileDTMC, prism);
            modelGen.setSomeUndefinedConstants(mdp.getConstantValues());
            RewardGeneratorMDStrat<?> rewGen = new RewardGeneratorMDStrat(modelGen, mdp, strategy);

            mc.setModelCheckingInfo(modelGen, pf, rewGen);

            Result result = mc.check(inducedDTMC, pf.getProperty(0));
            results.add((double) result.getResult());
        }

        return results;
    }

    public List<Double> checkVerificatonSetRLPolicy(Prism prism, String spec, int iteration) throws PrismException {
        List<Double> results = new ArrayList<>();
        for (MDP<Double> mdp : this.verificationSet) {
            PolicyLoader p = new PolicyLoader();

            MRStrategy rlStrat;
            rlStrat = switch (experiment.model) {
                case BETTING_GAME_FAVOURABLE ->
                        p.loadBettingPolicy(String.format("policies/betting/betting_policies_onemore/policy_single_%d.json", (iteration)), mdp);
                case AIRCRAFT ->
                        p.loadAircraftPolicy(String.format("policies/aircraft/aircraft_policies/policy_single_%d.json",(iteration)),mdp);
                case SAV2 ->
                        p.loadSavPolicy(String.format("policies/sav/sav_policies/policy_single_%d.json",(iteration)),mdp);
                case CHAIN_LARGE_TWO_ACTION ->
                        p.loadChainPolicy(String.format("policies/chain_twoact/chain_policies_extended/policy_single_%d.json",(iteration)), mdp);
                case DRONE ->
                        p.loadDronePolicy(String.format("policies/drone/drone_policies/policy_single_%d.json",(iteration)), mdp);
                default -> throw new PrismException("Unsupported model type: " + experiment.model);
            };
            //MRStrategy rlStrat = p.loadAircraftPolicy("policies/aircraft/policy.json", mdp);
            //MRStrategy rlStrat = p.loadBettingPolicy(String.format("policies/betting/betting_policies_onemore/policy_single_%d.json",(iteration)),mdp);
            //MRStrategy rlStrat = p.loadAircraftPolicy(String.format("policies/aircraft/aircraft_policies/policy_single_%d.json",(iteration)),mdp);
            //MRStrategy rlStrat = p.loadChainPolicy(String.format("policies/chain/chain_policies_three_extended/policy_single_%d.json",(iteration)), mdp);
            //MRStrategy rlStrat = p.loadChainPolicy(String.format("policies/chain_twoact/chain_policies_extended/policy_single_%d.json",(iteration)), mdp);
            //MRStrategy rlStrat = p.loadDronePolicy(String.format("policies/drone/drone_policies/policy_single_%d.json",(iteration)), mdp);
            //MRStrategy rlStrat = p.loadFirewirePolicy(String.format("policies/firewire/firewire_policies/policy_single_%d.json",(iteration)), mdp);

            StrategyExportOptions options = new StrategyExportOptions();
            options.setMode(StrategyExportOptions.InducedModelMode.REDUCE);
            DTMC<Double> inducedDTMC = (DTMC<Double>) rlStrat.constructInducedModel(options);

            DTMCModelChecker mc = new DTMCModelChecker(prism);
            mc.setErrorOnNonConverge(false);
            mc.setGenStrat(true);
            mc.setPrecomp(true);
            PropertiesFile pf = prism.parsePropertiesString(spec);

            buildModulesFiles(prism);
            ModulesFile modulesFileDTMC = (ModulesFile) modulesFileIMDP.deepCopy();
            modulesFileDTMC.setModelType(ModelType.DTMC);
            ModulesFileModelGenerator<Double> modelGen = (ModulesFileModelGenerator<Double>) ModulesFileModelGenerator.create(modulesFileDTMC, prism);
            modelGen.setSomeUndefinedConstants(mdp.getConstantValues());
            RewardGeneratorMRStrat<Double> rewGen = new RewardGeneratorMRStrat<>(modelGen, mdp, rlStrat);

            mc.setModelCheckingInfo(modelGen, pf, rewGen);

            Result result = mc.check(inducedDTMC, pf.getProperty(0));
            results.add((double) result.getResult());

            //System.out.println("Induced Model: " + rlStrat.constructInducedModel(options));

        }

        return results;
    }

    private Interval<Double> mergeIntervals(Interval<Double> i1, Interval<Double> i2) {
        return new Interval<>(Math.min(i1.getLower(), i2.getLower()), Math.max(i1.getUpper(), i2.getUpper()));
    }

    public void addMDP(MDP<Double> mdp) {
        this.mdps.add(mdp);
    }

    public void addMDPs(List<MDP<Double>> mdps) {
        this.mdps.addAll(mdps);
    }

    public void addVerificationMDP(MDP<Double> mdp) {
        this.verificationSet.add(mdp);
    }

    public void addVerificatonMDPs(List<MDP<Double>> mdps) {
        this.verificationSet.addAll(mdps);
    }

    public List<MDP<Double>> getMDPs() {
        return mdps;
    }

    public void setMDPs(List<MDP<Double>> imdps) {
        this.mdps = imdps;
    }

    public List<MDP<Double>> getVerificationSet() {
        return this.verificationSet;
    }

    public void setVerificationSet(List<MDP<Double>> verificationSet) {
        this.verificationSet = verificationSet;
    }

    public MDP<Function> getMdpParam() {
        return mdpParam;
    }

    public void setMdpParam(MDP<Function> mdpParam) {
        this.mdpParam = mdpParam;
    }

    public String getActionString(MDP<?> mdp, int s, int i) {
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

    public Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
    }
}
