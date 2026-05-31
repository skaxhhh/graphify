package com.graphify.history;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

final class AnalysisHistorySpecifications {

    private AnalysisHistorySpecifications() {
    }

    static Specification<AnalysisHistory> forUser(
            Long userId,
            String q,
            Instant from,
            Instant to
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));

            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("companyName")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("summaryLine"), "")), pattern)
                ));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("analyzedAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("analyzedAt"), to));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
