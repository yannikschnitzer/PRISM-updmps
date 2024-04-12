//==============================================================================
//	
//	Copyright (c) 2020-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
//	* Alberto Puggelli <alberto.puggelli@gmail.com>
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package explicit;

import common.Interval;
import parser.State;
import prism.Evaluator;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismNotSupportedException;
import strat.MDStrategy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Simple explicit-state representation of an IMDP.
 */
public class IMDPSimple<Value> extends ModelExplicit<Value> implements NondetModelSimple<Value>, IMDP<Value> {
	// IMDP transitions stored internally as an MDP over intervals
	protected MDPSimple<Interval<Value>> mdp;

	// Constructors

	/**
	 * Constructor: empty IMDP.
	 */
	public IMDPSimple() {
		mdp = new MDPSimple<>();
		createDefaultEvaluatorForMDP();
		initialise(0);
	}

	/**
	 * Constructor: new IMDP with fixed number of states.
	 */
	public IMDPSimple(int numStates) {
		mdp = new MDPSimple<>(numStates);
		createDefaultEvaluatorForMDP();
		initialise(numStates);
	}

	/**
	 * Copy constructor.
	 */
	public IMDPSimple(IMDPSimple<Value> imdp) {
		this(imdp.numStates);
		mdp = new MDPSimple<>(imdp.mdp);
		copyFrom(imdp);
	}

	/**
	 * Construct an IMDP from an existing one and a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 * Pointer to states list is NOT copied (since now wrong).
	 * Note: have to build new Distributions from scratch anyway to do this,
	 * so may as well provide this functionality as a constructor.
	 */
	public IMDPSimple(IMDPSimple<Value> imdp, int permut[]) {
		this(imdp.numStates);
		mdp = new MDPSimple<>(imdp.mdp, permut);
		copyFrom(imdp, permut);
	}

	/**
	 * Add a default (double interval) evaluator to the MDP
	 */
	private void createDefaultEvaluatorForMDP() {
		((IMDPSimple<Double>) this).setIntervalEvaluator(Evaluator.forDoubleInterval());
	}

	// Mutators (for ModelSimple)

	@Override
	public void initialise(int numStates) {
		mdp.initialise(numStates);
		super.initialise(numStates);
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
	}

	@Override
	public void buildFromPrismExplicit(String filename) throws PrismException {
		int lineNum = 0;
		// Open file for reading, automatic close
		try (BufferedReader in = new BufferedReader(new FileReader(new File(filename)))) {
			// Parse first line to get num states
			String info = in.readLine();
			lineNum = 1;
			if (info == null) {
				throw new PrismException("Missing first line of .tra file");
			}
			String[] infos = info.split(" ");
			if (infos.length < 3) {
				throw new PrismException("First line of .tra file must read #states, #choices, #transitions");
			}
			int n = Integer.parseInt(infos[0]);
			int expectedNumChoices = Integer.parseInt(infos[1]);
			int expectedNumTransitions = Integer.parseInt(infos[2]);

			int emptyDistributions = 0;

			// Initialise
			initialise(n);
			// Go though list of transitions in file
			String s = in.readLine();
			lineNum++;
			while (s != null) {
				s = s.trim();
				if (s.length() > 0) {
					String[] transition = s.split(" ");
					int source = Integer.parseInt(transition[0]);
					int choice = Integer.parseInt(transition[1]);
					int target = Integer.parseInt(transition[2]);
					Interval<Value> prob = getIntervalEvaluator().fromString(transition[3]);

					if (source < 0 || source >= numStates) {
						throw new PrismException("Problem in .tra file (line " + lineNum + "): illegal source state index " + source);
					}
					if (target < 0 || target >= numStates) {
						throw new PrismException("Problem in .tra file (line " + lineNum + "): illegal target state index " + target);
					}

					// ensure distributions for all choices up to choice (inclusive) exist
					// this potentially creates empty distributions that are never defined
					// so we keep track of the number of distributions that are still empty
					// and provide an error message if there are still empty distributions
					// after having read the full .tra file
					while (choice >= getNumChoices(source)) {
						addChoice(source, new Distribution<>(getIntervalEvaluator()));
						emptyDistributions++;
					}

					if (mdp.trans.get(source).get(choice).isEmpty()) {
						// was empty distribution, becomes non-empty below
						emptyDistributions--;
					}
					// add transition
					if (!mdp.trans.get(source).get(choice).add(target, prob)) {
						mdp.numTransitions++;
					} else {
						throw new PrismException("Problem in .tra file (line " + lineNum + "): redefinition of probability for " + source + " " + choice + " " + target);
					}

					// add action
					if (transition.length > 4) {
						String action = transition[4];
						Object oldAction = getAction(source, choice);
						if (oldAction != null && !action.equals(oldAction)) {
							throw new PrismException("Problem in .tra file (line " + lineNum + "):"
									+ "inconsistent action label for " + source + ", " + choice + ": "
									+ oldAction + " and " + action);
						}
						setAction(source, choice, action);
					}
				}
				s = in.readLine();
				lineNum++;
			}
			// check integrity
			if (getNumChoices() != expectedNumChoices) {
				throw new PrismException("Problem in .tra file: unexpected number of choices: " + getNumChoices());
			}
			if (getNumTransitions() != expectedNumTransitions) {
				throw new PrismException("Problem in .tra file: unexpected number of transitions: " + getNumTransitions());
			}
			assert (emptyDistributions >= 0);
			if (emptyDistributions > 0) {
				throw new PrismException("Problem in .tra file: there are " + emptyDistributions + " empty distribution, are there gaps in the choice indices?");
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + filename + "\": " + e.getMessage());
		} catch (NumberFormatException e) {
			throw new PrismException("Problem in .tra file (line " + lineNum + ") for " + getModelType());
		}
	}

	// Mutators (other)

	/**
	 * Set an Evaluator for intervals of Value.
	 * The default is for the (usual) case when Value is Double.
	 */
	public void setIntervalEvaluator(Evaluator<Interval<Value>> eval) {
		mdp.setEvaluator(eval);
	}

	/**
	 * Add a choice (uncertain distribution {@code udistr}) to state {@code s} (which must exist).
	 * Returns the index of the (newly added) distribution.
	 * Returns -1 in case of error.
	 */
	public int addChoice(int s, Distribution<Interval<Value>> udistr) {
		return mdp.addChoice(s, udistr);
	}

	/**
	 * Add a choice (uncertain distribution {@code udistr}) labelled with {@code action} to state {@code s} (which must exist).
	 * Returns the index of the (newly added) distribution.
	 * Returns -1 in case of error.
	 */
	public int addActionLabelledChoice(int s, Distribution<Interval<Value>> udistr, Object action) {
		return mdp.addActionLabelledChoice(s, udistr, action);
	}

	/**
	 * Set the action label for choice i in some state s.
	 */
	public void setAction(int s, int i, Object action) {
		mdp.setAction(s, i, action);
	}

	/**
	 * Delimit the intervals for probabilities for the ith choice (distribution) for state s.
	 * i.e., trim the bounds of the intervals such that at least one
	 * possible distribution takes each of the extremal values.
	 *
	 * @param s The index of the state to delimit
	 * @param i The index of the choice to delimit
	 */
	public void delimit(int s, int i) {
		IntervalUtils.delimit(mdp.trans.get(s).get(i), getEvaluator());
	}

	// Accessors (for Model)

	@Override
	public void findDeadlocks(boolean fix) throws PrismException {
		mdp.findDeadlocks(fix);
	}

	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException {
		mdp.checkForDeadlocks(except);
	}

	@Override
	public void exportToPrismExplicitTra(PrismLog out, int precision) {
		mdp.exportToPrismExplicitTra(out, precision);
	}

	@Override
	public void exportToPrismLanguage(final String filename, int precision) throws PrismException {
		mdp.exportToPrismLanguage(filename, precision);
	}

	// Accessors (for NondetModel)

	@Override
	public int getNumChoices(int s) {
		return mdp.getNumChoices(s);
	}

	@Override
	public Object getAction(int s, int i) {
		return mdp.getAction(s, i);
	}

	@Override
	public boolean allSuccessorsInSet(int s, int i, BitSet set) {
		return mdp.allSuccessorsInSet(s, i, set);
	}

	@Override
	public boolean someSuccessorsInSet(int s, int i, BitSet set) {
		return mdp.someSuccessorsInSet(s, i, set);
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int s, final int i) {
		return mdp.getSuccessorsIterator(s, i);
	}

	@Override
	public SuccessorsIterator getSuccessors(final int s, final int i) {
		return mdp.getSuccessors(s, i);
	}

	@Override
	public int getNumTransitions(int s, int i) {
		return mdp.getNumTransitions(s, i);
	}

	@Override
	public Model<Value> constructInducedModel(MDStrategy<Value> strat) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	public IDTMC<Value> constructInducedIDTMC(MDStrategy<Value> strat) {
		return new IDTMCFromIMDPAndMDStrategy<>(this, strat);
	}

	// Accessors (for UMDP)

	@Override
	public void checkLowerBoundsArePositive() throws PrismException {
		Evaluator<Interval<Value>> eval = mdp.getEvaluator();
		int numStates = getNumStates();
		for (int s = 0; s < numStates; s++) {
			int numChoices = getNumChoices(s);
			for (int j = 0; j < numChoices; j++) {
				Iterator<Map.Entry<Integer, Interval<Value>>> iter = getTransitionsIterator(s, j);
				while (iter.hasNext()) {
					Map.Entry<Integer, Interval<Value>> e = iter.next();
					// NB: we phrase the check as an operation on intervals, rather than
					// accessing the lower bound directly, to make use of the evaluator
					if (!eval.gt(e.getValue(), eval.zero())) {
						List<State> sl = getStatesList();
						String state = sl == null ? "" + s : sl.get(s).toString();
						throw new PrismException("Transition probability has lower bound of 0 in state " + state);
					}
				}
			}
		}
	}

	@Override
	public double mvMultUncSingle(int s, int k, double vect[], MinMax minMax) {
		@SuppressWarnings("unchecked")
		DoubleIntervalDistribution did = IntervalUtils.extractDoubleIntervalDistribution(((IMDP<Double>) this).getTransitionsIterator(s, k), getNumTransitions(s, k));
		return IDTMC.mvMultUncSingle(did, vect, minMax);
	}

	// Accessors (for IMDP)

	@Override
	public Evaluator<Interval<Value>> getIntervalEvaluator() {
		return mdp.getEvaluator();
	}

	@Override
	public Iterator<Map.Entry<Integer, Interval<Value>>> getTransitionsIterator(int s, int i) {
		return mdp.getTransitionsIterator(s, i);
	}

	@Override
	public String toString()
	{
		return mdp.toString();
	}
}
