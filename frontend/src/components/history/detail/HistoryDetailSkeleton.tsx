import { SkeletonBlock } from "@/components/shared/SkeletonBlock";

export function HistoryDetailSkeleton() {
  return (
    <div className="mx-auto max-w-[1400px] space-y-6 px-4 py-6 md:px-8" aria-busy="true">
      <SkeletonBlock className="h-10 w-64" />
      <SkeletonBlock className="h-12 w-full rounded-xl" />
      <SkeletonBlock className="min-h-[420px] w-full rounded-xl md:min-h-[520px]" />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <SkeletonBlock className="h-36 rounded-xl" />
        <SkeletonBlock className="h-36 rounded-xl" />
      </div>
    </div>
  );
}
