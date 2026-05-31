package com.graphify.company;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyInsightCardRepository extends JpaRepository<CompanyInsightCard, Long> {

    List<CompanyInsightCard> findByCompanyIdOrderBySortOrderAsc(Long companyId);
}
