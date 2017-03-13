package net.rodrigoamaral.spsp.project;


import java.util.ArrayList;
import java.util.List;

/**
 * Representa uma tarefa e seus atributos, conforme definição do SPSP
 */
public class Task {

    private int id;
    private double effort;
    private double duration;
    private double start;
    private double finish;
    private List<Integer> skills;

    public Task(int id, double effort) {
        this.id = id;
        this.effort = effort;
        this.skills =  new ArrayList<>();
    }

    public Task(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getEffort() {
        return effort;
    }

    public void setEffort(double effort) {
        this.effort = effort;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public double getStart() {
        return start;
    }

    public void setStart(double start) {
        this.start = start;
    }

    public double getFinish() {
        return finish;
    }

    public void setFinish(double finish) {
        this.finish = finish;
    }

    public List<Integer> getSkills() {
        return skills;
    }

    public void setSkills(List<Integer> skills) {
        this.skills = skills;
    }

    /**
     * Checks if task is being executed in a given moment.
     * Used for overwork calculation.
     *
     * @param instant
     * @return true if task is being executed in instant, false otherwise.
     */
    public boolean isTaskRunning(int instant) {
        return this.getStart() <= instant && instant <=this.getFinish();
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", effort=" + effort +
                ", duration=" + duration +
                ", start=" + start +
                ", finish=" + finish +
                ", skills=" + skills +
                '}';
    }
}