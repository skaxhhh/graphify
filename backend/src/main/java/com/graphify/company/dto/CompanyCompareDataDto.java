package com.graphify.company.dto;

import java.util.List;

public record CompanyCompareDataDto(String basis, List<CompareCompanyDto> companies) {
}
