package com.graphify.admin.prompt;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentPromptVersionRepository extends JpaRepository<AgentPromptVersion, Long> {

    List<AgentPromptVersion> findByPromptIdOrderByCreatedAtDesc(Long promptId);

    Optional<AgentPromptVersion> findByIdAndPromptId(Long id, Long promptId);

    Optional<AgentPromptVersion> findTopByPromptIdOrderByVersionNumberDesc(Long promptId);
}
