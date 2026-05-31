package com.graphify.company;

import com.graphify.agent.AgentSession;
import com.graphify.agent.AgentSessionRepository;
import com.graphify.common.dto.ApiResponse;
import com.graphify.common.exception.GraphifyException;
import com.graphify.company.dto.CompanyGraphDto;
import com.graphify.company.dto.GraphEdgeDto;
import com.graphify.company.dto.GraphNodeDto;
import com.graphify.company.dto.ProvenanceDto;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompanyGraphService {

    private final CompanyRepository companyRepository;
    private final RelationshipNodeRepository nodeRepository;
    private final RelationshipEdgeRepository edgeRepository;
    private final AgentSessionRepository agentSessionRepository;

    public CompanyGraphService(
            CompanyRepository companyRepository,
            RelationshipNodeRepository nodeRepository,
            RelationshipEdgeRepository edgeRepository,
            AgentSessionRepository agentSessionRepository
    ) {
        this.companyRepository = companyRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.agentSessionRepository = agentSessionRepository;
    }

    @Transactional
    public ApiResponse<CompanyGraphDto> getGraph(Long companyId, int depth, String filter) {
        Company company = findCompanyOrThrow(companyId);
        int resolvedDepth = Math.min(Math.max(depth, 1), 3);

        List<RelationshipNode> nodes = nodeRepository
                .findByIdCompanyIdAndDepthLevelLessThanEqualOrderByDepthLevelAsc(companyId, resolvedDepth);

        Set<String> allowedTypes = parseRelationTypes(filter);
        Set<String> visibleNodeIds = nodes.stream()
                .map(n -> n.getId().getNodeId())
                .collect(Collectors.toSet());

        List<GraphEdgeDto> edges = edgeRepository.findByCompanyId(companyId).stream()
                .filter(edge -> visibleNodeIds.contains(edge.getSourceNodeId())
                        && visibleNodeIds.contains(edge.getTargetNodeId()))
                .filter(edge -> allowedTypes.isEmpty() || allowedTypes.contains(edge.getRelationType()))
                .map(this::toEdgeDto)
                .toList();

        if (allowedTypes.isEmpty()) {
            // relation filter only — keep all visible nodes
        } else {
            Set<String> connected = new HashSet<>();
            edges.forEach(edge -> {
                connected.add(edge.source());
                connected.add(edge.target());
            });
            nodes = nodes.stream()
                    .filter(n -> n.getDepthLevel() == 0 || connected.contains(n.getId().getNodeId()))
                    .toList();
        }

        List<GraphNodeDto> nodeDtos = nodes.stream().map(this::toNodeDto).toList();

        AgentSession session = new AgentSession(companyId);
        agentSessionRepository.save(session);

        ProvenanceDto provenance = new ProvenanceDto(
                List.of("공시", "뉴스", "IR"),
                company.getUpdatedAt(),
                List.of("disclosure-api", "news-api", "finance-api")
        );

        return ApiResponse.ok(new CompanyGraphDto(
                nodeDtos,
                edges,
                session.getId().toString(),
                provenance
        ));
    }

    private Company findCompanyOrThrow(Long companyId) {
        if (companyId == null || companyId <= 0) {
            throw new GraphifyException(
                    "ERR_COMPANY_001",
                    "기업 정보를 찾을 수 없습니다.",
                    HttpStatus.NOT_FOUND
            );
        }
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new GraphifyException(
                        "ERR_COMPANY_001",
                        "기업 정보를 찾을 수 없습니다.",
                        HttpStatus.NOT_FOUND
                ));
    }

    private GraphNodeDto toNodeDto(RelationshipNode node) {
        return new GraphNodeDto(
                node.getId().getNodeId(),
                node.getLabel(),
                node.getNodeType(),
                node.getSummary(),
                node.getDegree(),
                node.getClusterId()
        );
    }

    private GraphEdgeDto toEdgeDto(RelationshipEdge edge) {
        return new GraphEdgeDto(
                edge.getId(),
                edge.getSourceNodeId(),
                edge.getTargetNodeId(),
                edge.getRelationType(),
                edge.getStrength(),
                edge.getEvidence(),
                edge.getUpdatedAt()
        );
    }

    private static Set<String> parseRelationTypes(String filter) {
        if (filter == null || filter.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(filter.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }
}
