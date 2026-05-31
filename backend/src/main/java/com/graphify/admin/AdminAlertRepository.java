package com.graphify.admin;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAlertRepository extends JpaRepository<AdminAlert, Long> {

    List<AdminAlert> findTop10ByOrderByDetectedAtDesc();
}
