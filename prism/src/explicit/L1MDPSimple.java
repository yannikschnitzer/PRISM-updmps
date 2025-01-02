package explicit;

import prism.Evaluator;
import prism.PrismException;
import strat.MDStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class L1MDPSimple<Value> extends ModelExplicit<Value> implements NondetModelSimple<Value>, L1MDP<Value> {

    // Center MDP - Corresponding to the center (empirical) distribution
    protected MDPSimple<Value> mdp;
    protected List<List<Double>> radii = new ArrayList<>();

    // Constructors

    public L1MDPSimple() {
        mdp = new MDPSimple<>();
        createDefaultEvaluatorForMDP();
        initialise(0);
    }

    public L1MDPSimple(int numStates) {
        mdp = new MDPSimple<>(numStates);
        createDefaultEvaluatorForMDP();
        initialise(numStates);
    }

    /**
     * Add a default (double interval) evaluator to the MDP
     */
    private void createDefaultEvaluatorForMDP() {
        ((L1MDPSimple<Double>) this).setEvaluator(Evaluator.forDouble());
    }

    public void setEvaluator(Evaluator<Value> evaluator) {
        this.mdp.setEvaluator(evaluator);
    }

    public void addActionLabelledChoice(int s, Distribution<Value> distr, double radius, Object action) {
        int success = mdp.addActionLabelledChoice(s, distr, action);
        if (success != -1) {
            radii.get(s).add(radius);
        }
    }

    @Override
    public void buildFromPrismExplicit(String filename) throws PrismException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void setRadius(int s, int a, double r) {
        radii.get(s).set(a, r);
    }

    @Override
    public void clearState(int s) {
        mdp.clearState(s);
    }

    @Override
    public int addState() {
        addStates(1);
        return numStates - 1;
    }

    @Override
    public void addStates(int numToAdd) {
        mdp.addStates(numToAdd);
        numStates += numToAdd;
        for (int i = 0; i < numToAdd; i++) {
            radii.add(new ArrayList<>());
        }
    }

    @Override
    public void initialise(int numStates) {
        mdp.initialise(numStates);
        super.initialise(numStates);
        for (int i = 0; i < numStates; i++) {
            radii.add(new ArrayList<>());
        }
    }

    @Override
    public void findDeadlocks(boolean fix) throws PrismException {
        mdp.findDeadlocks(fix);
    }

    @Override
    public void checkForDeadlocks(BitSet except) throws PrismException {
        mdp.checkForDeadlocks(except);
    }

    @Override
    public double mvMultUncSingle(int s, int k, double[] vect, MinMax minMax) {
        return 0;
    }

    @Override
    public int getNumChoices(int s) {
        return mdp.getNumChoices(s);
    }

    @Override
    public Object getAction(int s, int i) {
        return mdp.getAction(s, i);
    }

    @Override
    public int getNumTransitions(int s, int i) {
        return mdp.getNumTransitions(s, i);
    }

    @Override
    public SuccessorsIterator getSuccessors(int s, int i) {
        return mdp.getSuccessors(s, i);
    }

    @Override
    public Model<Value> constructInducedModel(MDStrategy<Value> strat) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void checkLowerBoundsArePositive() throws PrismException {
        ;
    }

    public void doSomething(){

    }
    @Override
    public String toString() {
        String s = "";
        s = "[ ";

        for (int i = 0; i < numStates; i++) {
            if (i > 0) {
                s += ", ";
            }
            s += i + ": ";
            s += "[";
            int n = getNumChoices(i);
            for (int j = 0; j < n; j++) {
                if (j > 0) {
                    s += ",";
                }
                Object o = mdp.getAction(i, n);
                if (o != null) {
                    s += o + ":";
                }
                s += mdp.trans.get(i).get(j);
                s += ", Radius: ";
                s += radii.get(i).get(j);
            }
            s += "]";
        }
        s += " ]\n";
        return s;
    }

}
