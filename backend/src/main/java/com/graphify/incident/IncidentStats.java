package com.graphify.incident;

public class IncidentStats {
    private long totalCount;
    private long criticalCount;
    private long highCount;
    private long mediumCount;
    private long lowCount;
    private double mttrMinutes;
    private double recurrenceRate;

    public IncidentStats(long totalCount, long criticalCount, long highCount, long mediumCount,
                         long lowCount, double mttrMinutes, double recurrenceRate) {
        this.totalCount = totalCount;
        this.criticalCount = criticalCount;
        this.highCount = highCount;
        this.mediumCount = mediumCount;
        this.lowCount = lowCount;
        this.mttrMinutes = mttrMinutes;
        this.recurrenceRate = recurrenceRate;
    }

    public long getTotalCount() { return totalCount; }
    public long getCriticalCount() { return criticalCount; }
    public long getHighCount() { return highCount; }
    public long getMediumCount() { return mediumCount; }
    public long getLowCount() { return lowCount; }
    public double getMttrMinutes() { return mttrMinutes; }
    public double getRecurrenceRate() { return recurrenceRate; }
}
