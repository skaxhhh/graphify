package com.graphify.ops;

import java.time.LocalDateTime;

public class BatchJob {
    private String id;
    private String name;
    private String description;
    private Status status;
    private String service;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String errorMessage;
    private int retryCount;

    public enum Status { RUNNING, SUCCESS, FAILED, PENDING }

    public BatchJob(String id, String name, String description, Status status, String service,
                    LocalDateTime startedAt, LocalDateTime finishedAt, String errorMessage, int retryCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.service = service;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Status getStatus() { return status; }
    public String getService() { return service; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public String getErrorMessage() { return errorMessage; }
    public int getRetryCount() { return retryCount; }
    public void setStatus(Status status) { this.status = status; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
