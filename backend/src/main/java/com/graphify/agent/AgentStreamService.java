package com.graphify.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphify.common.exception.GraphifyException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AgentStreamService {

    private static final long SSE_TIMEOUT_MS = 60_000L;

    private final AgentSessionRepository agentSessionRepository;
    private final ObjectMapper objectMapper;

    public AgentStreamService(AgentSessionRepository agentSessionRepository, ObjectMapper objectMapper) {
        this.agentSessionRepository = agentSessionRepository;
        this.objectMapper = objectMapper;
    }

    public SseEmitter stream(String sessionId) {
        UUID uuid;
        try {
            uuid = UUID.fromString(sessionId);
        } catch (IllegalArgumentException ex) {
            throw new GraphifyException(
                    "ERR_AGENT_001",
                    "유효하지 않은 세션입니다.",
                    HttpStatus.BAD_REQUEST
            );
        }

        agentSessionRepository.findByIdAndStatus(uuid, "ACTIVE")
                .orElseThrow(() -> new GraphifyException(
                        "ERR_AGENT_001",
                        "세션을 찾을 수 없습니다.",
                        HttpStatus.NOT_FOUND
                ));

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        List<Map<String, Object>> stages = List.of(
                Map.of("stage", "COLLECT", "message", "공시·뉴스 데이터를 수집하고 있습니다.", "progress", 20),
                Map.of("stage", "NORMALIZE", "message", "관계 데이터를 정규화합니다.", "progress", 45),
                Map.of("stage", "GRAPH", "message", "그래프 노드·엣지를 구성합니다.", "progress", 70),
                Map.of("stage", "INSIGHT", "message", "인사이트 요약을 생성합니다.", "progress", 90),
                Map.of("stage", "DONE", "message", "분석이 완료되었습니다.", "progress", 100)
        );

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                for (Map<String, Object> stage : stages) {
                    emitter.send(SseEmitter.event()
                            .name("progress")
                            .data(toJson(stage), MediaType.APPLICATION_JSON));
                    Thread.sleep(600);
                }
                emitter.complete();
            } catch (IOException | InterruptedException ex) {
                emitter.completeWithError(ex);
                Thread.currentThread().interrupt();
            }
        });

        return emitter;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new GraphifyException(
                    "ERR_AGENT_002",
                    "스트림 메시지 직렬화에 실패했습니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
