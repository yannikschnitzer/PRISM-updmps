package lava;

public class DataPointRobust {

    private int episode;

    private double imdpGuarantee;

    private double trueMDPGuarantee;

    private double robustIMDPPerformanceOnTrue;

    private double existentialGuarantee;

    public DataPointRobust(int episode, double[] results) {
        this.episode = episode;
        this.imdpGuarantee = results[0];
        this.trueMDPGuarantee = results[1];
        this.robustIMDPPerformanceOnTrue = results[2];
        this.existentialGuarantee = results[3];
    }

    @Override
    public boolean equals(Object o) {
 
        if (o == this) {
            return true;
        }
 

        if (!(o instanceof DataPointRobust)) {
            return false;
        }
         

        DataPointRobust d = (DataPointRobust) o;
         
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
}
