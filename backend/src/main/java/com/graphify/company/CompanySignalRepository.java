package com.graphify.company;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanySignalRepository extends JpaRepository<CompanySignal, Long> {

    List<CompanySignal> findByCompanyIdOrderBySortOrderAsc(Long companyId);
}
