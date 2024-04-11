package lava;

import explicit.IMDP;
import explicit.MDP;
import param.Function;

import java.util.List;

public class RobustPolicySynthesizer {

    // Parametric MDP defining the structure of the underlying problem
    private MDP<Function> mdpParam;

    // List of IMDPs to extract the robust policy for
    private List<IMDP<Double>> imdps;

    public RobustPolicySynthesizer(MDP<Function> mdpParam) {
           this.mdpParam = mdpParam;
    }

    public void addIMDP(IMDP<Double> imdp) {
        this.imdps.add(imdp);
    }

    public void addIMDPs(List<IMDP<Double>> imdps) {
        this.imdps.addAll(imdps);
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
}
