import { useCallback, useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { GraphCanvas } from "@/components/graph/GraphCanvas";
import { GraphLeftPanel } from "@/components/graph/GraphLeftPanel";
import { GraphRightPanel } from "@/components/graph/GraphRightPanel";
import { GraphSubheader } from "@/components/graph/GraphSubheader";
import { ClusterMembersPopover } from "@/components/graph/ClusterMembersPopover";
import { MobileBottomSheet } from "@/components/graph/MobileBottomSheet";
import { NodeCompanyPopover } from "@/components/graph/NodeCompanyPopover";
import { StreamingStatusDock } from "@/components/graph/StreamingStatusDock";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { InsightEvidenceModal } from "@/components/shared/InsightEvidenceModal";
import { PageState, type PageStateKind } from "@/components/shared/PageState";
import { ReliabilityCriteriaModal } from "@/components/shared/ReliabilityCriteriaModal";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import { useDebounce } from "@/hooks/useDebounce";
import { subscribeAgentStream } from "@/lib/agentStream";
import { fetchCompanyGraph } from "@/lib/graphApi";
import { fetchCompanyInsights } from "@/lib/companyApi";
import { clearGraphHighlightNodes, readGraphHighlightNodes } from "@/lib/graphSession";
import type { InsightCard } from "@/types/company";
import type { AgentStreamEvent, DimMode, GraphNode, RelationType } from "@/types/graph";

const ALL_RELATIONS: RelationType[] = ["SUPPLY_CHAIN", "INVESTMENT", "PARTNERSHIP", "RISK"];

function parseCompanyId(raw: string | undefined): number | null {
  if (!raw) return null;
  const id = Number(raw);
  if (!Number.isFinite(id) || id <= 0) return null;
  return id;
}

function toFilterParam(types: RelationType[]): string | undefined {
  if (types.length === 0 || types.length === ALL_RELATIONS.length) {
    return undefined;
  }
  return types.join(",");
}

export function GraphVisualizationPage() {
  const { companyId: companyIdParam } = useParams<{ companyId: string }>();
  const companyId = parseCompanyId(companyIdParam);

  const [depth, setDepth] = useState(2);
  const [relationTypes, setRelationTypes] = useState<RelationType[]>(ALL_RELATIONS);
  const [dimMode, setDimMode] = useState<DimMode>("dim");
  const debouncedTypes = useDebounce(relationTypes, 280);
  const debouncedDepth = useDebounce(depth, 280);

  const [highlightedIds, setHighlightedIds] = useState<string[]>(() => readGraphHighlightNodes());
  const [selectedNode, setSelectedNode] = useState<GraphNode | null>(null);
  const [popoverAnchor, setPopoverAnchor] = useState<{ x: number; y: number } | null>(null);
  const [clusterAnchor, setClusterAnchor] = useState<{ x: number; y: number } | null>(null);
  const [clusterId, setClusterId] = useState<string | null>(null);

  const [streamEvents, setStreamEvents] = useState<AgentStreamEvent[]>([]);
  const [streaming, setStreaming] = useState(false);
  const [streamError, setStreamError] = useState(false);

  const [filterSheetOpen, setFilterSheetOpen] = useState(false);
  const [insightSheetOpen, setInsightSheetOpen] = useState(false);
  const [evidenceCard, setEvidenceCard] = useState<InsightCard | null>(null);
  const [reliabilityOpen, setReliabilityOpen] = useState(false);

  useEffect(() => {
    if (highlightedIds.length === 0) return;
    const timer = window.setTimeout(() => {
      clearGraphHighlightNodes();
      setHighlightedIds([]);
    }, 600);
    return () => window.clearTimeout(timer);
  }, [highlightedIds]);

  const graphQuery = useQuery({
    queryKey: ["company-graph", companyId, debouncedDepth, debouncedTypes, dimMode],
    queryFn: async () => {
      const filter = dimMode === "hide" ? toFilterParam(debouncedTypes) : undefined;
      const response = await fetchCompanyGraph(companyId!, {
        depth: debouncedDepth,
        filter,
      });
      return response.data;
    },
    enabled: companyId != null,
    placeholderData: (prev) => prev,
  });

  const insightsQuery = useQuery({
    queryKey: ["company-insights", companyId],
    queryFn: async () => {
      const response = await fetchCompanyInsights(companyId!);
      return response.data;
    },
    enabled: companyId != null,
  });

  const startStream = useCallback((sessionId: string) => {
    setStreaming(true);
    setStreamError(false);
    setStreamEvents([]);
    return subscribeAgentStream(sessionId, {
      onEvent: (event) => setStreamEvents((prev) => [...prev, event]),
      onError: () => {
        setStreamError(true);
        setStreaming(false);
      },
      onComplete: () => setStreaming(false),
    });
  }, []);

  useEffect(() => {
    const sessionId = graphQuery.data?.sessionId;
    if (!sessionId) return;
    const unsubscribe = startStream(sessionId);
    return unsubscribe;
  }, [graphQuery.data?.sessionId, startStream]);

  const graph = graphQuery.data;
  const nodes = graph?.nodes ?? [];
  const edges = graph?.edges ?? [];

  const dimmedNodeIds = useMemo(() => {
    if (dimMode !== "dim" || relationTypes.length === ALL_RELATIONS.length) {
      return new Set<string>();
    }
    const active = new Set<string>();
    edges
      .filter((edge) => relationTypes.includes(edge.relationType))
      .forEach((edge) => {
        active.add(edge.source);
        active.add(edge.target);
      });
    return new Set(nodes.filter((node) => !active.has(node.id)).map((node) => node.id));
  }, [dimMode, relationTypes, edges, nodes]);

  const clusterMembers = useMemo(
    () => nodes.filter((node) => node.clusterId && node.clusterId === clusterId),
    [nodes, clusterId]
  );

  const pageState: PageStateKind = (() => {
    if (companyId == null) return "empty";
    if (graphQuery.isLoading && !graphQuery.data) return "loading";
    if (graphQuery.isError) return "error";
    if (nodes.length === 0) return "empty";
    return "populated";
  })();

  const depthLoading = graphQuery.isFetching && !!graphQuery.data;

  const filterSummary =
    relationTypes.length === ALL_RELATIONS.length
      ? "관계: 전체"
      : `관계: ${relationTypes.join(", ")}`;

  const handleReset = () => {
    setDepth(2);
    setRelationTypes(ALL_RELATIONS);
    setDimMode("dim");
    setHighlightedIds([]);
    clearGraphHighlightNodes();
    setSelectedNode(null);
    setPopoverAnchor(null);
    graphQuery.refetch();
  };

  const handleNodeClick = (node: GraphNode, position: { x: number; y: number }) => {
    if (node.clusterId) {
      setClusterId(node.clusterId);
      setClusterAnchor(position);
      setSelectedNode(null);
      setPopoverAnchor(null);
      return;
    }
    setSelectedNode(node);
    setPopoverAnchor(position);
    setClusterId(null);
    setClusterAnchor(null);
  };

  return (
    <div className="flex h-full min-h-0 flex-col">
      <PageState
        state={pageState}
        loading={
          <div className="flex min-h-[calc(100vh-4rem)] flex-col">
            <SkeletonBlock className="h-12 w-full" />
            <SkeletonBlock className="m-4 flex-1 rounded-xl" />
          </div>
        }
        empty={
          <EmptyState
            title="표시할 관계 데이터가 없습니다"
            description="필터를 조정하거나 깊이를 늘린 뒤 다시 시도해 주세요."
          />
        }
        error={
          <div className="p-8">
            <ErrorBanner
              message={
                graphQuery.error instanceof Error
                  ? graphQuery.error.message
                  : "그래프를 불러오지 못했습니다."
              }
              onRetry={() => graphQuery.refetch()}
            />
          </div>
        }
      >
        {companyId && graph ? (
          <div className="flex h-full min-h-0 flex-col overflow-hidden">
            <GraphSubheader
              companyId={companyId}
              depth={depth}
              onDepthChange={setDepth}
              onReset={handleReset}
              provenance={graph.provenance}
              filterSummary={filterSummary}
            />
            <StreamingStatusDock
              events={streamEvents}
              streaming={streaming}
              error={streamError}
              onReconnect={() => graph.sessionId && startStream(graph.sessionId)}
            />
            <div className="relative flex min-h-0 flex-1 overflow-hidden">
              <div className="hidden h-full lg:flex">
                <GraphLeftPanel
                  selectedTypes={relationTypes}
                  onTypesChange={setRelationTypes}
                  dimMode={dimMode}
                  onDimModeChange={setDimMode}
                />
              </div>
              <GraphCanvas
                graphNodes={nodes}
                graphEdges={edges}
                highlightedIds={highlightedIds}
                dimmedNodeIds={dimmedNodeIds}
                loading={false}
                depthLoading={depthLoading}
                onNodeClick={handleNodeClick}
              />
              <div className="hidden h-full lg:flex">
                <GraphRightPanel
                  insights={insightsQuery.data?.cards ?? []}
                  loading={insightsQuery.isLoading}
                  provenance={graph.provenance}
                  onReliability={() => setReliabilityOpen(true)}
                  onInsightDetail={setEvidenceCard}
                />
              </div>
              <div className="pointer-events-none absolute bottom-4 left-4 flex gap-2 lg:hidden">
                <button
                  type="button"
                  className="pointer-events-auto rounded-full bg-charcoal px-4 py-2 text-xs text-off-white shadow-btn-inset"
                  onClick={() => setFilterSheetOpen(true)}
                >
                  필터
                </button>
                <button
                  type="button"
                  className="pointer-events-auto rounded-full bg-charcoal px-4 py-2 text-xs text-off-white shadow-btn-inset"
                  onClick={() => setInsightSheetOpen(true)}
                >
                  인사이트
                </button>
              </div>
            </div>
          </div>
        ) : null}
      </PageState>

      <MobileBottomSheet open={filterSheetOpen} title="필터" onClose={() => setFilterSheetOpen(false)}>
        <GraphLeftPanel
          selectedTypes={relationTypes}
          onTypesChange={setRelationTypes}
          dimMode={dimMode}
          onDimModeChange={setDimMode}
        />
      </MobileBottomSheet>

      <MobileBottomSheet
        open={insightSheetOpen}
        title="인사이트"
        onClose={() => setInsightSheetOpen(false)}
      >
        {graph ? (
          <GraphRightPanel
            insights={insightsQuery.data?.cards ?? []}
            loading={insightsQuery.isLoading}
            provenance={graph.provenance}
            onReliability={() => setReliabilityOpen(true)}
            onInsightDetail={setEvidenceCard}
          />
        ) : null}
      </MobileBottomSheet>

      <NodeCompanyPopover
        node={selectedNode}
        anchor={popoverAnchor}
        onClose={() => {
          setSelectedNode(null);
          setPopoverAnchor(null);
        }}
        onOpenDetail={(nodeId) => setHighlightedIds([nodeId])}
      />
      <ClusterMembersPopover
        clusterId={clusterId}
        members={clusterMembers}
        anchor={clusterAnchor}
        onClose={() => {
          setClusterId(null);
          setClusterAnchor(null);
        }}
      />
      <InsightEvidenceModal
        card={evidenceCard}
        onClose={() => setEvidenceCard(null)}
      />
      <ReliabilityCriteriaModal open={reliabilityOpen} onClose={() => setReliabilityOpen(false)} />
    </div>
  );
}
