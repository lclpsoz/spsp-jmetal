package net.rodrigoamaral.dspsp.experiment;

import org.uma.jmetal.qualityindicator.impl.hypervolume.PISAHypervolume;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.front.imp.ArrayFront;
import org.uma.jmetal.util.point.impl.ArrayPoint;

import java.util.List;

public class CalcHypervolume {

    private double[] minRes, maxRes;
    private double[][] front;

    public CalcHypervolume() {
        minRes = new double[4];
        maxRes = new double[4];
    }

    public double getHypervolume (List<DoubleSolution> results) {
        front = new double[results.size()][4];

        for (DoubleSolution r : results)
            for (int i = 0; i < 4; i++) {
                minRes[i] = Math.min (minRes[i], r.getObjective(i));
                maxRes[i] = Math.max (maxRes[i], r.getObjective(i));
            }

        int pos = 0;
        for (DoubleSolution r : results)
            for (int i = 0; i < 4; i++)
                front[pos][i] = (r.getObjective(i) - minRes[i]) / (maxRes[i] - minRes[i]);

        ArrayFront referencePoints = new ArrayFront (results.size(), 4);
        double[] refPoint = {1.1, 1.1, 1.1, 1.1};
        for (int i = 0; i < results.size(); i++)
            referencePoints.setPoint(i, new ArrayPoint(refPoint));

        PISAHypervolume<DoubleSolution> pisa = new PISAHypervolume<DoubleSolution>(referencePoints);
        return pisa.calculateHypervolume(front, results.size(), 4);
    }
}
