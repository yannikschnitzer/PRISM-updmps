package lava;

import com.fasterxml.jackson.databind.ObjectMapper;
import explicit.MDP;
import explicit.Model;
import explicit.NondetModel;
import parser.State;
import strat.MRStrategy;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PolicyLoader {

    ObjectMapper mapper = new ObjectMapper();

    public MRStrategy loadAircraftPolicy(String policyFile, NondetModel<Double> model) {
        File policyJson = new File(policyFile);
        try {
            List<List<List<List<List<Double>>>>> policyList = mapper.readValue(policyJson, List.class);

            MRStrategy strat = new MRStrategy(model);

            for (State s : model.getStatesList()) {
                int x = (int) s.varValues[0];
                int y = (int) s.varValues[1];
                int ax = (int) s.varValues[2];
                int ay = (int) s.varValues[3];

                int state_index = model.getStatesList().indexOf(s);
                for (int i = 0; i < model.getNumChoices(state_index); i++) {
                    strat.setChoiceProbability(state_index, i, policyList.get(x).get(y).get(ax).get(ay).get(i));
                }

                //System.out.println("State " + s + " Actions " + policyList.get(x).get(y).get(ax).get(ay));

            }

            //System.out.println("Strategy " + strat);
            //System.out.println("Model " + model);

            return strat;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
