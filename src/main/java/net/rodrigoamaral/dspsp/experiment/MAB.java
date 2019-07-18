package net.rodrigoamaral.dspsp.experiment;

import net.rodrigoamaral.logging.SPSPLogger;

import java.util.Random;

public class MAB {

    private double hist, repaired;
    private double delta;
    private Random random;
    private int[] pullMachine = new int[6];
    private double[] rewards = new double[6];
    private int time;
    private int numArms;
    private int lastArm;

    public MAB (double hist, double repaired) {
        this.hist = hist;
        this.repaired = repaired;
        random = new Random();
        time = 0;
        delta = 0.05;
        numArms = 6;
        for (int i = 0; i < 6; i++) {
            pullMachine[i] = 0;
            rewards[i] = 0;
        }
    }

    public double getRepaired() {
        return repaired;
    }

    public double getHist() {
        return hist;
    }

    public void pullArm (int arm) {
        if ((hist) <= 2*delta && (arm == 0 || arm == 4))
            return;
        if ((repaired) <= 2*delta && (arm == 2 || arm == 5))
            return;
        if (arm == 0) // hist -> random
            hist -= delta;
        else if (arm == 1) // hist <- random
            hist += delta;
        else if (arm == 2) // repaired -> random
            repaired -= delta;
        else if (arm == 3) // repaired <- random
            repaired += delta;
        else if (arm == 4) { // hist -> repaired
            hist -= delta;
            repaired += delta;
        }
        else if (arm == 5) { // hist <- repaired
            hist += delta;
            repaired -= delta;
        }
    }

    /**
     * Update weights of each type of population for dynamic rescheduling.
     * The new values are in 0.05 intervals. It's guaranteed that each value
     * will be at least 0.05 and at most 0.90, that way each population will
     * have at least 0.05 representation. Randomly.
     */
    public void updWeightsRandom () {
        int hist = 1 + random.nextInt(18);
        int repaired = (1 + random.nextInt(19 - hist));
        this.hist = hist/20.0;
        this.repaired = repaired/20.0;
    }

    /**
     * Update weights of each type of population for dynamic rescheduling.
     * The new values are in delta intervals. It's guaranteed that each value
     * will be at least 0.05 and at most 0.90, that way each population will
     * have at least 0.05 representation. Using MAB.
     */
    public void updWeights () {
        int arm = 0;

        if (time < numArms)
            arm = time;
        else {
            double bst = -1;
            for (int i = 0; i < 6; i++) {
                double ucb = Math.sqrt((2 * Math.log10(time)) / pullMachine[i]);
                if (ucb + rewards[i]/pullMachine[i] > bst) {
                    bst = ucb + rewards[i];
                    arm = i;
                }
                SPSPLogger.info (String.valueOf(pullMachine[i]));
                SPSPLogger.info (String.valueOf(rewards[i]/pullMachine[i]));
            }
        }

        pullMachine[arm]++;
        pullArm (arm);
        lastArm = arm;
        time++;
    }

    public void insertReward (double reward) {
        rewards[lastArm] += reward;
    }
}
