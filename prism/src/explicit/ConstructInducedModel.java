//==============================================================================
//
//	Copyright (c) 2023-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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
import prism.ModelType;
import prism.PrismException;
import prism.PrismNotSupportedException;
import strat.MDStrategy;
import strat.Strategy;
import strat.StrategyExportOptions.InducedModelMode;
import strat.StrategyInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Construct the model induced by a memoryless deterministic strategy on a nondeterministic model
 */
public class ConstructInducedModel
{
	/**
	 * The "mode" of construction:
	 * "restrict" (same model type but restrict to selected action choices); or
	 * "reduce" (change mode type by removing nondeterminism)
	 */
	private InducedModelMode mode = InducedModelMode.RESTRICT;

	/**
	 * Whether to restrict strategy/model to reachable states
	 */
	private boolean reachOnly = true;

	/**
	 * Set the "mode" of construction:
	 * "restrict" (same model type but restrict to selected action choices); or
	 * "reduce" (change mode type by removing nondeterminism)
	 */
	public ConstructInducedModel setMode(InducedModelMode mode)
	{
		this.mode = mode;
		return this;
	}

	/**
	 * Set whether to restrict strategy/model to reachable states
	 */
	public ConstructInducedModel setReachOnly(boolean reachOnly)
	{
		this.reachOnly = reachOnly;
		return this;
	}

	/**
	 * Construct the model induced by a memoryless deterministic strategy on a nondeterministic model
	 * @param model The model
	 * @param strat The strategy
	 * @return The induced model
	 */
	@SuppressWarnings("unchecked")
	public <Value> Model<Value> constructInducedModel(NondetModel<Value> model, Strategy<Value> strat) throws PrismException
	{
		// This is for memoryless strategies
		if (strat.hasMemory()) {
			throw new PrismException("Induced model construction is for memoryless strategies");
		}

		// Determine type of induced model
		ModelType modelType = model.getModelType();
		ModelType inducedModelType = null;
		if (mode == InducedModelMode.REDUCE) {
			switch (modelType) {
				case MDP:
				case POMDP:
				case STPG:
					inducedModelType = ModelType.DTMC;
					break;
				case IMDP:
					inducedModelType = ModelType.IDTMC;
					break;
				default:
					throw new PrismNotSupportedException("Induced model construction not supported for " + modelType + "s");
			}
		} else {
			inducedModelType = modelType;
		}

		// Create a (simple, mutable) model of the appropriate type
		ModelSimple<Value> inducedModel = (ModelSimple<Value>) ModelSimple.forModelType(inducedModelType);
		
		// Attach evaluator and copy variable info
		if (modelType == ModelType.IMDP) {
		} else {
			((ModelExplicit<Value>) inducedModel).setEvaluator(model.getEvaluator());
		}
		((ModelExplicit<Value>) inducedModel).setVarList(model.getVarList());

		// Now do the actual induced model construction
		// This is a separate method so that we can alter the model type if needed,
		// e.g. construct an IMDP<Value> product as one over an MDP<Interval<Value>>
		switch (modelType) {
//			case IMDP:
//				inducedModelType = (mode == InducedModelMode.REDUCE) ? ModelType.DTMC : ModelType.MDP;
//				return doConstructInducedModel(ModelType.MDP, inducedModelType, inducedModel, model, strat);
			default:
				//System.out.println("Model type:" + modelType  + " induced model type:" + inducedModelType);
				return doConstructInducedModel(modelType, inducedModelType, inducedModel, model, strat);
		}
	}

	/**
	 * Do the main part of the construction of the model induced
	 * by a memoryless deterministic strategy on a nondeterministic model,
	 * inserting states and transitions into the provided ModelSimple object.
	 * @param modelType The type of the original model
	 * @param inducedModelType The type of the induced model
	 * @param inducedModel The (empty) induced model
	 * @param model The model
	 * @param strat The strategy
	 * @return The product model
	 */
	@SuppressWarnings("unchecked")
	public <Value> Model<Value> doConstructInducedModel(ModelType modelType, ModelType inducedModelType, ModelSimple<Value> inducedModel, NondetModel<Value> model, Strategy<Value> strat) throws PrismException
	{
		// Create new states list if needed
		List<State> inducedStatesList = model.getStatesList();
		if (reachOnly && inducedStatesList != null) {
			inducedStatesList = new ArrayList<>();
		}

		// Initially create an array with 0s for reachable state indices and -1s for unreachable ones
		int numStates = model.getNumStates();
		int[] map = new int[numStates];
		if (reachOnly) {
			Arrays.fill(map, -1);
			BitSet explore = new BitSet();
			// Get initial states
			for (int is : model.getInitialStates()) {
				map[is] = 0;
				explore.set(is);
			}

			// Compute reachable states (and store 0s in map)
			while (!explore.isEmpty()) {
				for (int s = explore.nextSetBit(0); s >= 0; s = explore.nextSetBit(s + 1)) {
					explore.set(s, false);
					int numChoices =  model.getNumChoices(s);
					// Extract strategy decision
					Object decision = strat.getChoiceAction(s, -1);
					// If it is undefined, just pick the first one
					if (decision == StrategyInfo.UNDEFINED && numChoices > 0) {
						decision = model.getAction(s, 0);
					}
					// Go through transitions from state s_1 in original model
					for (int j = 0; j < numChoices; j++) {
						Object act = model.getAction(s, j);
						// Skip choices not picked by the strategy
						if (!strat.isActionChosen(decision, act)) {
							continue;
						}
						for (Iterator<Integer> it = model.getSuccessorsIterator(s, j); it.hasNext(); ) {
							int dest = it.next();
							if (map[dest] == -1) {
								map[dest] = 0;
								explore.set(dest);
							}
						}
					}
				}
			}
			// Then populate map with indices
			int count = 0;
			for (int s = 0; s < numStates; s++) {
				if (map[s] != -1) {
					map[s] = count++;
				}
			}
		} else {
			// Skip reachability
			for (int s = 0; s < numStates; s++) {
				map[s] = s;
			}
		}

		// Iterate through reachable states to create new model
		Value stratChoiceProb = model.getEvaluator().one();
		for (int s = 0; s < numStates; s++) {
			if (map[s] == -1) {
				continue;
			}

			// Add state to model
			switch (inducedModelType) {
				case STPG:
					((STPGSimple<Value>) inducedModel).addState(((STPG<Value>) model).getPlayer(s));
					break;
				default:
					inducedModel.addState();
					break;
			}
			if (model.isInitialState(s)) {
				inducedModel.addInitialState(map[s]);
			}
			if (reachOnly && inducedStatesList != null) {
				inducedStatesList.add(model.getStatesList().get(s));
			}

			int numChoices = model.getNumChoices(s);
			// Extract strategy decision
			Object decision = strat.getChoiceAction(s, -1);
			// If it is undefined, just pick the first one
			if (decision == StrategyInfo.UNDEFINED && numChoices > 0) {
				decision = model.getAction(s, 0);
			}
			// Go through transitions from state s_1 in original model
			for (int j = 0; j < numChoices; j++) {
				Object act = model.getAction(s, j);
				// Skip choices not picked by the strategy
				if (!strat.isActionChosen(decision, act)) {
					continue;
				}
				if (strat.isRandomised()) {
					stratChoiceProb = strat.getChoiceActionProbability(decision, act);
				}

				if (modelType == ModelType.IMDP && mode == InducedModelMode.REDUCE) {
					Iterator<Map.Entry<Integer, Interval<Value>>> iter = ((IMDP<Value>) model).getTransitionsIterator(s, j);

					while (iter.hasNext()) {
						Map.Entry<Integer, Interval<Value>> entry = iter.next();
						//System.out.println("Entry: " + entry.getKey() + " -> " + entry.getValue());
						int s_2 = entry.getKey();
						Interval<Value> probInt = entry.getValue();
						//System.out.println("Prob Int:" + probInt);
						//System.out.println("Strat Prob: " + stratChoiceProb);
						if (strat.isRandomised()) {
							probInt = ((IMDP<Value>) model).getIntervalEvaluator().multiply(new Interval<>(stratChoiceProb, stratChoiceProb), probInt);
						}
						//System.out.println("Prob Int after:" + probInt);

						if (inducedModelType == ModelType.IDTMC) {
							((IDTMCSimple<Value>)inducedModel).addToProbability(map[s], map[s_2], probInt);
						} else {
							throw new PrismException("Induced model type does not match.");
						}
					}
				} else {
					// Go through transitions of original model
					Iterator<Map.Entry<Integer, Value>> iter = switch (modelType) {
                        case MDP -> ((MDP<Value>) model).getTransitionsIterator(s, j);
                        case POMDP -> ((POMDP<Value>) model).getTransitionsIterator(s, j);
                        case STPG -> ((STPG<Value>) model).getTransitionsIterator(s, j);
                        default ->
                                throw new PrismNotSupportedException("Induced model construction not implemented for " + modelType + "s");
                    };
                    Distribution<Value> prodDistr = null;
					if (inducedModelType.nondeterministic()) {
						prodDistr = new Distribution<>(model.getEvaluator());
					}
					while (iter.hasNext()) {
						Map.Entry<Integer, Value> e = iter.next();
						int s_2 = e.getKey();
						Value prob = e.getValue();
						if (strat.isRandomised()) {
							prob = model.getEvaluator().multiply(prob, stratChoiceProb);
						}
						// Add transition to model
						switch (inducedModelType) {
							case DTMC:
								((DTMCSimple<Value>) inducedModel).addToProbability(map[s], map[s_2], prob);
								break;
							case MDP:
							case POMDP:
							case STPG:
								prodDistr.set(map[s_2], prob);
								break;
							default:
								throw new PrismNotSupportedException("Induced model construction not implemented for " + modelType + "s");
						}
					}
					switch (inducedModelType) {
						case MDP:
							((MDPSimple<Value>) inducedModel).addActionLabelledChoice(map[s], prodDistr, ((MDP<Value>) model).getAction(s, j));
							break;
						case POMDP:
							((POMDPSimple<Value>) inducedModel).addActionLabelledChoice(map[s], prodDistr, ((POMDP<Value>) model).getAction(s, j));
							break;
						case STPG:
							((STPGSimple<Value>) inducedModel).addActionLabelledChoice(map[s], prodDistr, ((STPG<Value>) model).getAction(s, j));
							break;
						default:
							break;
					}
				}
			}
		}

		inducedModel.findDeadlocks(false);

		if (inducedStatesList != null) {
			inducedModel.setStatesList(inducedStatesList);
		}

		return inducedModel;
	}
}
