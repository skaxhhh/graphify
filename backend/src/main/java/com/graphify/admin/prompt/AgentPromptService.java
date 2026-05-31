package com.graphify.admin.prompt;

import com.graphify.admin.prompt.dto.AgentPromptDetailDto;
import com.graphify.admin.prompt.dto.AgentPromptRollbackRequest;
import com.graphify.admin.prompt.dto.AgentPromptSaveRequest;
import com.graphify.admin.prompt.dto.AgentPromptTestRequest;
import com.graphify.admin.prompt.dto.AgentPromptTestResultDto;
import com.graphify.admin.prompt.dto.AgentPromptVersionDto;
import com.graphify.admin.prompt.dto.TokenUsageDto;
import com.graphify.common.dto.ApiResponse;
import com.graphify.common.exception.GraphifyException;
import com.graphify.company.Company;
import com.graphify.company.CompanyRepository;
import com.graphify.history.HistoryService;
import com.graphify.user.User;
import com.graphify.user.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AgentPromptService {

    private static final Set<String> TASK_TYPES = Set.of(
            "RELATION_ANALYSIS",
            "RISK_DETECTION",
            "INSIGHT_SUMMARY"
    );

    private final AgentPromptRepository agentPromptRepository;
    private final AgentPromptVersionRepository agentPromptVersionRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    public AgentPromptService(
            AgentPromptRepository agentPromptRepository,
            AgentPromptVersionRepository agentPromptVersionRepository,
            UserRepository userRepository,
            CompanyRepository companyRepository
    ) {
        this.agentPromptRepository = agentPromptRepository;
        this.agentPromptVersionRepository = agentPromptVersionRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public ApiResponse<AgentPromptDetailDto> getPrompt(String type) {
        String taskType = normalizeTaskType(type);
        AgentPrompt prompt = agentPromptRepository.findByTaskType(taskType)
                .orElseThrow(() -> notFound("해당 유형의 프롬프트가 없습니다."));
        return ApiResponse.ok(toDetailDto(prompt));
    }

    public ApiResponse<AgentPromptDetailDto> savePrompt(AgentPromptSaveRequest request) {
        String taskType = normalizeTaskType(request.type());
        validatePromptLength(request.systemPrompt(), request.taskTemplate());

        AgentPrompt prompt = agentPromptRepository.findByTaskType(taskType).orElse(null);
        if (prompt == null) {
            prompt = new AgentPrompt();
            prompt.setTaskType(taskType);
        }
        prompt.setSystemPrompt(request.systemPrompt().trim());
        prompt.setTaskTemplate(request.taskTemplate().trim());
        prompt = agentPromptRepository.save(prompt);

        appendVersion(prompt, request.changeNote(), "저장");
        return ApiResponse.ok(toDetailDto(prompt));
    }

    public ApiResponse<AgentPromptDetailDto> rollbackPrompt(Long promptId, AgentPromptRollbackRequest request) {
        AgentPrompt prompt = requirePrompt(promptId);
        AgentPromptVersion target = agentPromptVersionRepository
                .findByIdAndPromptId(request.targetVersionId(), promptId)
                .orElseThrow(() -> new GraphifyException(
                        "ERR_ADMIN_PROMPT_004",
                        "버전을 찾을 수 없습니다.",
                        HttpStatus.NOT_FOUND
                ));

        prompt.setSystemPrompt(target.getSystemPrompt());
        prompt.setTaskTemplate(target.getTaskTemplate());
        agentPromptRepository.save(prompt);

        String note = "v" + target.getVersionNumber() + " 롤백";
        appendVersion(prompt, note, note);
        return ApiResponse.ok(toDetailDto(prompt));
    }

    public ApiResponse<AgentPromptTestResultDto> testPrompt(Long promptId, AgentPromptTestRequest request) {
        AgentPrompt prompt = requirePrompt(promptId);
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new GraphifyException(
                        "ERR_ADMIN_PROMPT_003",
                        "기업을 찾을 수 없습니다.",
                        HttpStatus.NOT_FOUND
                ));

        String sample = request.sampleInput() == null ? "" : request.sampleInput().trim();
        String output = buildMockOutput(prompt, company, sample);
        int inputTokens = estimateTokens(prompt.getSystemPrompt(), prompt.getTaskTemplate(), sample);
        int outputTokens = Math.max(48, output.length() / 4);
        TokenUsageDto usage = new TokenUsageDto(inputTokens, outputTokens, inputTokens + outputTokens);

        return ApiResponse.ok(new AgentPromptTestResultDto(output, usage, company.getName()));
    }

    private void appendVersion(AgentPrompt prompt, String changeNote, String summaryPrefix) {
        int nextVersion = agentPromptVersionRepository
                .findTopByPromptIdOrderByVersionNumberDesc(prompt.getId())
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        Long authorId = HistoryService.requireCurrentUserId();
        String authorName = userRepository.findById(authorId)
                .map(User::getDisplayName)
                .orElse("관리자");

        String note = trimToNull(changeNote);
        String summary = summaryPrefix;
        if (note != null && !note.isBlank()) {
            summary = summaryPrefix + " — " + note;
        }
        if (summary.length() > 255) {
            summary = summary.substring(0, 252) + "...";
        }

        AgentPromptVersion version = new AgentPromptVersion();
        version.setPrompt(prompt);
        version.setVersionNumber(nextVersion);
        version.setSystemPrompt(prompt.getSystemPrompt());
        version.setTaskTemplate(prompt.getTaskTemplate());
        version.setChangeNote(note);
        version.setAuthorId(authorId);
        version.setAuthorName(authorName);
        version.setSummary(summary);
        agentPromptVersionRepository.save(version);
    }

    private AgentPromptDetailDto toDetailDto(AgentPrompt prompt) {
        List<AgentPromptVersionDto> versions = agentPromptVersionRepository
                .findByPromptIdOrderByCreatedAtDesc(prompt.getId())
                .stream()
                .map(v -> new AgentPromptVersionDto(
                        v.getId(),
                        v.getVersionNumber(),
                        v.getCreatedAt(),
                        v.getAuthorName(),
                        v.getSummary(),
                        v.getChangeNote()
                ))
                .toList();

        return new AgentPromptDetailDto(
                prompt.getId(),
                prompt.getTaskType(),
                prompt.getSystemPrompt(),
                prompt.getTaskTemplate(),
                versions
        );
    }

    private static String buildMockOutput(AgentPrompt prompt, Company company, String sample) {
        String taskLabel = switch (prompt.getTaskType()) {
            case "RISK_DETECTION" -> "리스크 탐지";
            case "INSIGHT_SUMMARY" -> "인사이트 요약";
            default -> "관계 분석";
        };
        StringBuilder sb = new StringBuilder();
        sb.append("[테스트 실행 — ").append(taskLabel).append("]\n\n");
        sb.append("대상: ").append(company.getName());
        if (company.getTicker() != null && !company.getTicker().isBlank()) {
            sb.append(" (").append(company.getTicker()).append(")");
        }
        sb.append("\n\n");
        sb.append("시스템 지침 요약: ")
                .append(truncate(prompt.getSystemPrompt(), 120))
                .append("\n\n");
        sb.append("태스크 템플릿 적용 결과:\n");
        sb.append(truncate(prompt.getTaskTemplate(), 200));
        if (!sample.isBlank()) {
            sb.append("\n\n추가 입력:\n").append(sample);
        }
        sb.append("\n\n※ 로컬 dev 모드 mock 응답입니다. Azure OpenAI 연동(T17) 후 실제 추론으로 대체됩니다.");
        return sb.toString();
    }

    private static int estimateTokens(String systemPrompt, String taskTemplate, String sample) {
        int chars = systemPrompt.length() + taskTemplate.length() + sample.length();
        return Math.max(32, chars / 4);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().replace('\n', ' ');
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max) + "...";
    }

    private AgentPrompt requirePrompt(Long id) {
        return agentPromptRepository.findById(id)
                .orElseThrow(() -> notFound("프롬프트를 찾을 수 없습니다."));
    }

    private static String normalizeTaskType(String type) {
        if (type == null || type.isBlank()) {
            throw new GraphifyException(
                    "ERR_ADMIN_PROMPT_002",
                    "type은 RELATION_ANALYSIS, RISK_DETECTION, INSIGHT_SUMMARY 중 하나여야 합니다.",
                    HttpStatus.BAD_REQUEST
            );
        }
        String normalized = type.trim().toUpperCase(Locale.ROOT);
        if (!TASK_TYPES.contains(normalized)) {
            throw new GraphifyException(
                    "ERR_ADMIN_PROMPT_002",
                    "type은 RELATION_ANALYSIS, RISK_DETECTION, INSIGHT_SUMMARY 중 하나여야 합니다.",
                    HttpStatus.BAD_REQUEST
            );
        }
        return normalized;
    }

    private static void validatePromptLength(String systemPrompt, String taskTemplate) {
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            throw badRequest("systemPrompt는 필수입니다.");
        }
        if (taskTemplate == null || taskTemplate.trim().isEmpty()) {
            throw badRequest("taskTemplate는 필수입니다.");
        }
        if (systemPrompt.length() > 16000 || taskTemplate.length() > 16000) {
            throw badRequest("프롬프트는 각각 16,000자 이하여야 합니다.");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static GraphifyException notFound(String message) {
        return new GraphifyException("ERR_ADMIN_PROMPT_001", message, HttpStatus.NOT_FOUND);
    }

    private static GraphifyException badRequest(String message) {
        return new GraphifyException("ERR_ADMIN_PROMPT_002", message, HttpStatus.BAD_REQUEST);
    }
}
