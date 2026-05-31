package com.graphify.terms;

import com.graphify.common.dto.ApiResponse;
import com.graphify.terms.dto.TermsLatestDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/terms")
public class TermsController {

    private final TermsService termsService;

    public TermsController(TermsService termsService) {
        this.termsService = termsService;
    }

    @GetMapping("/latest")
    public ApiResponse<TermsLatestDto> latest() {
        return ApiResponse.ok(termsService.getLatest());
    }
}
