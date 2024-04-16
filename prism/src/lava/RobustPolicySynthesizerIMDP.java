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

public class RobustPolicySynthesizerIMDP {

    // Parametric MDP defining the structure of the underlying problem
    private MDP<Function> mdpParam;

    private IMDP<Double> combinedIMDP;

    // List of IMDPs to extract the robust policy for
    private List<IMDP<Double>> imdps = new ArrayList<>();

    // Subset of imdps used for verification
    private List<IMDP<Double>> verificationSet = new ArrayList<>();

    public RobustPolicySynthesizerIMDP(MDP<Function> mdpParam) {
           this.mdpParam = mdpParam;
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
        System.out.println("Robust Strategy IMDPs:" + strat);
        System.out.println("Robust Performance IMDPs:" + result.getResult());

        return strat;
    }

    public List<Double> checkVerificationSet(Prism prism, MDStrategy<Double> strategy, String spec) throws PrismException {
        List<Double> results = new ArrayList<>();
        for (IMDP<Double> imdp : this.verificationSet) {
            IDTMC<Double> inducedIDTMC = imdp.constructInducedIDTMC(strategy);
            IDTMCModelChecker mc = new IDTMCModelChecker(prism);
            mc.setErrorOnNonConverge(false);
            mc.setGenStrat(true);
            PropertiesFile pf = prism.parsePropertiesString(spec);
            Result result = mc.check(inducedIDTMC, pf.getProperty(0));
            results.add((double) result.getResult());
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
        String action = (String) imdp.getAction(s,i);
        if (action == null) {
            action = "_empty";
        }
        return action;
    }

    public String getActionString(MDP<Function> mdp, int s, int i) {
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
