import { useCallback, useEffect, useMemo } from "react";
import {
  Background,
  ReactFlow,
  ReactFlowProvider,
  useEdgesState,
  useNodesState,
  useReactFlow,
  type NodeMouseHandler,
  type ReactFlowInstance,
} from "@xyflow/react";
import "@xyflow/react/dist/style.css";
import { buildFlowGraph } from "@/components/graph/graphLayout";
import type { GraphEdge, GraphNode } from "@/types/graph";

interface GraphViewProps {
  graphNodes: GraphNode[];
  graphEdges: GraphEdge[];
  highlightedIds: string[];
  dimmedNodeIds: Set<string>;
  onFlowReady: (instance: ReactFlowInstance) => void;
  onNodeClick: (node: GraphNode, position: { x: number; y: number }) => void;
}

function GraphViewInner({
  graphNodes,
  graphEdges,
  highlightedIds,
  dimmedNodeIds,
  onFlowReady,
  onNodeClick,
}: GraphViewProps) {
  const highlightSet = useMemo(() => new Set(highlightedIds), [highlightedIds]);

  const { nodes: initialNodes, edges: initialEdges } = useMemo(
    () => buildFlowGraph(graphNodes, graphEdges, highlightSet, dimmedNodeIds),
    [graphNodes, graphEdges, highlightSet, dimmedNodeIds]
  );

  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);
  const { fitView } = useReactFlow();

  useEffect(() => {
    const built = buildFlowGraph(graphNodes, graphEdges, highlightSet, dimmedNodeIds);
    setNodes(built.nodes);
    setEdges(built.edges);
  }, [graphNodes, graphEdges, highlightSet, dimmedNodeIds, setNodes, setEdges]);

  useEffect(() => {
    if (graphNodes.length === 0) return;
    const frame = requestAnimationFrame(() => {
      fitView({ padding: 0.3, duration: 150 });
    });
    return () => cancelAnimationFrame(frame);
  }, [graphNodes, graphEdges, fitView]);

  const nodeMap = useMemo(
    () => new Map(graphNodes.map((node) => [node.id, node])),
    [graphNodes]
  );

  const handleNodeClick: NodeMouseHandler = useCallback(
    (event, node) => {
      const data = nodeMap.get(node.id);
      if (!data) return;
      onNodeClick(data, { x: event.clientX, y: event.clientY });
    },
    [nodeMap, onNodeClick]
  );

  return (
    <ReactFlow
      className="h-full w-full"
      nodes={nodes}
      edges={edges}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      onInit={onFlowReady}
      onNodeClick={handleNodeClick}
      fitView
      fitViewOptions={{ padding: 0.25 }}
      minZoom={0.2}
      maxZoom={2}
      proOptions={{ hideAttribution: true }}
    >
      <Background color="#e8e4dc" gap={24} />
    </ReactFlow>
  );
}

export function GraphView(props: GraphViewProps) {
  return (
    <div className="h-full w-full" style={{ width: "100%", height: "100%" }}>
      <ReactFlowProvider>
        <GraphViewInner {...props} />
      </ReactFlowProvider>
    </div>
  );
}
