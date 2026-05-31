import type { ReactFlowInstance } from "@xyflow/react";

interface ZoomControlsProps {
  flow: ReactFlowInstance | null;
}

export function ZoomControls({ flow }: ZoomControlsProps) {
  return (
    <aside className="absolute bottom-4 right-4 flex flex-col gap-2 rounded-lg border border-warm-border bg-cream/95 p-2 shadow-sm">
      <button
        type="button"
        className="h-8 w-8 rounded border border-warm-border text-charcoal hover:bg-charcoal/[0.04]"
        onClick={() => flow?.zoomIn({ duration: 150 })}
        aria-label="확대"
      >
        +
      </button>
      <button
        type="button"
        className="h-8 w-8 rounded border border-warm-border text-charcoal hover:bg-charcoal/[0.04]"
        onClick={() => flow?.zoomOut({ duration: 150 })}
        aria-label="축소"
      >
        −
      </button>
      <button
        type="button"
        className="h-8 px-2 rounded border border-warm-border text-xs text-charcoal hover:bg-charcoal/[0.04]"
        onClick={() => flow?.fitView({ duration: 150, padding: 0.2 })}
      >
        Fit
      </button>
    </aside>
  );
}
