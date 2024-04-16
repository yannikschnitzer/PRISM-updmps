package lava;

import common.Interval;
import explicit.*;
import param.Function;
import parser.ast.Expression;
import parser.ast.PropertiesFile;
import prism.Evaluator;
import prism.Prism;
import prism.PrismException;
import prism.Result;
import strat.MDStrategy;

import java.util.*;

public class RobustPolicySynthesizerMDP {

    // Parametric MDP defining the structure of the underlying problem
    private MDP<Function> mdpParam;

    private IMDP<Double> combinedIMDP;

    // List of MDPs to extract the robust policy for
    private List<MDP<Double>> mdps = new ArrayList<>();

    // Subset of imdps used for verification
    private List<MDP<Double>> verificationSet = new ArrayList<>();

    public RobustPolicySynthesizerMDP(MDP<Function> mdpParam) {
           this.mdpParam = mdpParam;
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
                            transitionMap.put(t, new Interval<>(j,j));
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

                mdpParam.forEachTransition(s, i, (int sFrom, int sTo, Function p)->{
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

        PropertiesFile pf = prism.parsePropertiesString(spec);
        Expression exprTarget = pf.getProperty(0);

        Result result = mc.check(this.combinedIMDP, exprTarget);

        MDStrategy<Double> strat = (MDStrategy<Double>) result.getStrategy();
        System.out.println("Robust Strategy MDPs:" + strat);
        System.out.println("Robust Performance MDPs:" + result.getResult());

        return strat;
    }

    public List<Double> checkVerificationSet(Prism prism, MDStrategy<Double> strategy, String spec) throws PrismException {
        List<Double> results = new ArrayList<>();
        for (MDP<Double> mdp : this.verificationSet) {
            DTMC<Double> inducedDTMC = (DTMC<Double>) mdp.constructInducedModel(strategy);
            DTMCModelChecker mc = new DTMCModelChecker(prism);
            mc.setErrorOnNonConverge(false);
            mc.setGenStrat(true);
            PropertiesFile pf = prism.parsePropertiesString(spec);
            Result result = mc.check(inducedDTMC, pf.getProperty(0));
            results.add((double) result.getResult());
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

    public MDP<Function> getMdpParam() {
        return mdpParam;
    }

    public void setMdpParam(MDP<Function> mdpParam) {
        this.mdpParam = mdpParam;
    }

    public String getActionString(MDP<?> mdp, int s, int i) {
        String action = (String) mdp.getAction(s,i);
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
