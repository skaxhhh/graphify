import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { OpenAiConfigSkeleton } from "@/components/admin/openai/OpenAiConfigSkeleton";
import { OpenAiStatusStrip } from "@/components/admin/openai/OpenAiStatusStrip";
import { TemperatureSlider } from "@/components/admin/openai/TemperatureSlider";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { GhostButton } from "@/components/shared/GhostButton";
import { PageState, type PageStateKind } from "@/components/shared/PageState";
import { PasswordField } from "@/components/shared/PasswordField";
import { PrimaryButton } from "@/components/shared/PrimaryButton";
import { TextField } from "@/components/shared/TextField";
import { ApiRequestError } from "@/lib/apiClient";
import {
  fetchOpenAiConfig,
  fetchOpenAiStatus,
  updateOpenAiConfig,
} from "@/lib/adminOpenAiApi";
import {
  DEFAULT_OPENAI_DRAFT,
  OPENAI_MODEL_OPTIONS,
  type OpenAiConfigUpdatePayload,
} from "@/types/openaiConfig";

type FieldErrors = Partial<Record<keyof OpenAiConfigUpdatePayload, string>>;

function configToDraft(
  c: NonNullable<Awaited<ReturnType<typeof fetchOpenAiConfig>>["data"]>
): OpenAiConfigUpdatePayload {
  return {
    endpointUrl: c.endpointUrl,
    deploymentName: c.deploymentName,
    apiVersion: c.apiVersion,
    model: c.model,
    temperature: Number(c.temperature),
    maxTokens: c.maxTokens,
    topP: Number(c.topP),
    embeddingModel: c.embeddingModel,
    embeddingDeployment: c.embeddingDeployment,
    fallbackEndpoint: c.fallbackEndpoint ?? "",
    fallbackDeploymentName: c.fallbackDeploymentName ?? "",
  };
}

function validateDraft(
  draft: OpenAiConfigUpdatePayload,
  requireApiKey: boolean
): FieldErrors {
  const errors: FieldErrors = {};
  if (!draft.endpointUrl.trim()) {
    errors.endpointUrl = "엔드포인트 URL은 필수입니다.";
  } else if (!draft.endpointUrl.trim().startsWith("https://")) {
    errors.endpointUrl = "https URL이어야 합니다.";
  }
  if (requireApiKey && !draft.apiKey?.trim()) {
    errors.apiKey = "API 키는 필수입니다.";
  }
  if (!draft.deploymentName.trim()) errors.deploymentName = "배포명은 필수입니다.";
  if (!draft.apiVersion.trim()) errors.apiVersion = "API 버전은 필수입니다.";
  if (draft.maxTokens < 1 || draft.maxTokens > 128000) {
    errors.maxTokens = "1~128000 범위여야 합니다.";
  }
  const fb = draft.fallbackEndpoint?.trim();
  if (fb && !fb.startsWith("https://")) {
    errors.fallbackEndpoint = "https URL이어야 합니다.";
  }
  return errors;
}

export function AdminOpenAIConfigPage() {
  const queryClient = useQueryClient();
  const [draft, setDraft] = useState<OpenAiConfigUpdatePayload>(DEFAULT_OPENAI_DRAFT);
  const [baseline, setBaseline] = useState<OpenAiConfigUpdatePayload>(DEFAULT_OPENAI_DRAFT);
  const [apiKey, setApiKey] = useState("");
  const [fallbackApiKey, setFallbackApiKey] = useState("");
  const [hasApiKey, setHasApiKey] = useState(false);
  const [hasFallbackApiKey, setHasFallbackApiKey] = useState(false);
  const [configured, setConfigured] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [toast, setToast] = useState<string | null>(null);
  const lastRefreshRef = useRef(0);

  const configQuery = useQuery({
    queryKey: ["admin", "openai", "config"],
    queryFn: () => fetchOpenAiConfig(),
    retry: (failureCount, error) => {
      if (error instanceof ApiRequestError && error.code.startsWith("ERR_AUTH")) {
        return false;
      }
      return failureCount < 1;
    },
  });

  const statusQuery = useQuery({
    queryKey: ["admin", "openai", "status"],
    queryFn: () => fetchOpenAiStatus(false),
    enabled: configQuery.isSuccess,
    retry: (failureCount, error) => {
      if (error instanceof ApiRequestError && error.code.startsWith("ERR_AUTH")) {
        return false;
      }
      return failureCount < 1;
    },
  });

  const applyFromServer = useCallback(
    (data: NonNullable<typeof configQuery.data>["data"]) => {
      if (!data) return;
      const next = configToDraft(data);
      setDraft(next);
      setBaseline(next);
      setHasApiKey(data.hasApiKey);
      setHasFallbackApiKey(data.hasFallbackApiKey);
      setConfigured(data.configured);
      setApiKey("");
      setFallbackApiKey("");
      setFieldErrors({});
    },
    []
  );

  useEffect(() => {
    if (configQuery.data?.data) {
      applyFromServer(configQuery.data.data);
    }
  }, [applyFromServer, configQuery.data]);

  const dirty = useMemo(() => {
    const keys = Object.keys(draft) as (keyof OpenAiConfigUpdatePayload)[];
    const draftChanged = keys.some(
      (k) => draft[k] !== baseline[k] && k !== "apiKey" && k !== "fallbackApiKey"
    );
    return draftChanged || apiKey.length > 0 || fallbackApiKey.length > 0;
  }, [apiKey, baseline, draft, fallbackApiKey]);

  const pageState: PageStateKind = useMemo(() => {
    if (configQuery.isLoading) return "loading";
    if (configQuery.isError) return "error";
    if (!configured) return "empty";
    return "populated";
  }, [configured, configQuery.isError, configQuery.isLoading]);

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload: OpenAiConfigUpdatePayload = {
        ...draft,
        apiKey: apiKey.trim() || undefined,
        fallbackApiKey: fallbackApiKey.trim() || undefined,
        fallbackEndpoint: draft.fallbackEndpoint?.trim() || undefined,
        fallbackDeploymentName: draft.fallbackDeploymentName?.trim() || undefined,
      };
      return updateOpenAiConfig(payload);
    },
    onSuccess: async (res) => {
      if (res.data) applyFromServer(res.data);
      setToast("저장되었습니다.");
      await queryClient.invalidateQueries({ queryKey: ["admin", "openai"] });
    },
  });

  const handleSave = () => {
    const errors = validateDraft(
      { ...draft, apiKey, fallbackApiKey },
      !hasApiKey
    );
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      const firstKey = Object.keys(errors)[0] as keyof FieldErrors;
      const el = document.getElementById(String(firstKey));
      el?.scrollIntoView({ behavior: "smooth", block: "center" });
      return;
    }
    saveMutation.mutate();
  };

  const handleReload = () => {
    if (configQuery.data?.data) {
      applyFromServer(configQuery.data.data);
    }
  };

  const handleRefreshStatus = () => {
    const now = Date.now();
    if (now - lastRefreshRef.current < 500) return;
    lastRefreshRef.current = now;
    void queryClient.fetchQuery({
      queryKey: ["admin", "openai", "status"],
      queryFn: () => fetchOpenAiStatus(true),
    });
  };

  const updateDraft = <K extends keyof OpenAiConfigUpdatePayload>(
    key: K,
    value: OpenAiConfigUpdatePayload[K]
  ) => {
    setDraft((prev) => ({ ...prev, [key]: value }));
  };

  const formContent = (
    <div className="mx-auto max-w-[960px] space-y-8 md:space-y-10">
      <section className="space-y-4 rounded-xl border border-warm-border bg-cream p-6">
        <h2 className="text-sm font-semibold text-charcoal">연결 기본</h2>
        <TextField
          id="endpointUrl"
          label="Azure OpenAI 엔드포인트 URL"
          value={draft.endpointUrl}
          onChange={(e) => updateDraft("endpointUrl", e.target.value)}
          disabled={saveMutation.isPending}
          className="font-mono text-sm"
          placeholder="https://your-resource.openai.azure.com"
          error={fieldErrors.endpointUrl}
        />
        <PasswordField
          id="apiKey"
          label="API 키"
          value={apiKey}
          onChange={(e) => setApiKey(e.target.value)}
          disabled={saveMutation.isPending}
          placeholder={hasApiKey ? "•••••••• (변경 시에만 입력)" : "API 키 입력"}
          hint="비워 두면 기존 키를 유지합니다."
          error={fieldErrors.apiKey}
        />
        <TextField
          id="deploymentName"
          label="배포명 (Deployment)"
          value={draft.deploymentName}
          onChange={(e) => updateDraft("deploymentName", e.target.value)}
          disabled={saveMutation.isPending}
          error={fieldErrors.deploymentName}
        />
        <TextField
          id="apiVersion"
          label="API 버전"
          value={draft.apiVersion}
          onChange={(e) => updateDraft("apiVersion", e.target.value)}
          disabled={saveMutation.isPending}
          className="font-mono text-sm"
          error={fieldErrors.apiVersion}
        />
      </section>

      <section className="space-y-4 rounded-xl border border-warm-border bg-cream p-6">
        <h2 className="text-sm font-semibold text-charcoal">모델 파라미터</h2>
        <div className="space-y-1.5">
          <label htmlFor="model" className="block text-sm text-charcoal">
            모델
          </label>
          <select
            id="model"
            value={draft.model}
            disabled={saveMutation.isPending}
            onChange={(e) => updateDraft("model", e.target.value)}
            className="h-11 w-full rounded-md border border-warm-border bg-cream px-3 text-sm text-charcoal focus:outline-none focus:ring-2 focus:ring-ring-blue"
          >
            {OPENAI_MODEL_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
        <TemperatureSlider
          value={draft.temperature}
          onChange={(v) => updateDraft("temperature", v)}
          disabled={saveMutation.isPending}
        />
        <TextField
          id="maxTokens"
          label="Max tokens"
          type="number"
          min={1}
          max={128000}
          value={String(draft.maxTokens)}
          onChange={(e) => updateDraft("maxTokens", Number(e.target.value) || 0)}
          disabled={saveMutation.isPending}
          error={fieldErrors.maxTokens}
        />
        <div className="space-y-2">
          <div className="flex items-center justify-between text-sm text-charcoal">
            <span>Top P</span>
            <span className="font-mono text-muted-gray">{draft.topP.toFixed(2)}</span>
          </div>
          <input
            type="range"
            min={0}
            max={1}
            step={0.01}
            value={draft.topP}
            disabled={saveMutation.isPending}
            onChange={(e) => updateDraft("topP", Number(e.target.value))}
            className="h-10 w-full cursor-pointer appearance-none rounded-full bg-light-cream disabled:opacity-50 [&::-webkit-slider-thumb]:h-5 [&::-webkit-slider-thumb]:w-5 [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-charcoal"
            aria-label="Top P"
          />
        </div>
      </section>

      <section className="space-y-4 rounded-xl border border-warm-border bg-cream p-6">
        <h2 className="text-sm font-semibold text-charcoal">임베딩 모델</h2>
        <TextField
          id="embeddingDeployment"
          label="임베딩 배포명"
          value={draft.embeddingDeployment}
          onChange={(e) => updateDraft("embeddingDeployment", e.target.value)}
          disabled={saveMutation.isPending}
        />
        <TextField
          id="embeddingModel"
          label="임베딩 모델"
          value={draft.embeddingModel}
          onChange={(e) => updateDraft("embeddingModel", e.target.value)}
          disabled={saveMutation.isPending}
        />
      </section>

      <section className="space-y-4 rounded-xl border border-warm-border bg-cream p-6">
        <h2 className="text-sm font-semibold text-charcoal">폴백 엔드포인트 (선택)</h2>
        <TextField
          id="fallbackEndpoint"
          label="폴백 URL"
          value={draft.fallbackEndpoint ?? ""}
          onChange={(e) => updateDraft("fallbackEndpoint", e.target.value)}
          disabled={saveMutation.isPending}
          className="font-mono text-sm"
          error={fieldErrors.fallbackEndpoint}
        />
        <PasswordField
          id="fallbackApiKey"
          label="폴백 API 키"
          value={fallbackApiKey}
          onChange={(e) => setFallbackApiKey(e.target.value)}
          disabled={saveMutation.isPending}
          placeholder={
            hasFallbackApiKey ? "•••••••• (변경 시에만 입력)" : "선택 입력"
          }
        />
        <TextField
          id="fallbackDeploymentName"
          label="폴백 배포명"
          value={draft.fallbackDeploymentName ?? ""}
          onChange={(e) => updateDraft("fallbackDeploymentName", e.target.value)}
          disabled={saveMutation.isPending}
        />
      </section>

      <div className="flex flex-wrap items-center gap-3">
        <PrimaryButton
          type="button"
          className="!w-auto md:px-8"
          loading={saveMutation.isPending}
          disabled={!dirty && configured}
          onClick={handleSave}
        >
          저장
        </PrimaryButton>
        <GhostButton type="button" className="!w-auto" onClick={handleReload}>
          취소
        </GhostButton>
        {dirty ? (
          <span className="rounded-md bg-charcoal/10 px-2 py-1 text-xs font-medium text-charcoal">
            변경됨
          </span>
        ) : null}
      </div>

      {saveMutation.isError ? (
        <p className="text-sm text-red-600">
          {saveMutation.error instanceof ApiRequestError
            ? saveMutation.error.message
            : "저장에 실패했습니다."}
        </p>
      ) : null}

      <OpenAiStatusStrip
        status={statusQuery.data?.data ?? null}
        loading={statusQuery.isFetching}
        onRefresh={handleRefreshStatus}
      />
    </div>
  );

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-charcoal">Azure OpenAI 설정</h1>
        <p className="mt-1 text-sm text-muted-gray">
          Agent 추론·임베딩에 사용할 Azure OpenAI 연결 정보를 관리합니다.
        </p>
      </div>

      {toast ? (
        <p className="mb-4 rounded-lg border border-warm-border bg-light-cream/60 px-4 py-2 text-sm text-charcoal">
          {toast}
        </p>
      ) : null}

      <PageState
        state={pageState}
        loading={<OpenAiConfigSkeleton />}
        empty={
          <div className="space-y-8">
            <EmptyState
              title="Azure OpenAI가 아직 설정되지 않았습니다"
              description="엔드포인트 URL과 API 키를 입력한 뒤 저장하세요."
            />
            <PrimaryButton
              type="button"
              className="!w-auto"
              onClick={() => {
                setDraft(DEFAULT_OPENAI_DRAFT);
                setBaseline(DEFAULT_OPENAI_DRAFT);
              }}
            >
              기본값 불러오기
            </PrimaryButton>
            {formContent}
          </div>
        }
        error={
          <ErrorBanner
            message={
              configQuery.error instanceof ApiRequestError
                ? configQuery.error.message
                : "설정을 불러오지 못했습니다."
            }
            onRetry={() => void configQuery.refetch()}
          />
        }
      >
        {formContent}
      </PageState>
    </div>
  );
}
