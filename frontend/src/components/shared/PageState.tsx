import type { ReactNode } from "react";

export type PageStateKind = "loading" | "empty" | "error" | "populated";

interface PageStateProps {
  state: PageStateKind;
  loading?: ReactNode;
  empty?: ReactNode;
  error?: ReactNode;
  children: ReactNode;
}

export function PageState({
  state,
  loading,
  empty,
  error,
  children,
}: PageStateProps) {
  if (state === "loading") {
    return (
      <div className="w-full" role="status" aria-live="polite">
        {loading ?? <DefaultLoading />}
      </div>
    );
  }

  if (state === "empty") {
    return (
      <div className="w-full" role="status">
        {empty ?? <DefaultEmpty />}
      </div>
    );
  }

  if (state === "error") {
    return (
      <div className="w-full" role="alert">
        {error ?? <DefaultError />}
      </div>
    );
  }

  return <>{children}</>;
}

function DefaultLoading() {
  return (
    <div className="space-y-4 p-8">
      <div className="h-8 w-3/4 max-w-md animate-pulse rounded-md bg-light-cream" />
      <div className="h-4 w-1/2 max-w-sm animate-pulse rounded-md bg-light-cream" />
      <div className="h-12 w-full max-w-lg animate-pulse rounded-lg bg-light-cream" />
    </div>
  );
}

function DefaultEmpty() {
  return (
    <p className="p-8 text-center text-muted-gray">
      표시할 데이터가 없습니다.
    </p>
  );
}

function DefaultError() {
  return (
    <div className="m-4 rounded-lg border border-warm-border bg-cream p-4">
      <p className="text-charcoal">오류가 발생했습니다. 잠시 후 다시 시도해 주세요.</p>
    </div>
  );
}
