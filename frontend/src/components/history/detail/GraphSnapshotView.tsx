import { useMemo } from "react";
import type { ReactFlowInstance } from "@xyflow/react";
import { GraphView } from "@/components/graph/GraphView";
import type { GraphEdge, GraphNode } from "@/types/graph";

interface GraphSnapshotViewProps {
  nodes: GraphNode[];
  edges: GraphEdge[];
  label?: string;
  onFlowReady?: (instance: ReactFlowInstance) => void;
}

export function GraphSnapshotView({
  nodes,
  edges,
  label = "과거",
  onFlowReady,
}: GraphSnapshotViewProps) {
  const empty = nodes.length === 0;

  const dimmed = useMemo(() => new Set<string>(), []);

  if (empty) {
    return (
      <div className="flex min-h-[420px] items-center justify-center rounded-xl border border-warm-border bg-cream p-6 text-sm text-muted-gray">
        저장된 그래프 스냅샷이 없습니다.
      </div>
    );
  }

  return (
    <div className="flex min-h-[420px] flex-col overflow-hidden rounded-xl border border-warm-border bg-cream md:min-h-[520px]">
      <p className="border-b border-warm-border px-4 py-2 text-xs text-muted-gray">{label}</p>
      <div className="relative min-h-[380px] flex-1 md:min-h-[480px]">
        <GraphView
          graphNodes={nodes}
          graphEdges={edges}
          highlightedIds={[]}
          dimmedNodeIds={dimmed}
          onFlowReady={onFlowReady ?? (() => {})}
          onNodeClick={() => {}}
        />
      </div>
    </div>
  );
}
