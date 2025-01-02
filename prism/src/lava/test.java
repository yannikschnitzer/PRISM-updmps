package lava;

import common.Interval;
import org.apache.commons.statistics.distribution.NormalDistribution;

public class test {

    static NormalDistribution distribution = NormalDistribution.of(0, 1);

    public static void main(String[] args) {
        int n = 100000;
        System.out.println(computeWilsonCC(n, 0.9, 0.0001) + " " + computeWilsonCCwrong(n, 0.9, 0.0001));

    }

    private static Interval<Double> computeWilsonCC(double n, double p, double delta) {
        double z = distribution.inverseCumulativeProbability(1 - delta / 2.0);

        double pWCCLower = Math.max(0, (2 * n * p + z * z - z * Math.sqrt(z * z - (1.0 / n) + 4 * n * p * (1 - p) + 4 * p - 2) - 1) / (2 * (n + z * z)));
        double pWCCUpper = Math.min(1, (2 * n * p + z * z + z * Math.sqrt(z * z - (1.0 / n) + 4 * n * p * (1 - p) - 4 * p + 2) + 1) / (2 * (n + z * z)));

        return new Interval<>(pWCCLower, pWCCUpper);
    }

    private static Interval<Double> computeWilsonCCwrong(double n, double p, double delta) {
        double z = distribution.inverseCumulativeProbability(1 - delta / 2.0);

        double pWCCLower = Math.max(0, (2 * n * p + z * z - z * Math.sqrt(z * z - (1.0 / n) + 4 * n * p * (1 - p) + 4 * p - 2 - 1)) / (2 * (n + z * z)));
        double pWCCUpper = Math.min(1, (2 * n * p + z * z + z * Math.sqrt(z * z - (1.0 / n) + 4 * n * p * (1 - p) - 4 * p + 2 + 1)) / (2 * (n + z * z)));

        return new Interval<>(pWCCLower, pWCCUpper);
    }

}
