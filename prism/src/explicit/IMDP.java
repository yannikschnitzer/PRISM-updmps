//==============================================================================
//	
//	Copyright (c) 2020-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;

import common.Interval;
import common.IterableStateSet;
import explicit.rewards.MDPRewards;
import parser.State;
import prism.Evaluator;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;
import strat.MDStrategy;

/**
 * Interface for classes that provide (read) access to an explicit-state interval MDP.
 */
public interface IMDP<Value> extends UMDP<Value>
{
	// Accessors (for Model) - default implementations
	
	@Override
	public default ModelType getModelType()
	{
		return ModelType.IMDP;
	}

	// Accessors

	/**
	 * Get an Evaluator for intervals of Value.
	 * A default implementation tries to create one from the main iterator
	 * (which itself by default exists for the (usual) case when Value is Double).
	 */
	@SuppressWarnings("unchecked")
	public default Evaluator<Interval<Value>> getIntervalEvaluator()
	{
		return getEvaluator().createIntervalEvaluator();
	}

	/**
	 * Get an iterator over the transitions from choice {@code i} of state {@code s}.
	 */
	public Iterator<Map.Entry<Integer, Interval<Value>>> getTransitionsIterator(int s, int i);

	/**
	 * Functional interface for an interval consumer,
	 * accepting transitions (s,t,i), i.e.,
	 * from state s to state t with interval d.
	 */
	@FunctionalInterface
	public interface TransitionConsumer<Value> {
		void accept(int s, int t, Interval<Value> i);
	}

	public IDTMC<Value> constructInducedIDTMC(MDStrategy<Value> strat);

	/**
	 * Iterate over the outgoing transitions of state {@code s} and choice {@code i}
	 * and call the accept method of the consumer for each of them:
	 * <br>
	 * Call {@code accept(s,t,d)} where t is the successor state i = P(s,i,t)
	 * is the probability interval from s to t with choice i.
	 * <p>
	 * <i>Default implementation</i>: The default implementation relies on iterating over the
	 * iterator returned by {@code getTransitionsIterator()}.
	 * <p><i>Note</i>: This method is the base for the default implementation of the numerical
	 * computation methods (mvMult, etc). In derived classes, it may thus be worthwhile to
	 * provide a specialised implementation for this method that avoids using the Iterator mechanism.
	 *
	 * @param s the state s
	 * @param i the choice i
	 * @param c the consumer
	 */
	public default void forEachTransition(int s, int i, TransitionConsumer<Value> c)
	{
		for (Iterator<Map.Entry<Integer, Interval<Value>>> it = getTransitionsIterator(s, i); it.hasNext(); ) {
			Map.Entry<Integer, Interval<Value>> e = it.next();
			c.accept(s, e.getKey(), e.getValue());
		}
	}

	// Methods for case where Value is Double

	@FunctionalInterface
	public interface DoubleIntervalTransitionConsumer {
		void accept(int s, int t, Interval<Double> d);
	}

	public default void forEachDoubleIntervalTransition(int s, int i, DoubleIntervalTransitionConsumer c) {
		for (Iterator<Map.Entry<Integer, Interval<Value>>> it = getTransitionsIterator(s, i); it.hasNext(); ) {
			Map.Entry<Integer, Interval<Value>> e = it.next();
			c.accept(s, e.getKey(), new Interval<Double>(getEvaluator().toDouble(e.getValue().getLower()),
					getEvaluator().toDouble(e.getValue().getUpper())));
		}
	}


}
