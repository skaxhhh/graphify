import { useState } from "react";
import type { ReactFlowInstance } from "@xyflow/react";
import { GraphView } from "@/components/graph/GraphView";
import { ZoomControls } from "@/components/graph/ZoomControls";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import type { GraphEdge, GraphNode } from "@/types/graph";

interface GraphCanvasProps {
  graphNodes: GraphNode[];
  graphEdges: GraphEdge[];
  highlightedIds: string[];
  dimmedNodeIds: Set<string>;
  loading: boolean;
  depthLoading: boolean;
  onNodeClick: (node: GraphNode, position: { x: number; y: number }) => void;
}

export function GraphCanvas({
  graphNodes,
  graphEdges,
  highlightedIds,
  dimmedNodeIds,
  loading,
  depthLoading,
  onNodeClick,
}: GraphCanvasProps) {
  const [flow, setFlow] = useState<ReactFlowInstance | null>(null);

  return (
    <section className="relative min-h-0 w-full flex-1 bg-off-white">
      {loading ? (
        <SkeletonBlock className="absolute inset-4 rounded-xl" />
      ) : (
        <div className="absolute inset-0">
          <GraphView
            graphNodes={graphNodes}
            graphEdges={graphEdges}
            highlightedIds={highlightedIds}
            dimmedNodeIds={dimmedNodeIds}
            onFlowReady={setFlow}
            onNodeClick={onNodeClick}
          />
        </div>
      )}
      {depthLoading ? (
        <div className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center bg-cream/60">
          <span className="inline-block h-8 w-8 animate-spin rounded-full border-2 border-charcoal/20 border-t-charcoal" />
        </div>
      ) : null}
      <ZoomControls flow={flow} />
    </section>
  );
}
