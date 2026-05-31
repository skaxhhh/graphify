package com.graphify.company.dart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphify.admin.prompt.AgentPrompt;
import com.graphify.admin.prompt.AgentPromptRepository;
import com.graphify.agent.AzureChatCompletionClient;
import com.graphify.common.dto.ApiResponse;
import com.graphify.common.exception.GraphifyException;
import com.graphify.company.Company;
import com.graphify.company.CompanyRepository;
import com.graphify.company.dto.AgentInsightDto;
import com.graphify.company.dto.CompanyDartProfileDto;
import com.graphify.company.market.CompanyMarketTechnicalService;
import com.graphify.company.market.MarketTechnicalContextFormatter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyInsightAgentService {

    private static final Logger log = LoggerFactory.getLogger(CompanyInsightAgentService.class);
    private static final String TASK_INSIGHT = "INSIGHT_SUMMARY";
    private static final String TASK_SIGNALS = "RISK_DETECTION";
    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final String CONTEXT_FALLBACK_SUFFIX = "\n\n---\n수집 데이터:\n";
    private static final String MARKET_TECHNICAL_FALLBACK_SUFFIX = "\n\n---\n시장·기술 지표:\n";
    private static final String SIGNAL_JSON_INSTRUCTION = """

            위 데이터만 근거로 리스크·기회 신호를 JSON으로만 출력하세요 (마크다운 금지):
            {"risks":[{"label":"신호 제목","sources":["공시|뉴스|재무"]}],"opportunities":[{"label":"신호 제목","sources":["공시|뉴스|재무"]}]}
            각 배열 2~4개, label은 80자 이내.
            """;

    private final CompanyRepository companyRepository;
    private final CompanyDartSnapshotRepository snapshotRepository;
    private final CompanyAgentInsightRepository agentInsightRepository;
    private final CompanyAgentSignalRepository agentSignalRepository;
    private final AgentPromptRepository agentPromptRepository;
    private final CompanyDartProfileMapper profileMapper;
    private final AzureChatCompletionClient chatClient;
    private final ObjectMapper objectMapper;
    private final CompanyMarketTechnicalService marketTechnicalService;

    public CompanyInsightAgentService(
            CompanyRepository companyRepository,
            CompanyDartSnapshotRepository snapshotRepository,
            CompanyAgentInsightRepository agentInsightRepository,
            CompanyAgentSignalRepository agentSignalRepository,
            AgentPromptRepository agentPromptRepository,
            CompanyDartProfileMapper profileMapper,
            AzureChatCompletionClient chatClient,
            ObjectMapper objectMapper,
            CompanyMarketTechnicalService marketTechnicalService
    ) {
        this.companyRepository = companyRepository;
        this.snapshotRepository = snapshotRepository;
        this.agentInsightRepository = agentInsightRepository;
        this.agentSignalRepository = agentSignalRepository;
        this.agentPromptRepository = agentPromptRepository;
        this.profileMapper = profileMapper;
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.marketTechnicalService = marketTechnicalService;
    }

    @Transactional
    public ApiResponse<AgentInsightDto> generate(Long companyId) {
        Company company = requireCompany(companyId);
        CompanyDartSnapshot snapshot = requireSnapshot(companyId);
        CompanyDartProfileDto profile = requireProfile(snapshot);

        String context = profileMapper.buildAgentContext(profile, company.getName(), company.getSummary());
        String marketTechnical = marketTechnicalService.resolveOptional(company)
                .map(MarketTechnicalContextFormatter::format)
                .orElse("");

        AgentInsightDto insight = generateInsight(companyId, company, profile, context, marketTechnical);
        generateSignals(companyId, company, profile, context, marketTechnical);

        return ApiResponse.ok(insight);
    }

    @Transactional(readOnly = true)
    public AgentInsightDto findInsight(Long companyId) {
        return agentInsightRepository.findByCompanyId(companyId)
                .map(this::toInsightDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<CompanyAgentSignal> findSignals(Long companyId) {
        return agentSignalRepository.findByCompanyIdOrderBySortOrderAsc(companyId);
    }

    private AgentInsightDto generateInsight(
            Long companyId,
            Company company,
            CompanyDartProfileDto profile,
            String context,
            String marketTechnical
    ) {
        AgentPrompt prompt = requirePrompt(TASK_INSIGHT);
        String userMessage = buildUserMessage(
                prompt.getTaskTemplate(),
                company.getName(),
                context,
                marketTechnical,
                null
        );

        AzureChatCompletionClient.CompletionResult completion = chatClient
                .complete(prompt.getSystemPrompt(), userMessage)
                .orElse(null);

        String content;
        String modelLabel;
        if (completion != null) {
            content = completion.content();
            modelLabel = completion.modelLabel();
        } else {
            log.warn("INSIGHT_SUMMARY LLM 호출 실패 — mock 사용 companyId={}", companyId);
            content = buildMockInsight(company, profile, prompt);
            modelLabel = "mock-dev";
        }

        CompanyAgentInsight insight = agentInsightRepository.findByCompanyId(companyId).orElseGet(CompanyAgentInsight::new);
        insight.setCompanyId(companyId);
        insight.setTaskType(TASK_INSIGHT);
        insight.setContent(content);
        insight.setModelLabel(modelLabel);
        insight.setStatus("READY");
        insight.setGeneratedAt(Instant.now());
        agentInsightRepository.save(insight);
        return toInsightDto(insight);
    }

    private void generateSignals(
            Long companyId,
            Company company,
            CompanyDartProfileDto profile,
            String context,
            String marketTechnical
    ) {
        agentSignalRepository.deleteByCompanyId(companyId);

        AgentPrompt prompt = requirePrompt(TASK_SIGNALS);
        String userMessage = buildUserMessage(
                prompt.getTaskTemplate(),
                company.getName(),
                context,
                marketTechnical,
                SIGNAL_JSON_INSTRUCTION
        );

        AzureChatCompletionClient.CompletionResult completion = chatClient
                .complete(prompt.getSystemPrompt(), userMessage)
                .orElse(null);

        List<CompanyAgentSignal> parsed = completion != null
                ? parseSignals(companyId, completion.content())
                : List.of();

        if (parsed.isEmpty()) {
            log.warn("RISK_DETECTION LLM 파싱 실패 — mock 신호 사용 companyId={}", companyId);
            parsed = buildMockSignals(companyId, profile);
        }

        agentSignalRepository.saveAll(parsed);
    }

    private List<CompanyAgentSignal> parseSignals(Long companyId, String rawContent) {
        try {
            String json = extractJson(rawContent);
            JsonNode root = objectMapper.readTree(json);
            List<CompanyAgentSignal> signals = new ArrayList<>();
            int order = 0;
            order = appendSignals(signals, companyId, "RISK", root.path("risks"), order);
            appendSignals(signals, companyId, "OPPORTUNITY", root.path("opportunities"), order);
            return signals;
        } catch (Exception ex) {
            log.warn("신호 JSON 파싱 실패: {}", ex.getMessage());
            return List.of();
        }
    }

    private int appendSignals(
            List<CompanyAgentSignal> target,
            Long companyId,
            String kind,
            JsonNode arrayNode,
            int startOrder
    ) {
        int order = startOrder;
        if (!arrayNode.isArray()) {
            return order;
        }
        for (JsonNode node : arrayNode) {
            String label = text(node, "label");
            if (label == null) {
                continue;
            }
            CompanyAgentSignal signal = new CompanyAgentSignal();
            signal.setCompanyId(companyId);
            signal.setSignalKind(kind);
            signal.setLabel(label);
            signal.setRationale(text(node, "rationale"));
            signal.setSources(joinSources(node.path("sources")));
            signal.setSortOrder(order++);
            signal.setGeneratedAt(Instant.now());
            target.add(signal);
        }
        return order;
    }

    private static String extractJson(String raw) {
        Matcher matcher = JSON_BLOCK.matcher(raw);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw.trim();
    }

    private static String joinSources(JsonNode sourcesNode) {
        if (!sourcesNode.isArray()) {
            return null;
        }
        List<String> sources = new ArrayList<>();
        for (JsonNode node : sourcesNode) {
            String value = node.asText(null);
            if (value != null && !value.isBlank()) {
                sources.add(value.trim());
            }
        }
        return sources.isEmpty() ? null : String.join(",", sources);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String raw = value.asText().trim();
        return raw.isEmpty() ? null : raw;
    }

    private List<CompanyAgentSignal> buildMockSignals(Long companyId, CompanyDartProfileDto profile) {
        List<CompanyAgentSignal> signals = new ArrayList<>();
        int order = 0;

        if (!profile.recentDisclosures().isEmpty()) {
            var latest = profile.recentDisclosures().get(0);
            CompanyAgentSignal risk = new CompanyAgentSignal();
            risk.setCompanyId(companyId);
            risk.setSignalKind("RISK");
            risk.setLabel("최근 공시: " + truncate(latest.reportName(), 60));
            risk.setSources("공시");
            risk.setSortOrder(order++);
            risk.setGeneratedAt(Instant.now());
            signals.add(risk);
        }

        if (!profile.financialStatements().isEmpty()) {
            CompanyAgentSignal risk = new CompanyAgentSignal();
            risk.setCompanyId(companyId);
            risk.setSignalKind("RISK");
            risk.setLabel("분기 재무 변동성 — DART 주요계정 추이 모니터링 필요");
            risk.setSources("재무");
            risk.setSortOrder(order++);
            risk.setGeneratedAt(Instant.now());
            signals.add(risk);
        }

        if (!profile.relatedNews().isEmpty()) {
            CompanyAgentSignal opp = new CompanyAgentSignal();
            opp.setCompanyId(companyId);
            opp.setSignalKind("OPPORTUNITY");
            opp.setLabel("관련 뉴스 모멘텀: " + truncate(profile.relatedNews().get(0).title(), 50));
            opp.setSources("뉴스");
            opp.setSortOrder(order++);
            opp.setGeneratedAt(Instant.now());
            signals.add(opp);
        }

        if (signals.isEmpty()) {
            CompanyAgentSignal placeholder = new CompanyAgentSignal();
            placeholder.setCompanyId(companyId);
            placeholder.setSignalKind("RISK");
            placeholder.setLabel("수집 데이터 기반 신호 생성 대기");
            placeholder.setSortOrder(0);
            placeholder.setGeneratedAt(Instant.now());
            signals.add(placeholder);
        }
        return signals;
    }

    private Company requireCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new GraphifyException(
                        "ERR_COMPANY_001",
                        "기업 정보를 찾을 수 없습니다.",
                        HttpStatus.NOT_FOUND
                ));
    }

    private CompanyDartSnapshot requireSnapshot(Long companyId) {
        return snapshotRepository.findById(companyId)
                .orElseThrow(() -> new GraphifyException(
                        "ERR_COMPANY_005",
                        "DART 수집 데이터가 없습니다. 먼저 동기화를 실행해 주세요.",
                        HttpStatus.BAD_REQUEST
                ));
    }

    private CompanyDartProfileDto requireProfile(CompanyDartSnapshot snapshot) {
        CompanyDartProfileDto profile = profileMapper.toProfileDto(snapshot);
        if (profile == null) {
            throw new GraphifyException(
                    "ERR_COMPANY_004",
                    "수집 데이터를 해석하지 못했습니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return profile;
    }

    private AgentPrompt requirePrompt(String taskType) {
        return agentPromptRepository.findByTaskType(taskType)
                .orElseThrow(() -> new GraphifyException(
                        "ERR_ADMIN_PROMPT_001",
                        taskType + " 프롬프트가 설정되지 않았습니다.",
                        HttpStatus.NOT_FOUND
                ));
    }

    private static String buildUserMessage(
            String template,
            String companyName,
            String context,
            String marketTechnical,
            String signalJsonAppend
    ) {
        String safeTemplate = template != null ? template : "";
        String signalInstruction = signalJsonAppend != null ? signalJsonAppend.trim() : "";
        String resolved = applyTemplate(safeTemplate, companyName, context, marketTechnical, signalInstruction);

        if (!safeTemplate.contains("{{context}}") && context != null && !context.isBlank()) {
            resolved = resolved + CONTEXT_FALLBACK_SUFFIX + context;
        }
        if (!safeTemplate.contains("{{market_technical}}")
                && marketTechnical != null
                && !marketTechnical.isBlank()) {
            resolved = resolved + MARKET_TECHNICAL_FALLBACK_SUFFIX + marketTechnical;
        }
        if (signalJsonAppend != null
                && !signalJsonAppend.isBlank()
                && !safeTemplate.contains("{{signal_json_instruction}}")) {
            resolved = resolved + "\n\n" + signalJsonAppend.trim();
        }
        return resolved;
    }

    private static String applyTemplate(
            String template,
            String companyName,
            String context,
            String marketTechnical,
            String signalJsonInstruction
    ) {
        return template
                .replace("{{company_name}}", companyName != null ? companyName : "")
                .replace("{{context}}", context != null ? context : "")
                .replace("{{market_technical}}", marketTechnical != null ? marketTechnical : "")
                .replace("{{signal_json_instruction}}", signalJsonInstruction != null ? signalJsonInstruction : "");
    }

    private AgentInsightDto toInsightDto(CompanyAgentInsight insight) {
        return new AgentInsightDto(
                insight.getContent(),
                insight.getModelLabel(),
                insight.getStatus(),
                insight.getGeneratedAt()
        );
    }

    private static String buildMockInsight(Company company, CompanyDartProfileDto profile, AgentPrompt prompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(company.getName()).append(" — 투자·관계 인사이트 (dev mock)\n\n");
        sb.append("**시장·개요**: ");
        if (profile.corpClassLabel() != null) {
            sb.append(profile.corpClassLabel()).append(" 상장");
        } else {
            sb.append("비상장/시장 미확인");
        }
        if (profile.ceoName() != null) {
            sb.append(", 대표 ").append(profile.ceoName());
        }
        sb.append(".\n\n");
        if (!profile.financialStatements().isEmpty()) {
            sb.append("**재무**: DART 주요계정 ").append(profile.financialStatements().size()).append("건 수집.\n\n");
        }
        if (!profile.recentDisclosures().isEmpty()) {
            sb.append("**최근 공시** (").append(profile.recentDisclosures().size()).append("건)\n");
            profile.recentDisclosures().stream().limit(3).forEach(d -> {
                sb.append("- ").append(d.receiptDate() != null ? d.receiptDate() : "—");
                sb.append(" ").append(d.reportName()).append('\n');
            });
            sb.append('\n');
        }
        if (!profile.relatedNews().isEmpty()) {
            sb.append("**뉴스**: ").append(profile.relatedNews().size()).append("건 수집.\n\n");
        }
        sb.append("**참고**: OpenAI 배포명(`OPENAI_DEPLOYMENT`)이 게이트웨이와 일치해야 실제 LLM 응답이 생성됩니다.\n");
        sb.append("_시스템 지침 요약_: ").append(truncate(prompt.getSystemPrompt(), 100));
        return sb.toString();
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
}
