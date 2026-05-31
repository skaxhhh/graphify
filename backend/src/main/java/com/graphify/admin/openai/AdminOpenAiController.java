package com.graphify.admin.openai;

import com.graphify.admin.openai.dto.OpenAiConfigDto;
import com.graphify.admin.openai.dto.OpenAiConfigUpdateRequest;
import com.graphify.admin.openai.dto.OpenAiStatusDto;
import com.graphify.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/openai")
public class AdminOpenAiController {

    private final OpenAiSettingsService openAiSettingsService;

    public AdminOpenAiController(OpenAiSettingsService openAiSettingsService) {
        this.openAiSettingsService = openAiSettingsService;
    }

    @GetMapping("/config")
    public ApiResponse<OpenAiConfigDto> getConfig() {
        return openAiSettingsService.getConfig();
    }

    @PutMapping("/config")
    public ApiResponse<OpenAiConfigDto> updateConfig(
            @Valid @RequestBody OpenAiConfigUpdateRequest request
    ) {
        return openAiSettingsService.updateConfig(request);
    }

    @GetMapping("/status")
    public ApiResponse<OpenAiStatusDto> getStatus(
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh
    ) {
        if (refresh) {
            return openAiSettingsService.refreshStatus();
        }
        return openAiSettingsService.getStatus();
    }
}
