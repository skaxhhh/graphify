import type { GraphNode } from "@/types/graph";

interface NodeCompanyPopoverProps {
  node: GraphNode | null;
  anchor: { x: number; y: number } | null;
  onClose: () => void;
  onOpenDetail: (nodeId: string) => void;
}

export function NodeCompanyPopover({
  node,
  anchor,
  onClose,
  onOpenDetail,
}: NodeCompanyPopoverProps) {
  if (!node || !anchor) return null;

  return (
    <>
      <button
        type="button"
        className="fixed inset-0 z-40 cursor-default"
        aria-label="닫기"
        onClick={onClose}
      />
      <article
        className="fixed z-50 w-64 rounded-lg border border-warm-border bg-cream p-4 shadow-focus"
        style={{ left: anchor.x, top: anchor.y, transform: "translate(-50%, -110%)" }}
      >
        <h4 className="font-semibold text-charcoal">{node.label}</h4>
        <p className="mt-1 text-xs text-muted-gray">{node.type}</p>
        {node.summary ? (
          <p className="mt-2 text-sm text-muted-gray">{node.summary}</p>
        ) : null}
        <button
          type="button"
          className="mt-3 text-xs text-charcoal underline"
          onClick={() => onOpenDetail(node.id)}
        >
          연결 관계 강조
        </button>
      </article>
    </>
  );
}
