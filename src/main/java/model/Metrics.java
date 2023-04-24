package model;

import java.util.ArrayList;
import java.util.List;

public class Metrics {

    private boolean buggyness = false;
    private int size;
    private final List<Integer> addedLinesOfCodeList = new ArrayList<>();
    private final List<Integer> deletedLinesOfCodeList = new ArrayList<>();
    private int maxLocAdded;
    private int locAdded;
    private double avgLocAdded;
    private int churn;
    private int maxChurn;
    private double avgChurn;

    public Metrics() {}

    public void setClassBuggyness() {
        this.buggyness = true;
    }

    public boolean isBuggyness() {
        return buggyness;
    }

    public String isBuggy() {
        if(buggyness) return "True";

        return "False";
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public List<Integer> getAddedLinesList() {
        return addedLinesOfCodeList;
    }

    public List<Integer> getDeletedLinesList() {
        return deletedLinesOfCodeList;
    }

    public void setMaxLocAdded(int maxLocAdded) {
        this.maxLocAdded = maxLocAdded;
    }

    public int getMaxLocAdded() {
        return maxLocAdded;
    }

    public void setLocAdded(int locAdded) {
        this.locAdded = locAdded;
    }

    public int getLocAdded() {
        return locAdded;
    }

    public void setAvgLocAdded(double avgLocAdded) {
        this.avgLocAdded = avgLocAdded;
    }

    public double getAvgLocAdded() {
        return avgLocAdded;
    }

    public void setChurn(int churn) {
        this.churn = churn;
    }

    public int getChurn() {
        return churn;
    }

    public void setMaxChurn(int maxChurn) {
        this.maxChurn = maxChurn;
    }

    public int getMaxChurn() {
        return maxChurn;
    }

    public void setAvgChurn(double avgChurn) {
        this.avgChurn = avgChurn;
    }

    public double getAvgChurn() {
        return avgChurn;
    }
}
