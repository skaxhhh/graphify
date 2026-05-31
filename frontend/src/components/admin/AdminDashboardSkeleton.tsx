import { SkeletonBlock } from "@/components/shared/SkeletonBlock";

export function AdminDashboardSkeleton() {
  return (
    <div className="mx-auto w-full max-w-[1600px] space-y-6">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <SkeletonBlock key={i} className="h-[120px] rounded-xl" />
        ))}
      </div>
      <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
        <SkeletonBlock className="h-64 rounded-xl" />
        <SkeletonBlock className="h-64 rounded-xl" />
      </div>
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <SkeletonBlock className="h-48 rounded-xl" />
        <SkeletonBlock className="h-48 rounded-xl" />
      </div>
    </div>
  );
}
