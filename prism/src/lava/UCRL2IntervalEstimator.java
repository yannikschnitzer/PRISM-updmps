package lava;

import prism.Prism;

public class UCRL2IntervalEstimator extends PACIntervalEstimator {

    public UCRL2IntervalEstimator(Prism prism, Experiment ex) {
        super(prism, ex);
        this.name = "UCRL";
    }


    @Override
    protected Double confidenceInterval(TransitionTriple t) {
        return computeUCRL2Bound(t);
    }

    private Double computeUCRL2Bound(TransitionTriple t) {
        // ensure variable names match the UCRL2 paper definition
        double delta = error_tolerance;
        int S = mdp.getNumStates();
        int A = mdp.getNumChoices(t.getState());

        int tk = getTotalTransitionCount();        // samples in total processed
        int Nksa = getStateActionCount(t.getStateAction());            // samples in total for (s,a)

        //System.out.println("Triple" + t + " Num States " + S + " Num Choices " + A + " Total Transition count: " + tk + " State action count: " + Nksa + " Interval Size:" + Math.sqrt((14*S*(Math.log(2*A)+ Math.log(tk) - Math.log(delta)))/(Math.max(1, Nksa))));
        return Math.sqrt((14 * S * (Math.log(2 * A) + Math.log(tk) - Math.log(delta))) / (Math.max(1, Nksa)));
    }


}
