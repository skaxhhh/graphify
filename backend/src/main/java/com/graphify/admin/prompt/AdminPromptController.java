package com.graphify.admin.prompt;

import com.graphify.admin.prompt.dto.AgentPromptDetailDto;
import com.graphify.admin.prompt.dto.AgentPromptRollbackRequest;
import com.graphify.admin.prompt.dto.AgentPromptSaveRequest;
import com.graphify.admin.prompt.dto.AgentPromptTestRequest;
import com.graphify.admin.prompt.dto.AgentPromptTestResultDto;
import com.graphify.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/prompts")
public class AdminPromptController {

    private final AgentPromptService agentPromptService;

    public AdminPromptController(AgentPromptService agentPromptService) {
        this.agentPromptService = agentPromptService;
    }

    @GetMapping
    public ApiResponse<AgentPromptDetailDto> getPrompt(
            @RequestParam("type") String type
    ) {
        return agentPromptService.getPrompt(type);
    }

    @PostMapping
    public ApiResponse<AgentPromptDetailDto> savePrompt(
            @Valid @RequestBody AgentPromptSaveRequest request
    ) {
        return agentPromptService.savePrompt(request);
    }

    @PostMapping("/{id}/rollback")
    public ApiResponse<AgentPromptDetailDto> rollbackPrompt(
            @PathVariable Long id,
            @Valid @RequestBody AgentPromptRollbackRequest request
    ) {
        return agentPromptService.rollbackPrompt(id, request);
    }

    @PostMapping("/{id}/test")
    public ApiResponse<AgentPromptTestResultDto> testPrompt(
            @PathVariable Long id,
            @Valid @RequestBody AgentPromptTestRequest request
    ) {
        return agentPromptService.testPrompt(id, request);
    }
}
