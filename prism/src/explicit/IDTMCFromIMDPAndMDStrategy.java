package explicit;

import common.Interval;
import explicit.rewards.MCRewards;
import parser.State;
import parser.Values;
import prism.PrismException;
import prism.PrismNotSupportedException;
import strat.MDStrategy;

import java.util.*;

public class IDTMCFromIMDPAndMDStrategy<Value> extends IDTMCSimple<Value>{
    // Parent IMDP
    protected IMDP<Value> imdp;
    // MD strategy
    protected MDStrategy<?> strat;

    /**
     * Constructor: create from MDP and memoryless adversary.
     */
    public IDTMCFromIMDPAndMDStrategy(IMDP<Value> imdp, MDStrategy<?> strat)
    {
        this.imdp = imdp;
        this.numStates = imdp.getNumStates();
        this.strat = strat;
        numTransitions = imdp.getNumTransitions();
    }

    @Override
    public void buildFromPrismExplicit(String filename) throws PrismException
    {
        throw new PrismNotSupportedException("Not supported");
    }

    // Accessors (for Model)

    public int getNumStates()
    {
        return imdp.getNumStates();
    }

    public int getNumInitialStates()
    {
        return imdp.getNumInitialStates();
    }

    public Iterable<Integer> getInitialStates()
    {
        return imdp.getInitialStates();
    }

    public int getFirstInitialState()
    {
        return imdp.getFirstInitialState();
    }

    public boolean isInitialState(int i)
    {
        return imdp.isInitialState(i);
    }

    public boolean isDeadlockState(int i)
    {
        return imdp.isDeadlockState(i);
    }

    public List<State> getStatesList()
    {
        return imdp.getStatesList();
    }

    public Values getConstantValues()
    {
        return imdp.getConstantValues();
    }

    public int getNumTransitions(int s)
    {
        return strat.isChoiceDefined(s) ? imdp.getNumTransitions(s, strat.getChoiceIndex(s)) : 0;
    }

   @Override
   public Iterator<Integer> getSuccessorsIterator(int s)
   {
       SuccessorsIterator successors = getSuccessors(s);
       return successors.distinct();
   }

    public SuccessorsIterator getSuccessors(final int s)
    {
        if (strat.isChoiceDefined(s)) {
            return imdp.getSuccessors(s, strat.getChoiceIndex(s));
        } else {
            return SuccessorsIterator.empty();
        }
    }

    public int getNumChoices(int s)
    {
        // Always 1 for a DTMC
        return 1;
    }

    public void findDeadlocks(boolean fix) throws PrismException
    {
        // No deadlocks by definition
    }

    public void checkForDeadlocks() throws PrismException
    {
        // No deadlocks by definition
    }

    public void checkForDeadlocks(BitSet except) throws PrismException
    {
        // No deadlocks by definition
    }

    // Accessors (for DTMC)

    public Iterator<Map.Entry<Integer, Interval<Value>>> getTransitionsIterator(int s)
    {
        if (strat.isChoiceDefined(s)) {
            return imdp.getTransitionsIterator(s, strat.getChoiceIndex(s));
        } else {
            // Empty iterator
            Map<Integer, Interval<Value>> empty = Collections.emptyMap();
            return empty.entrySet().iterator();
        }
    }

    public void forEachTransition(int s, IMDP.TransitionConsumer<Value> c)
    {
        if (!strat.isChoiceDefined(s)) {
            return;
        }
        imdp.forEachTransition(s, strat.getChoiceIndex(s), c::accept);
    }

    @Override
    public BitSet getLabelStates(String name)
    {
        return this.imdp.getLabelStates(name);
    }

    @Override
    public void vmMult(double vect[], double result[])
    {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public String toString()
    {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public boolean equals(Object o)
    {
        throw new RuntimeException("Not implemented yet");
    }
}

