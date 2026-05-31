package com.graphify.terms;

import com.graphify.search.SearchService;
import com.graphify.terms.dto.TermItemDto;
import com.graphify.terms.dto.TermsLatestDto;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TermsService {

    private final TermsDocumentRepository termsDocumentRepository;
    private final SearchService searchService;

    public TermsService(TermsDocumentRepository termsDocumentRepository, SearchService searchService) {
        this.termsDocumentRepository = termsDocumentRepository;
        this.searchService = searchService;
    }

    public TermsLatestDto getLatest() {
        List<TermItemDto> terms = termsDocumentRepository.findAllByOrderByTypeAsc().stream()
                .map(doc -> new TermItemDto(
                        doc.getId(),
                        doc.getType(),
                        doc.getTitle(),
                        doc.getVersion(),
                        doc.isRequired(),
                        doc.getContent()
                ))
                .toList();

        String version = terms.isEmpty() ? "0" : terms.getFirst().version();

        return new TermsLatestDto(version, terms, searchService.countCompanies());
    }
}
