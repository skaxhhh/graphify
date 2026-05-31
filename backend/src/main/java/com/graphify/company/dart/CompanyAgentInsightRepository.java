package com.graphify.company.dart;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyAgentInsightRepository extends JpaRepository<CompanyAgentInsight, Long> {

    Optional<CompanyAgentInsight> findByCompanyId(Long companyId);
}
