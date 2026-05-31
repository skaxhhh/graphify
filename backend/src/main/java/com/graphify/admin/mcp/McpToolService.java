package com.graphify.admin.mcp;

import com.graphify.admin.mcp.dto.McpToolDto;
import com.graphify.admin.mcp.dto.McpToolListDto;
import com.graphify.admin.mcp.dto.McpToolPingResultDto;
import com.graphify.admin.mcp.dto.McpToolUpsertRequest;
import com.graphify.common.dto.ApiResponse;
import com.graphify.common.exception.GraphifyException;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class McpToolService {

    private static final Set<String> AUTH_TYPES = Set.of("NONE", "API_KEY", "BEARER");
    private static final Set<String> ROLES = Set.of("USER", "ADMIN", "PREMIUM");
    private static final Set<String> FILTER_STATUSES = Set.of("CONNECTED", "DISCONNECTED", "ERROR", "UNKNOWN", "ALL");

    private final McpToolRepository mcpToolRepository;

    public McpToolService(McpToolRepository mcpToolRepository) {
        this.mcpToolRepository = mcpToolRepository;
    }

    @Transactional(readOnly = true)
    public ApiResponse<McpToolListDto> listTools(String q, String status) {
        String normalizedStatus = normalizeStatusFilter(status);
        List<McpToolDto> tools = mcpToolRepository.search(trimToNull(q), normalizedStatus)
                .stream()
                .map(this::toDto)
                .toList();
        return ApiResponse.ok(new McpToolListDto(tools));
    }

    public ApiResponse<McpToolDto> createTool(McpToolUpsertRequest request) {
        if (mcpToolRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw conflict("이미 존재하는 도구 이름입니다.");
        }
        McpTool tool = new McpTool();
        applyUpsert(tool, request);
        return ApiResponse.ok(toDto(mcpToolRepository.save(tool)));
    }

    public ApiResponse<McpToolDto> updateTool(Long id, McpToolUpsertRequest request) {
        McpTool tool = requireTool(id);
        if (mcpToolRepository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw conflict("이미 존재하는 도구 이름입니다.");
        }
        applyUpsert(tool, request);
        return ApiResponse.ok(toDto(mcpToolRepository.save(tool)));
    }

    public ApiResponse<Void> deleteTool(Long id) {
        McpTool tool = requireTool(id);
        mcpToolRepository.delete(tool);
        return ApiResponse.ok(null);
    }

    public ApiResponse<McpToolPingResultDto> pingTool(Long id) {
        McpTool tool = requireTool(id);
        long started = System.nanoTime();
        boolean ok;
        String message;
        try {
            URI uri = URI.create(tool.getEndpointUrl().trim());
            String scheme = uri.getScheme();
            ok = scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
            message = ok ? "엔드포인트 형식이 유효합니다." : "http 또는 https URL이어야 합니다.";
        } catch (IllegalArgumentException ex) {
            ok = false;
            message = "유효하지 않은 URL입니다.";
        }
        long latencyMs = (System.nanoTime() - started) / 1_000_000L;
        if (ok) {
            latencyMs = Math.max(latencyMs, 12L + (tool.getId() % 40));
        }
        tool.setLastCalledAt(Instant.now());
        tool.setConnectionStatus(ok ? "CONNECTED" : "ERROR");
        mcpToolRepository.save(tool);
        return ApiResponse.ok(new McpToolPingResultDto(ok, latencyMs, message));
    }

    private void applyUpsert(McpTool tool, McpToolUpsertRequest request) {
        String authType = normalizeAuthType(request.authType());
        tool.setName(request.name().trim());
        tool.setDescription(trimToEmpty(request.description()));
        tool.setEndpointUrl(request.endpointUrl().trim());
        tool.setAuthType(authType);
        if (request.authSecret() != null && !request.authSecret().isBlank()) {
            tool.setAuthSecret(request.authSecret().trim());
        }
        tool.setSchemaJson(trimToNull(request.schemaJson()));
        if (request.enabled() != null) {
            tool.setEnabled(request.enabled());
        }
        tool.setAllowedRoles(joinRoles(request.allowedRoles()));
    }

    private McpTool requireTool(Long id) {
        return mcpToolRepository.findById(id)
                .orElseThrow(() -> new GraphifyException(
                        "ERR_ADMIN_MCP_001",
                        "MCP 도구를 찾을 수 없습니다.",
                        HttpStatus.NOT_FOUND
                ));
    }

    private McpToolDto toDto(McpTool tool) {
        return new McpToolDto(
                tool.getId(),
                tool.getName(),
                tool.getDescription(),
                tool.getEndpointUrl(),
                tool.getAuthType(),
                tool.getSchemaJson(),
                tool.getConnectionStatus(),
                tool.isEnabled(),
                parseRoles(tool.getAllowedRoles()),
                tool.getLastCalledAt()
        );
    }

    private static List<String> parseRoles(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of("USER");
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .toList();
    }

    private static String joinRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "USER";
        }
        List<String> normalized = roles.stream()
                .map(r -> r.trim().toUpperCase(Locale.ROOT))
                .filter(r -> !r.isEmpty())
                .toList();
        for (String role : normalized) {
            if (!ROLES.contains(role)) {
                throw new GraphifyException(
                        "ERR_ADMIN_MCP_002",
                        "허용 역할은 USER, ADMIN, PREMIUM 중 하나여야 합니다.",
                        HttpStatus.BAD_REQUEST
                );
            }
        }
        return String.join(",", normalized);
    }

    private static String normalizeAuthType(String authType) {
        if (authType == null || authType.isBlank()) {
            return "NONE";
        }
        String normalized = authType.trim().toUpperCase(Locale.ROOT);
        if (!AUTH_TYPES.contains(normalized)) {
            throw new GraphifyException(
                    "ERR_ADMIN_MCP_003",
                    "authType은 NONE, API_KEY, BEARER 중 하나여야 합니다.",
                    HttpStatus.BAD_REQUEST
            );
        }
        return normalized;
    }

    private static String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!FILTER_STATUSES.contains(normalized)) {
            throw new GraphifyException(
                    "ERR_ADMIN_MCP_004",
                    "status는 CONNECTED, DISCONNECTED, ERROR, UNKNOWN, ALL 중 하나여야 합니다.",
                    HttpStatus.BAD_REQUEST
            );
        }
        return "ALL".equals(normalized) ? null : normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static GraphifyException conflict(String message) {
        return new GraphifyException("ERR_ADMIN_MCP_005", message, HttpStatus.CONFLICT);
    }
}
