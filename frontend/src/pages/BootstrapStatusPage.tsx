import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { PageState, type PageStateKind } from "@/components/shared/PageState";
import { fetchActuatorHealth, fetchBootstrapStatus } from "@/lib/api";

export function BootstrapStatusPage() {
  const healthQuery = useQuery({
    queryKey: ["actuator", "health"],
    queryFn: fetchActuatorHealth,
  });

  const bootstrapQuery = useQuery({
    queryKey: ["bootstrap", "status"],
    queryFn: fetchBootstrapStatus,
  });

  const pageState: PageStateKind = (() => {
    if (healthQuery.isLoading || bootstrapQuery.isLoading) return "loading";
    if (healthQuery.isError || bootstrapQuery.isError) return "error";
    if (
      healthQuery.data?.status !== "UP" ||
      !bootstrapQuery.data?.success
    ) {
      return "empty";
    }
    return "populated";
  })();

  return (
    <div className="mx-auto w-full max-w-[720px] px-4 py-16">
      <h1 className="text-3xl font-semibold tracking-tight text-charcoal">
        graphify 부트스트랩
      </h1>
      <p className="mt-2 text-muted-gray">
        T01 — 레이아웃 셸·API·DB 연결 확인 (SCREEN_FLOW §3)
      </p>

      <PageState
        state={pageState}
        error={
          <div className="mt-8 rounded-lg border border-warm-border p-6">
            <p className="text-charcoal">백엔드에 연결할 수 없습니다.</p>
            <p className="mt-2 text-sm text-muted-gray">
              PostgreSQL: <code>docker compose up -d</code>
              <br />
              API: <code>./gradlew bootRun</code> (backend)
            </p>
            <button
              type="button"
              className="mt-4 text-sm underline"
              onClick={() => {
                void healthQuery.refetch();
                void bootstrapQuery.refetch();
              }}
            >
              재시도
            </button>
          </div>
        }
        empty={
          <div className="mt-8 rounded-lg border border-warm-border p-6 text-muted-gray">
            헬스 상태가 UP이 아니거나 bootstrap API가 실패했습니다.
          </div>
        }
      >
        <dl className="mt-8 space-y-4 rounded-xl border border-warm-border bg-cream p-6">
          <div>
            <dt className="text-sm text-muted-gray">Actuator</dt>
            <dd className="font-medium text-charcoal">
              {healthQuery.data?.status ?? "—"}
            </dd>
          </div>
          <div>
            <dt className="text-sm text-muted-gray">Bootstrap API</dt>
            <dd className="font-medium text-charcoal">
              {bootstrapQuery.data?.data?.service ?? "—"} /{" "}
              {bootstrapQuery.data?.data?.phase ?? "—"}
            </dd>
          </div>
        </dl>

        <nav className="mt-8 flex flex-wrap gap-3 text-sm">
          <Link to="/" className="underline">
            Guest 홈 (S01 placeholder)
          </Link>
          <Link to="/app" className="underline">
            User 레이아웃
          </Link>
          <Link to="/admin" className="underline">
            Admin 레이아웃
          </Link>
        </nav>
      </PageState>
    </div>
  );
}
