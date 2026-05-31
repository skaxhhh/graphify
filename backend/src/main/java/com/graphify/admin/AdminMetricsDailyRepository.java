package com.graphify.admin;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminMetricsDailyRepository extends JpaRepository<AdminMetricsDaily, LocalDate> {

    List<AdminMetricsDaily> findByMetricDateBetweenOrderByMetricDateAsc(
            LocalDate from,
            LocalDate to
    );
}
