//==============================================================================
//
//	Copyright (c) 2024-
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

package prism;

import explicit.NondetModel;
import parser.State;
import strat.Strategy;

/**
 * Class to convert a reward generator for a nondeterministic model (e.g. an MDP)
 * to a corresponding model induced by a memoryless/randomised strategy (e.g. DTMC).
 */
public class RewardGeneratorMRStrat<Value> extends RewardGeneratorMDStrat<Value>
{
	private Strategy<Value> stratR;

	public RewardGeneratorMRStrat(RewardGenerator<Value> rewGen, NondetModel<?> model, Strategy<Value> stratR)
	{
		super(rewGen, model, null);
		this.rewGen = rewGen;
		this.model = model;
		this.stratR = stratR;
	}

	@Override
	public Value getStateReward(int r, State state) throws PrismException
	{
		int s = model.getStatesList().indexOf(state);
		Evaluator<Value> eval = getRewardEvaluator();
		Value rew = rewGen.getStateReward(r, state);
		int numChoices = model.getNumChoices(s);
		for (int i = 0; i < numChoices; i++) {
			Object action = model.getAction(s, i);
			Value pAction = stratR.getChoiceActionProbability(s, -1, action);
			Value tr = rewGen.getStateActionReward(r, state, action);
			rew = eval.add(rew, eval.multiply(pAction, tr));
		}
		return rew;
	}

	@Override
	public Value getStateReward(int r, int s) throws PrismException
	{
		Evaluator<Value> eval = getRewardEvaluator();
		Value rew = rewGen.getStateReward(r, s);
		int numChoices = model.getNumChoices(s);
		for (int i = 0; i < numChoices; i++) {
			Object action = model.getAction(s, i);
			Value pAction = stratR.getChoiceActionProbability(s, -1, action);
			Value tr = rewGen.getStateActionReward(r, s, action);
			rew = eval.add(rew, eval.multiply(pAction, tr));
		}
		return rew;
	}
}
