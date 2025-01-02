package lava;

public class DataPointRobust {

    private int episode;

    private double imdpGuarantee;

    private double trueMDPGuarantee;

    private double robustIMDPPerformanceOnTrue;

    private double existentialGuarantee;

    private double imdpGuaranteeRL;

    private double RLPerformanceOnTrue;

    public DataPointRobust(int episode, double[] results) {
        this.episode = episode;
        this.imdpGuarantee = results[0];
        this.trueMDPGuarantee = results[1];
        this.robustIMDPPerformanceOnTrue = results[2];
        this.existentialGuarantee = results[3];
        this.imdpGuaranteeRL = results[4];
        this.RLPerformanceOnTrue = results[5];
    }

    @Override
    public boolean equals(Object o) {

        if (o == this) {
            return true;
        }


        if (!(o instanceof DataPointRobust d)) {
            return false;
        }


        boolean eq = false;
        // todo implement equality on d and this

        return eq;
    }

    public int getEpisode() {
        return episode;
    }

    public void setEpisode(int episode) {
        this.episode = episode;
    }

    public double getImdpGuarantee() {
        return imdpGuarantee;
    }

    public void setImdpGuarantee(double imdpGuarantee) {
        this.imdpGuarantee = imdpGuarantee;
    }

    public double getTrueMDPGuarantee() {
        return trueMDPGuarantee;
    }

    public void setTrueMDPGuarantee(double trueMDPGuarantee) {
        this.trueMDPGuarantee = trueMDPGuarantee;
    }

    public double getRobustIMDPPerformanceOnTrue() {
        return robustIMDPPerformanceOnTrue;
    }

    public void setRobustIMDPPerformanceOnTrue(double robustIMDPPerformanceOnTrue) {
        this.robustIMDPPerformanceOnTrue = robustIMDPPerformanceOnTrue;
    }

    public double getExistentialGuarantee() {
        return existentialGuarantee;
    }

    public void setExistentialGuarantee(double existentialGuarantee) {
        this.existentialGuarantee = existentialGuarantee;
    }

    public double getImdpGuaranteeRL() {
        return imdpGuaranteeRL;
    }

    public void setImdpGuaranteeRL(double imdpGuaranteeRL) {
        this.imdpGuaranteeRL = imdpGuaranteeRL;
    }

    public double getRLPerformanceOnTrue() {
        return RLPerformanceOnTrue;
    }

    public void setRLPerformanceOnTrue(double RLPerformanceOnTrue) {
        this.RLPerformanceOnTrue = RLPerformanceOnTrue;
    }
}
