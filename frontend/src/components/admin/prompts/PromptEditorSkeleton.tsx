import { SkeletonBlock } from "@/components/shared/SkeletonBlock";

export function PromptEditorSkeleton() {
  return (
    <div className="flex min-h-0 flex-1 flex-col gap-4 lg:flex-row">
      <div className="flex min-w-0 flex-1 flex-col gap-4">
        <SkeletonBlock className="h-10 w-full max-w-md rounded-md" />
        <SkeletonBlock className="min-h-[240px] w-full rounded-md" />
        <SkeletonBlock className="min-h-[200px] w-full rounded-md" />
        <SkeletonBlock className="h-11 w-40 rounded-md" />
      </div>
      <SkeletonBlock className="h-64 w-full rounded-md lg:h-auto lg:w-[320px]" />
    </div>
  );
}
