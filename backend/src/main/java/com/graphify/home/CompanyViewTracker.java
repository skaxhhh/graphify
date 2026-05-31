package com.graphify.home;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyViewTracker {

    private final CompanyViewStatsRepository companyViewStatsRepository;

    public CompanyViewTracker(CompanyViewStatsRepository companyViewStatsRepository) {
        this.companyViewStatsRepository = companyViewStatsRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordView(long companyId) {
        companyViewStatsRepository.findById(companyId).ifPresentOrElse(
                stats -> {
                    stats.setViewCount(stats.getViewCount() + 1);
                    companyViewStatsRepository.save(stats);
                },
                () -> {
                    CompanyViewStats created = new CompanyViewStats();
                    created.setCompanyId(companyId);
                    created.setViewCount(1);
                    created.setUpdatedAt(Instant.now());
                    companyViewStatsRepository.save(created);
                }
        );
    }
}
