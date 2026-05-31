package com.graphify.terms;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsDocumentRepository extends JpaRepository<TermsDocument, Long> {

    List<TermsDocument> findAllByOrderByTypeAsc();
}
