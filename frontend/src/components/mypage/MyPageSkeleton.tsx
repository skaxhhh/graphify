import { SkeletonBlock } from "@/components/shared/SkeletonBlock";

export function MyPageSkeleton() {
  return (
    <div className="mx-auto max-w-[720px] space-y-6 px-4 py-10 md:px-8" aria-busy="true">
      <SkeletonBlock className="h-24 rounded-xl" />
      <SkeletonBlock className="h-64 rounded-xl" />
      <SkeletonBlock className="h-48 rounded-xl" />
    </div>
  );
}
