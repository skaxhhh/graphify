import type { GraphNode } from "@/types/graph";

interface ClusterMembersPopoverProps {
  clusterId: string | null;
  members: GraphNode[];
  anchor: { x: number; y: number } | null;
  onClose: () => void;
}

export function ClusterMembersPopover({
  clusterId,
  members,
  anchor,
  onClose,
}: ClusterMembersPopoverProps) {
  if (!clusterId || !anchor || members.length === 0) return null;

  return (
    <>
      <button type="button" className="fixed inset-0 z-40" aria-label="닫기" onClick={onClose} />
      <article
        className="fixed z-50 w-72 rounded-lg border border-warm-border bg-cream p-4 shadow-focus"
        style={{ left: anchor.x, top: anchor.y, transform: "translate(-50%, -110%)" }}
      >
        <h4 className="text-sm font-semibold text-charcoal">클러스터 멤버</h4>
        <ul className="mt-2 space-y-1 text-sm text-muted-gray">
          {members.map((member) => (
            <li key={member.id}>{member.label}</li>
          ))}
        </ul>
      </article>
    </>
  );
}
