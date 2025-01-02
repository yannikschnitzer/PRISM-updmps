package lava;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParallelLearner {
    private static final int seed = 1650280571;

    public static List<Integer> get_seeds(int seed, int num) {
        ArrayList<Integer> seeds = new ArrayList<>();
        seeds.add(seed);
        Random r = new Random(seed);

        for (int i = 0; i < num - 1; i++) {
            seeds.add(r.nextInt(seed));
        }

        return seeds;
    }

    public static void main(String[] args) {
        List<Integer> seeds = get_seeds(seed, 10);
        System.out.println(seeds);
        List<Thread> threads = new ArrayList<>();
        for (int seed : seeds.subList(0, 3)) {
            Thread thread = new Thread(new LearningThread(seed));
            thread.start();
            threads.add(thread);
            System.out.println("here");
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class LearningThread implements Runnable {
        int seed;

        public LearningThread(int seed) {
            this.seed = seed;
        }

        @Override
        public void run() {
            LearnVerifyParallel learner = new LearnVerifyParallel(seed);
            learner.basic();
        }
    }
}
