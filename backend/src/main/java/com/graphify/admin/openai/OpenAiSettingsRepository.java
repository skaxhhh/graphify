package com.graphify.admin.openai;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OpenAiSettingsRepository extends JpaRepository<OpenAiSettings, Long> {
}
