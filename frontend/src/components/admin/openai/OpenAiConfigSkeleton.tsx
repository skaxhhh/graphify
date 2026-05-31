import { SkeletonBlock } from "@/components/shared/SkeletonBlock";

export function OpenAiConfigSkeleton() {
  return (
    <div className="mx-auto max-w-[960px] space-y-10">
      {[1, 2, 3, 4].map((i) => (
        <div
          key={i}
          className="space-y-4 rounded-xl border border-warm-border bg-cream p-6"
        >
          <SkeletonBlock className="h-5 w-32 rounded" />
          <SkeletonBlock className="h-11 w-full rounded-md" />
          <SkeletonBlock className="h-11 w-full rounded-md" />
          <SkeletonBlock className="h-11 w-3/4 rounded-md" />
        </div>
      ))}
    </div>
  );
}
