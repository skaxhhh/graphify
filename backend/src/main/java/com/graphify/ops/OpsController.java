package com.graphify.ops;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/npo/ops")
public class OpsController {

    private final OpsService service;

    public OpsController(OpsService service) {
        this.service = service;
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<BatchJob>> getJobs() {
        return ResponseEntity.ok(service.getAllJobs());
    }

    @GetMapping("/jobs/failed")
    public ResponseEntity<List<BatchJob>> getFailedJobs() {
        return ResponseEntity.ok(service.getFailedJobs());
    }

    @PostMapping("/jobs/{id}/retry")
    public ResponseEntity<BatchJob> retryJob(@PathVariable String id) {
        try {
            return ResponseEntity.ok(service.retryJob(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/closing/today")
    public ResponseEntity<ClosingReport> getTodayClosing() {
        return ResponseEntity.ok(service.getTodayClosing());
    }

    @GetMapping("/closing/yesterday")
    public ResponseEntity<ClosingReport> getYesterdayClosing() {
        return ResponseEntity.ok(service.getYesterdayClosing());
    }
}
