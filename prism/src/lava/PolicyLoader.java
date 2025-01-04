package lava;

import com.fasterxml.jackson.databind.ObjectMapper;
import explicit.NondetModel;
import parser.State;
import strat.MRStrategy;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PolicyLoader {

    ObjectMapper mapper = new ObjectMapper();

    public MRStrategy loadDronePolicy(String policyFile, NondetModel<Double> model) {
        File policyJson = new File(policyFile);

        try {
            List<List<List<List<Double>>>> policyList = mapper.readValue(policyJson, List.class);

            MRStrategy strat = new MRStrategy(model);

            for (State s : model.getStatesList()) {
                int x = (int) s.varValues[0];
                int y = (int) s.varValues[1];
                int z = (int) s.varValues[2];

                int state_index = model.getStatesList().indexOf(s);

                for (int i = 0; i < model.getNumChoices(state_index); i++) {
                    strat.setChoiceProbability(state_index, i, policyList.get(x - 1).get(y - 1).get(z - 1).get(i));
                }

                //System.out.println("State " + s + " Actions " + policyList.get(x-1).get(y-1).get(z-1));


            }

            return strat;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MRStrategy loadChainPolicy(String policyFile, NondetModel<Double> model) {
        File policyJson = new File(policyFile);

        try {
            List<List<List<Double>>> policyList = mapper.readValue(policyJson, List.class);

            MRStrategy strat = new MRStrategy(model);

            for (State s : model.getStatesList()) {
                int state = (int) s.varValues[0];

                int state_index = model.getStatesList().indexOf(s);

                for (int i = 0; i < model.getNumChoices(state_index); i++) {
                    strat.setChoiceProbability(state_index, i, policyList.get(state).getFirst().get(i));
                }

                //System.out.println("State " + s + " Actions " + policyList.get(x-1).get(y-1).get(z-1));

            }

            return strat;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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

    public MRStrategy loadSavPolicy(String policyFile, NondetModel<Double> model) {
        //File policyJson = new File(policyFile);
        //List<List<List<List<List<List<Double>>>>>> policyList = mapper.readValue(policyJson, List.class);

        MRStrategy strat = new MRStrategy(model);

        for (State s : model.getStatesList()) {
            int xLoc = (int) s.varValues[0];
            int yLoc = (int) s.varValues[1];
            int unreported = (int) s.varValues[2];
            int hasSendNow = ((boolean) s.varValues[3]) ? 1 : 0;
            int tries = (int) s.varValues[4];

            int state_index = model.getStatesList().indexOf(s);
            for (int i = 0; i < model.getNumChoices(state_index); i++) {
                strat.setChoiceProbability(state_index, i, 0.2);
            }

            //System.out.println("State " + s + " Actions " + policyList.get(x).get(y).get(ax).get(ay));

        }

        //System.out.println("Strategy " + strat);
        //System.out.println("Model " + model);

        return strat;

    }

    public MRStrategy loadBettingPolicy(String policyFile, NondetModel<Double> model) {
        File policyJson = new File(policyFile);
        try {
            List<List<List<Double>>> policyList = mapper.readValue(policyJson, List.class);

            MRStrategy strat = new MRStrategy(model);
            for (State s : model.getStatesList()) {
                int money = (int) s.varValues[0];
                int steps = (int) s.varValues[1];

                int state_index = model.getStatesList().indexOf(s);
                for (int i = 0; i < model.getNumChoices(state_index); i++) {
                    strat.setChoiceProbability(state_index, i, policyList.get(money).get(steps).get(i));
                }

                //System.out.println("State " + s + " Actions " + policyList.get(money).get(steps));

            }

            //System.out.println("Strategy " + strat);
            //System.out.println("Model " + model);

            return strat;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MRStrategy loadFirewirePolicy(String policyFile, NondetModel<Double> model) {
        //File policyJson = new File(policyFile);
        //List<List<List<Double>>> policyList = mapper.readValue(policyJson, List.class);

        MRStrategy strat = new MRStrategy(model);
        for (State s : model.getStatesList()) {

            int state_index = model.getStatesList().indexOf(s);
            int num_trans = model.getNumChoices(state_index);
            for (int i = 0; i < model.getNumChoices(state_index); i++) {
                strat.setChoiceProbability(state_index, i, 1.0 / (double) num_trans);
            }

            //System.out.println("State " + s + " Actions " + policyList.get(money).get(steps));

        }

        //System.out.println("Strategy " + strat);
        //System.out.println("Model " + model);

        return strat;

    }
}
