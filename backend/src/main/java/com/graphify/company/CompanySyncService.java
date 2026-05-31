package com.graphify.company;

import com.graphify.common.dto.ApiResponse;
import com.graphify.common.exception.GraphifyException;
import com.graphify.company.dto.CompanyDetailDto;
import com.graphify.company.dart.CompanyDartCollectionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanySyncService {

    private final CompanyDartCollectionService dartCollectionService;
    private final CompanyDetailService companyDetailService;

    public CompanySyncService(
            CompanyDartCollectionService dartCollectionService,
            CompanyDetailService companyDetailService
    ) {
        this.dartCollectionService = dartCollectionService;
        this.companyDetailService = companyDetailService;
    }

    @Transactional
    public ApiResponse<CompanyDetailDto> syncDetail(Long id) {
        dartCollectionService.collectFromDart(id);
        return companyDetailService.getDetail(id);
    }
}
