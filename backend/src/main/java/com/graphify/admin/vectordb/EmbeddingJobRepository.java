package com.graphify.admin.vectordb;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmbeddingJobRepository extends JpaRepository<EmbeddingJob, Long> {

    List<EmbeddingJob> findTop5ByOrderByCreatedAtDesc();
}
