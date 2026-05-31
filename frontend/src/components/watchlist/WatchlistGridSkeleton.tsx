import { SkeletonBlock } from "@/components/shared/SkeletonBlock";

export function WatchlistGridSkeleton() {
  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
      {Array.from({ length: 6 }).map((_, index) => (
        <SkeletonBlock key={index} className="h-48 rounded-xl" />
      ))}
    </div>
  );
}
