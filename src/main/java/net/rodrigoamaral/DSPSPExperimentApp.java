package net.rodrigoamaral;

import net.rodrigoamaral.dspsp.experiment.ExperimentCLI;
import net.rodrigoamaral.dspsp.experiment.ExperimentRunner;
import net.rodrigoamaral.logging.SPSPLogger;
import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * DSPSP experiment entry point.
 *
 * @author Rodrigo Amaral
 */
public class DSPSPExperimentApp {
    public static void main(String[] args) {
        long stTime, allRunsTime;

        stTime = System.currentTimeMillis();

        ExperimentCLI cli = new ExperimentCLI(args);
        ExperimentRunner runner = new ExperimentRunner(cli.getExperimentSettings());
        runner.run();

        allRunsTime = System.currentTimeMillis() - stTime;
        SPSPLogger.info("All runs duration: " + DurationFormatUtils.formatDuration(allRunsTime, "HH:mm:ss,SSS"));
    }
}