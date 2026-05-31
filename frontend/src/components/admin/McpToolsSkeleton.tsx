import { SkeletonBlock } from "@/components/shared/SkeletonBlock";

export function McpToolsSkeleton() {
  return (
    <div className="space-y-4">
      <SkeletonBlock className="h-11 w-full max-w-md" />
      <SkeletonBlock className="h-64 w-full rounded-xl" />
    </div>
  );
}
