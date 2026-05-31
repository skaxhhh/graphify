package com.graphify.search;

import com.graphify.company.Company;
import com.graphify.company.CompanyRepository;
import com.graphify.search.dto.AutocompleteItemDto;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SearchService {

    private final CompanyRepository companyRepository;

    public SearchService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public List<AutocompleteItemDto> autocomplete(String query) {
        String normalized = query.trim();
        return companyRepository.searchByKeyword(normalized, PageRequest.of(0, 10)).stream()
                .map(this::toDto)
                .toList();
    }

    public long countCompanies() {
        return companyRepository.count();
    }

    private AutocompleteItemDto toDto(Company company) {
        String matchType = company.getTicker() != null && !company.getTicker().isBlank()
                ? "TICKER"
                : "NAME";
        return new AutocompleteItemDto(
                company.getId(),
                company.getName(),
                company.getTicker(),
                matchType
        );
    }
}
