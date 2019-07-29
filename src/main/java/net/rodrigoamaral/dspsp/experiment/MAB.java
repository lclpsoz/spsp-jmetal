package net.rodrigoamaral.dspsp.experiment;

import net.rodrigoamaral.logging.SPSPLogger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class MAB {

    private double hist, repaired;
    private double delta;
    private double epsilon;
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
        for (int i = 0; i < 6; i++) {
            pullMachine[i] = 0;
            rewards[i] = 0;
        }

        epsilon = 0.5;
        delta = 0.05;
        numArms = 6;
    }

    public double getRepaired() {
        return repaired;
    }

    public double getHist() {
        return hist;
    }

    /**
     * Round with good enough precision.
     * @param value
     * @param places
     * @return
     */
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public void pullArm (int arm) {
        pullMachine[arm]++;
        if ((hist) <= 2*delta && (arm == 0 || arm == 4))
            return;
        if ((repaired) <= 2*delta && (arm == 2 || arm == 5))
            return;
        if ((1.0-hist-repaired) <= 2*delta && (arm == 1 || arm == 3))
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
        hist = round (hist, 4);
        repaired = round (repaired, 4);
        lastArm = arm;
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
    public void updWeightsUCB1() {
        int arm = 0;

        if (time < numArms)
            arm = time;
        else {
            double bst = -1;
            for (int i = 0; i < 6; i++) {
                double a = Math.sqrt((2 * Math.log10(time)) / pullMachine[i]);
                double eval = rewards[i]/pullMachine[i] + a;
                if (eval > bst) {
                    bst = eval;
                    arm = i;
                }
                SPSPLogger.info ("Arm " + (i+1) + ": " + pullMachine[i] + "\t" + eval);
            }
        }

        pullArm (arm);
        time++;
    }

    /**
     * Update weights of each type of population for dynamic rescheduling.
     * The new values are in delta intervals. It's guaranteed that each value
     * will be at least 0.05 and at most 0.90, that way each population will
     * have at least 0.05 representation. Using Epsilon Greedy.
     */
    public void updWeightsEpsilonGreedy () {
        int arm = 0;

        if (time < numArms)
            arm = time;
        else if (random.nextDouble() < epsilon)
            arm = random.nextInt(6);
        else {
            double bst = -1;
            for (int i = 0; i < 6; i++) {
                double eval = rewards[i] / pullMachine[i];
                if (eval > bst) {
                    bst = eval;
                    arm = i;
                }
                SPSPLogger.info("Arm " + (i+1) + ": " + pullMachine[i] + "\t" + eval);
            }
        }

        pullArm (arm);
        time++;
    }

    public void insertReward (double reward) {
        rewards[lastArm] += reward;
    }
}
