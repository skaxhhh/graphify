package com.graphify.admin.prompt;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentPromptRepository extends JpaRepository<AgentPrompt, Long> {

    Optional<AgentPrompt> findByTaskType(String taskType);
}
