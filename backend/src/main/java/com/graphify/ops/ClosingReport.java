package com.graphify.ops;

import java.time.LocalDate;
import java.util.List;

public class ClosingReport {
    private LocalDate targetDate;
    private String overallStatus; // OK, WARNING, FAILED
    private List<ClosingItem> items;
    private String summary;

    public ClosingReport(LocalDate targetDate, String overallStatus, List<ClosingItem> items, String summary) {
        this.targetDate = targetDate;
        this.overallStatus = overallStatus;
        this.items = items;
        this.summary = summary;
    }

    public LocalDate getTargetDate() { return targetDate; }
    public String getOverallStatus() { return overallStatus; }
    public List<ClosingItem> getItems() { return items; }
    public String getSummary() { return summary; }

    public static class ClosingItem {
        private String checkName;
        private String category; // SALES, SETTLEMENT, INVENTORY, APPROVAL
        private String status;   // OK, WARNING, FAILED, PENDING
        private String detail;
        private Object value;

        public ClosingItem(String checkName, String category, String status, String detail, Object value) {
            this.checkName = checkName;
            this.category = category;
            this.status = status;
            this.detail = detail;
            this.value = value;
        }

        public String getCheckName() { return checkName; }
        public String getCategory() { return category; }
        public String getStatus() { return status; }
        public String getDetail() { return detail; }
        public Object getValue() { return value; }
    }
}
