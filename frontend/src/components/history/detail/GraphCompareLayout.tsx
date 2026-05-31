import type { ReactFlowInstance } from "@xyflow/react";
import { GraphSnapshotView } from "@/components/history/detail/GraphSnapshotView";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import { GraphView } from "@/components/graph/GraphView";
import type { GraphEdge, GraphNode } from "@/types/graph";

interface GraphCompareLayoutProps {
  compareEnabled: boolean;
  snapshotNodes: GraphNode[];
  snapshotEdges: GraphEdge[];
  liveNodes: GraphNode[];
  liveEdges: GraphEdge[];
  liveLoading: boolean;
}

export function GraphCompareLayout({
  compareEnabled,
  snapshotNodes,
  snapshotEdges,
  liveNodes,
  liveEdges,
  liveLoading,
}: GraphCompareLayoutProps) {
  if (!compareEnabled) {
    return (
      <GraphSnapshotView nodes={snapshotNodes} edges={snapshotEdges} label="분석 시점" />
    );
  }

  return (
    <div
      className="grid min-h-[420px] grid-cols-1 gap-4 transition-all duration-300 ease-out lg:min-h-[520px] lg:grid-cols-2"
    >
      <GraphSnapshotView nodes={snapshotNodes} edges={snapshotEdges} label="과거" />
      <div className="flex min-h-[420px] flex-col overflow-hidden rounded-xl border border-warm-border bg-cream md:min-h-[520px]">
        <p className="border-b border-warm-border px-4 py-2 text-xs text-muted-gray">현재</p>
        <div className="relative min-h-[380px] flex-1 md:min-h-[480px]">
          {liveLoading ? (
            <SkeletonBlock className="absolute inset-4 rounded-lg" />
          ) : liveNodes.length === 0 ? (
            <p className="flex h-full items-center justify-center text-sm text-muted-gray">
              현재 그래프를 불러오지 못했습니다.
            </p>
          ) : (
            <GraphView
              graphNodes={liveNodes}
              graphEdges={liveEdges}
              highlightedIds={[]}
              dimmedNodeIds={new Set()}
              onFlowReady={(_instance: ReactFlowInstance) => {}}
              onNodeClick={() => {}}
            />
          )}
        </div>
      </div>
    </div>
  );
}
