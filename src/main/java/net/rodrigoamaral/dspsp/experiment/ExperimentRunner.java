package net.rodrigoamaral.dspsp.experiment;

import net.rodrigoamaral.dspsp.DSPSProblem;
import net.rodrigoamaral.dspsp.decision.ComparisonMatrix;
import net.rodrigoamaral.dspsp.decision.DecisionMaker;
import net.rodrigoamaral.dspsp.project.DynamicEmployee;
import net.rodrigoamaral.dspsp.project.DynamicProject;
import net.rodrigoamaral.dspsp.project.events.DynamicEvent;
import net.rodrigoamaral.dspsp.results.SolutionFileWriter;
import net.rodrigoamaral.dspsp.solution.DynamicPopulationCreator;
import net.rodrigoamaral.dspsp.solution.SchedulingHistory;
import net.rodrigoamaral.dspsp.solution.SchedulingResult;
import net.rodrigoamaral.dspsp.solution.repair.EmployeeLeaveStrategy;
import net.rodrigoamaral.dspsp.solution.repair.EmployeeReturnStrategy;
import net.rodrigoamaral.dspsp.solution.repair.IScheduleRepairStrategy;
import net.rodrigoamaral.logging.SPSPLogger;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.AlgorithmRunner;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * Runs experiments on DSPSP problem.
 *
 * ExperimentRunner can read problem instances from JSON files in a specific
 * directory.
 *
 * @author Rodrigo Amaral
 *
 */
public class ExperimentRunner {

    private CalcHypervolume calcHypervolume;
    private final ExperimentSettings experimentSettings;
    private SchedulingHistory history;
    private int reschedulings;
    private Integer reschedulingsLimit;
    private MAB mab;
    private double originalHist, originalRepaired;

    public ExperimentRunner(final ExperimentSettings experimentSettings, Integer reschedulingsLimit) {
        this (experimentSettings);
        this.reschedulingsLimit = reschedulingsLimit;
    }

    public ExperimentRunner(final ExperimentSettings experimentSettings) {
        originalHist = experimentSettings.getHistPropPreviousEventSolutions();
        originalRepaired = experimentSettings.getRepairedSolutions();
        mab = new MAB (originalHist, originalRepaired);

        this.experimentSettings = experimentSettings;
        this.history = new SchedulingHistory();
        calcHypervolume = new CalcHypervolume();
        reschedulingsLimit = null;
    }

    private DSPSProblem loadProblemInstance(final String instanceFile) {
        try {
            return new DSPSProblem(instanceFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    private DSPSProblem loadProblemInstance(final DynamicProject project) {
        return new DSPSProblem(project);
    }

    public ExperimentSettings getExperimentSettings() {
        return experimentSettings;
    }

    private void runInstance(DSPSProblem problem, AlgorithmAssembler assembler, int run) {

        final String algorithmID = assembler.getAlgorithmID();

        reschedulings = 0;

        SPSPLogger.info("Starting simulation -> algorithm: " + algorithmID + "; " +
                "instance: " + problem.getInstanceDescription());

        SPSPLogger.info("Performing initial scheduling...");

        Algorithm<List<DoubleSolution>> algorithm = assembler.assemble(problem);

        AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute() ;

        List<DoubleSolution> population = algorithm.getResult() ;

        history.put(reschedulings, population);

        long totalComputingTime = algorithmRunner.getComputingTime();

        SPSPLogger.info("Initial scheduling complete.");
        SPSPLogger.info("Elapsed time: " + DurationFormatUtils.formatDuration(totalComputingTime, "HH:mm:ss,SSS"));

        new SolutionFileWriter(population)
                .setAlgorithmID(algorithmID)
                .setInstanceID(problem.getInstanceDescription())
                .setRunNumber(run)
                .setSeparator(" ")
                .write();

        // Decides on the best initial schedule
        ComparisonMatrix comparisonMatrix = new ComparisonMatrix();
        DoubleSolution initialSchedule = new DecisionMaker(population, comparisonMatrix)
                .chooseInitialSchedule();

        // Loops through rescheduling points
        DynamicProject project = problem.getProject();
        List<DynamicEvent> reschedulingPoints = project.getEvents();

        DoubleSolution currentSchedule = initialSchedule;

        for (DynamicEvent event: reschedulingPoints) {

            reschedulings++;

            if (project.isFinished() || (reschedulingsLimit != null && reschedulings > reschedulingsLimit)) {
                break;
            }

            SPSPLogger.rescheduling(reschedulings, event, run, experimentSettings.getNumberOfRuns());

            SchedulingResult result = reschedule(project, event, currentSchedule, assembler);


            if (!problem.getProject().isFinished())
                mab.insertReward (calcHypervolume.getHypervolume (result.getSchedules()));

            history.put(reschedulings, result.getSchedules());


            totalComputingTime += result.getComputingTime();

            SPSPLogger.info("Rescheduling "+ reschedulings +" complete in " + DurationFormatUtils.formatDuration(result.getComputingTime(), "HH:mm:ss,SSS") + ". ");
            SPSPLogger.info("Elapsed time: " + DurationFormatUtils.formatDuration(totalComputingTime, "HH:mm:ss,SSS"));
            SPSPLogger.info("Project current duration: " + project.getTotalDuration());
            SPSPLogger.info("Project current cost    : " + project.getTotalCost());


            new SolutionFileWriter(result.getSchedules())
                    .setAlgorithmID(algorithmID)
                    .setInstanceID(problem.getInstanceDescription())
                    .setRunNumber(run)
                    .setReschedulingPoint(reschedulings)
                    .setSeparator(" ")
                    .write();

            currentSchedule = new DecisionMaker(result.getSchedules(), comparisonMatrix).chooseNewSchedule();

        }


        SPSPLogger.info("Total execution time: " + DurationFormatUtils.formatDuration(totalComputingTime, "HH:mm:ss,SSS"));

        // TODO: Write final repairedSolution files
    }

    private SchedulingResult reschedule(DynamicProject project, DynamicEvent event, DoubleSolution lastSchedule, AlgorithmAssembler assembler) {

        IScheduleRepairStrategy repairStrategy = null;
        switch (event.getType()) {
            case EMPLOYEE_LEAVE:
                repairStrategy = new EmployeeLeaveStrategy(lastSchedule, project, (DynamicEmployee) event.getSubject());
                break;
            case EMPLOYEE_RETURN:
                repairStrategy = new EmployeeReturnStrategy(lastSchedule, project, (DynamicEmployee) event.getSubject());
                break;
            default:
                repairStrategy = null;
        }


        project.update(event, lastSchedule);

        DSPSProblem problem = loadProblemInstance(project);

        Algorithm<List<DoubleSolution>> algorithm;

        // First rescheduling doesn't take initial population
        if ((reschedulings > 1) && (assembler.getAlgorithmID().toUpperCase().contains("DYNAMIC"))) {

            if (assembler.getAlgorithmID().startsWith("NSGAIIDynamic_RANDOM"))
                mab.updWeightsRandom();
            else if (assembler.getAlgorithmID().startsWith("NSGAIIDynamic_UCB1"))
                mab.updWeightsUCB1();
            else if (assembler.getAlgorithmID().startsWith("NSGAIIDynamic_EPSILON_GREEDY"))
                mab.updWeightsEpsilonGreedy();

            experimentSettings.setRepairedSolutions(mab.getRepaired());
            experimentSettings.setHistPropPreviousEventSolutions(mab.getHist());
            SPSPLogger.info ("Repaired Solutions: " + experimentSettings.getRepairedSolutions()*100 + " %");
            SPSPLogger.info ("History Solutions: " + experimentSettings.getHistPropPreviousEventSolutions()*100 + " %");

            List<DoubleSolution> initialPopulation = new DynamicPopulationCreator(
                    problem,
                    history,
                    experimentSettings,
                    assembler.getAlgorithmID(),
                    repairStrategy
            ).create(reschedulings);

            algorithm = assembler.assemble(problem, initialPopulation);
        } else {
            algorithm = assembler.assemble(problem);
        }

        AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute();

        return new SchedulingResult(algorithm.getResult(),
                algorithmRunner.getComputingTime(),
                problem.getProject().isFinished());
    }

    /**
     * Executes algorithms for each problem instance.
     *
     * Both instances and algorithms are passed in settings file loaded in
     * experimentSettings.
     *
     */
    public void run() {
        System.out.println(experimentSettings);
        for (String instanceFile : experimentSettings.getInstanceFiles()) {
            for (String algorithmID : experimentSettings.getAlgorithms()) {
                final Integer numberOfRuns = experimentSettings.getNumberOfRuns();
                for (int run = 1; run <= numberOfRuns; run++) {
                    mab = new MAB (originalHist, originalRepaired);
                    calcHypervolume = new CalcHypervolume();
                    SPSPLogger.printRun(run, numberOfRuns);
                    final DSPSProblem problem = loadProblemInstance(instanceFile);
                    AlgorithmAssembler assembler = new AlgorithmAssembler(algorithmID, experimentSettings);
                    runInstance(problem, assembler, run);
                }
            }
        }
    }
}