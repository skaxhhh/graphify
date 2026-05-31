import type { Edge, Node } from "@xyflow/react";
import type { GraphEdge, GraphNode } from "@/types/graph";

export function buildFlowGraph(
  graphNodes: GraphNode[],
  graphEdges: GraphEdge[],
  highlightedIds: Set<string>,
  dimmedNodeIds: Set<string>
): { nodes: Node[]; edges: Edge[] } {
  const center = graphNodes.find((n) => n.degree === 0) ?? graphNodes[0];
  const others = graphNodes.filter((n) => n.id !== center?.id);
  const radius = 220;

  const nodes: Node[] = graphNodes.map((node, index) => {
    let x = 0;
    let y = 0;
    if (center && node.id !== center.id) {
      const idx = others.findIndex((o) => o.id === node.id);
      const angle = (2 * Math.PI * Math.max(idx, index)) / Math.max(others.length, 1);
      x = Math.cos(angle) * radius * (node.degree > 1 ? 1.35 : 1);
      y = Math.sin(angle) * radius * (node.degree > 1 ? 1.35 : 1);
    }

    const highlighted = highlightedIds.has(node.id);
    const dimmed = dimmedNodeIds.has(node.id);

    return {
      id: node.id,
      position: { x, y },
      data: {
        label: node.label,
        summary: node.summary,
        clusterId: node.clusterId,
        nodeType: node.type,
      },
      className: [
        highlighted ? "graph-node-highlight" : "",
        dimmed ? "opacity-30" : "",
      ]
        .filter(Boolean)
        .join(" "),
      style: {
        border: highlighted ? "2px solid #2d2d2d" : "1px solid #e8e4dc",
        borderRadius: 12,
        padding: 8,
        background: "#faf8f4",
        fontSize: 12,
        minWidth: 120,
        textAlign: "center" as const,
      },
    };
  });

  const edges: Edge[] = graphEdges.map((edge) => ({
    id: String(edge.id),
    source: edge.source,
    target: edge.target,
    label: edge.relationType,
    animated: highlightedIds.has(edge.source) || highlightedIds.has(edge.target),
    style: {
      stroke: "#2d2d2d",
      strokeWidth: Math.max(1, edge.strength * 3),
      opacity: dimmedNodeIds.size > 0
        && !dimmedNodeIds.has(edge.source)
        && !dimmedNodeIds.has(edge.target)
          ? 1
          : dimmedNodeIds.size > 0
            ? 0.2
            : 0.85,
    },
  }));

  return { nodes, edges };
}
