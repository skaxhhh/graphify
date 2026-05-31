import { SkeletonBlock } from "@/components/shared/SkeletonBlock";

export function VectorDbSkeleton() {
  return (
    <div className="space-y-8" role="status" aria-label="로딩 중">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <SkeletonBlock key={i} className="min-h-[100px] rounded-xl" />
        ))}
      </div>
      <div className="grid gap-8 lg:grid-cols-2">
        <SkeletonBlock className="min-h-[280px] rounded-xl" />
        <SkeletonBlock className="min-h-[280px] rounded-xl" />
      </div>
      <SkeletonBlock className="min-h-[220px] rounded-xl" />
    </div>
  );
}
