package com.graphify.admin.mcp;

import com.graphify.admin.mcp.dto.McpToolDto;
import com.graphify.admin.mcp.dto.McpToolListDto;
import com.graphify.admin.mcp.dto.McpToolPingResultDto;
import com.graphify.admin.mcp.dto.McpToolUpsertRequest;
import com.graphify.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/tools")
public class AdminMcpToolController {

    private final McpToolService mcpToolService;

    public AdminMcpToolController(McpToolService mcpToolService) {
        this.mcpToolService = mcpToolService;
    }

    @GetMapping
    public ApiResponse<McpToolListDto> listTools(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) String status
    ) {
        return mcpToolService.listTools(q, status);
    }

    @PostMapping
    public ApiResponse<McpToolDto> createTool(@Valid @RequestBody McpToolUpsertRequest request) {
        return mcpToolService.createTool(request);
    }

    @PutMapping("/{id}")
    public ApiResponse<McpToolDto> updateTool(
            @PathVariable Long id,
            @Valid @RequestBody McpToolUpsertRequest request
    ) {
        return mcpToolService.updateTool(id, request);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTool(@PathVariable Long id) {
        return mcpToolService.deleteTool(id);
    }

    @PostMapping("/{id}/ping")
    public ApiResponse<McpToolPingResultDto> pingTool(@PathVariable Long id) {
        return mcpToolService.pingTool(id);
    }
}
