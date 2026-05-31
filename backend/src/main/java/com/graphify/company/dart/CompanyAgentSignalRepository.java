package com.graphify.company.dart;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyAgentSignalRepository extends JpaRepository<CompanyAgentSignal, Long> {

    List<CompanyAgentSignal> findByCompanyIdOrderBySortOrderAsc(Long companyId);

    void deleteByCompanyId(Long companyId);

    boolean existsByCompanyId(Long companyId);
}
