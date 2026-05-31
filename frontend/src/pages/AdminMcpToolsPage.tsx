import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { DeleteMcpToolDialog } from "@/components/admin/DeleteMcpToolDialog";
import { McpToolFormModal } from "@/components/admin/McpToolFormModal";
import { McpToolsMobileList } from "@/components/admin/McpToolsMobileList";
import { McpToolsSkeleton } from "@/components/admin/McpToolsSkeleton";
import { McpToolsTable } from "@/components/admin/McpToolsTable";
import { McpToolsToolbar } from "@/components/admin/McpToolsToolbar";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { PageState, type PageStateKind } from "@/components/shared/PageState";
import { PrimaryButton } from "@/components/shared/PrimaryButton";
import { useDebounce } from "@/hooks/useDebounce";
import { ApiRequestError } from "@/lib/apiClient";
import {
  createMcpTool,
  deleteMcpTool,
  fetchMcpTools,
  pingMcpTool,
  updateMcpTool,
} from "@/lib/adminMcpApi";
import type { McpConnectionStatus, McpTool, McpToolUpsertPayload } from "@/types/mcpTool";

export function AdminMcpToolsPage() {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<McpConnectionStatus | "ALL">("ALL");
  const debouncedQuery = useDebounce(query, 200);

  const [modalOpen, setModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<"create" | "edit">("create");
  const [editingTool, setEditingTool] = useState<McpTool | null>(null);

  const [deleteTarget, setDeleteTarget] = useState<McpTool | null>(null);
  const [pingLoadingId, setPingLoadingId] = useState<number | null>(null);
  const [pingMessageById, setPingMessageById] = useState<Record<number, string>>({});
  const [toggleLoadingId, setToggleLoadingId] = useState<number | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  const toolsQuery = useQuery({
    queryKey: ["admin", "mcp-tools", debouncedQuery, status],
    queryFn: async () => {
      const res = await fetchMcpTools({ q: debouncedQuery, status });
      return res.data?.tools ?? [];
    },
    retry: (failureCount, error) => {
      if (error instanceof ApiRequestError && error.code.startsWith("ERR_AUTH")) {
        return false;
      }
      return failureCount < 1;
    },
  });

  const tools = toolsQuery.data ?? [];

  const pageState: PageStateKind = useMemo(() => {
    if (toolsQuery.isLoading) return "loading";
    if (toolsQuery.isError) return "error";
    if (tools.length === 0) return "empty";
    return "populated";
  }, [tools.length, toolsQuery.isError, toolsQuery.isLoading]);

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ["admin", "mcp-tools"] });

  const saveMutation = useMutation({
    mutationFn: async ({
      mode,
      id,
      payload,
    }: {
      mode: "create" | "edit";
      id?: number;
      payload: McpToolUpsertPayload;
    }) => {
      if (mode === "create") {
        return createMcpTool(payload);
      }
      if (id == null) throw new Error("missing id");
      return updateMcpTool(id, payload);
    },
    onSuccess: async () => {
      setModalOpen(false);
      setEditingTool(null);
      setToast("저장되었습니다.");
      await invalidate();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteMcpTool(id),
    onSuccess: async () => {
      setDeleteTarget(null);
      setToast("삭제되었습니다.");
      await invalidate();
    },
  });

  const openCreate = () => {
    setModalMode("create");
    setEditingTool(null);
    setModalOpen(true);
  };

  const openEdit = (tool: McpTool) => {
    setModalMode("edit");
    setEditingTool(tool);
    setModalOpen(true);
  };

  const handlePing = async (tool: McpTool) => {
    setPingLoadingId(tool.id);
    try {
      const res = await pingMcpTool(tool.id);
      const result = res.data;
      if (!result) {
        throw new ApiRequestError("ERR_ADMIN_MCP", "Ping 응답이 없습니다.");
      }
      const msg = result.ok
        ? `OK · ${result.latencyMs}ms`
        : `실패 · ${result.message}`;
      setPingMessageById((prev) => ({ ...prev, [tool.id]: msg }));
      await invalidate();
      window.setTimeout(() => {
        setPingMessageById((prev) => {
          const next = { ...prev };
          delete next[tool.id];
          return next;
        });
      }, 2000);
    } catch (err) {
      const message =
        err instanceof ApiRequestError ? err.message : "Ping에 실패했습니다.";
      setPingMessageById((prev) => ({ ...prev, [tool.id]: message }));
    } finally {
      setPingLoadingId(null);
    }
  };

  const handleToggleEnabled = async (tool: McpTool, enabled: boolean) => {
    setToggleLoadingId(tool.id);
    const previous = tool.enabled;
    queryClient.setQueryData<McpTool[]>(
      ["admin", "mcp-tools", debouncedQuery, status],
      (old) =>
        old?.map((t) => (t.id === tool.id ? { ...t, enabled } : t)) ?? old
    );
    try {
      await updateMcpTool(tool.id, {
        name: tool.name,
        description: tool.description,
        endpointUrl: tool.endpointUrl,
        authType: tool.authType,
        schemaJson: tool.schemaJson ?? undefined,
        enabled,
        allowedRoles: tool.allowedRoles,
      });
      await invalidate();
    } catch (err) {
      queryClient.setQueryData<McpTool[]>(
        ["admin", "mcp-tools", debouncedQuery, status],
        (old) =>
          old?.map((t) =>
            t.id === tool.id ? { ...t, enabled: previous } : t
          ) ?? old
      );
      const message =
        err instanceof ApiRequestError
          ? err.message
          : "활성 상태 변경에 실패했습니다.";
      setToast(message);
    } finally {
      setToggleLoadingId(null);
    }
  };

  return (
    <div className="mx-auto w-full max-w-[1400px]">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-charcoal">MCP 도구</h1>
          <p className="mt-1 text-sm text-muted-gray">
            Agent가 사용하는 MCP 연동을 등록·점검합니다.
          </p>
        </div>
        <PrimaryButton type="button" className="!w-auto" onClick={openCreate}>
          신규 등록
        </PrimaryButton>
      </div>

      {toast ? (
        <p className="mb-4 rounded-lg border border-warm-border bg-light-cream/60 px-4 py-2 text-sm text-charcoal">
          {toast}
        </p>
      ) : null}

      <PageState
        state={pageState}
        loading={<McpToolsSkeleton />}
        empty={
          <div className="space-y-4">
            <EmptyState
              title="등록된 MCP 도구가 없습니다"
              description="신규 등록으로 첫 도구를 추가해 보세요."
            />
            <div className="flex justify-center">
              <PrimaryButton type="button" className="!w-auto" onClick={openCreate}>
                신규 등록
              </PrimaryButton>
            </div>
          </div>
        }
        error={
          <ErrorBanner
            message={
              toolsQuery.error instanceof ApiRequestError
                ? toolsQuery.error.message
                : "MCP 도구 목록을 불러오지 못했습니다."
            }
            onRetry={() => void toolsQuery.refetch()}
          />
        }
      >
        <div className="space-y-4">
          <McpToolsToolbar
            query={query}
            status={status}
            onQueryChange={setQuery}
            onStatusChange={setStatus}
          />
          <McpToolsTable
            tools={tools}
            pingLoadingId={pingLoadingId}
            pingMessageById={pingMessageById}
            toggleLoadingId={toggleLoadingId}
            onPing={handlePing}
            onEdit={openEdit}
            onDelete={setDeleteTarget}
            onToggleEnabled={handleToggleEnabled}
          />
          <McpToolsMobileList
            tools={tools}
            onPing={handlePing}
            onEdit={openEdit}
            onDelete={setDeleteTarget}
          />
        </div>
      </PageState>

      <McpToolFormModal
        open={modalOpen}
        mode={modalMode}
        initial={editingTool}
        saving={saveMutation.isPending}
        onClose={() => {
          setModalOpen(false);
          setEditingTool(null);
        }}
        onSave={(payload) =>
          saveMutation.mutate({
            mode: modalMode,
            id: editingTool?.id,
            payload,
          })
        }
      />

      <DeleteMcpToolDialog
        open={deleteTarget != null}
        toolName={deleteTarget?.name ?? ""}
        loading={deleteMutation.isPending}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => {
          if (deleteTarget) deleteMutation.mutate(deleteTarget.id);
        }}
      />
    </div>
  );
}
