package com.graphify.admin.vectordb;

import com.graphify.admin.vectordb.dto.CleanupRequest;
import com.graphify.admin.vectordb.dto.CleanupPreviewDto;
import com.graphify.admin.vectordb.dto.CleanupResultDto;
import com.graphify.admin.vectordb.dto.EmbeddingJobDto;
import com.graphify.admin.vectordb.dto.ReindexRequest;
import com.graphify.admin.vectordb.dto.ReindexResultDto;
import com.graphify.admin.vectordb.dto.VectorDbStatsDto;
import com.graphify.common.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/vectordb")
public class AdminVectorDbController {

    private final VectorDbAdminService vectorDbAdminService;

    public AdminVectorDbController(VectorDbAdminService vectorDbAdminService) {
        this.vectorDbAdminService = vectorDbAdminService;
    }

    @GetMapping("/stats")
    public ApiResponse<VectorDbStatsDto> getStats() {
        return vectorDbAdminService.getStats();
    }

    @PostMapping("/reindex")
    public ApiResponse<ReindexResultDto> reindex(@Valid @RequestBody ReindexRequest request) {
        return vectorDbAdminService.startReindex(request);
    }

    @GetMapping("/jobs/{jobId}")
    public ApiResponse<EmbeddingJobDto> getJob(@PathVariable long jobId) {
        return vectorDbAdminService.getJob(jobId);
    }

    @GetMapping("/cleanup/preview")
    public ApiResponse<CleanupPreviewDto> previewCleanup(
            @RequestParam int olderThanDays,
            @RequestParam String types
    ) {
        List<String> typeList = Arrays.stream(types.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return vectorDbAdminService.previewCleanup(olderThanDays, typeList);
    }

    @DeleteMapping("/cleanup")
    public ApiResponse<CleanupResultDto> cleanup(@Valid @RequestBody CleanupRequest request) {
        return vectorDbAdminService.runCleanup(request);
    }
}
