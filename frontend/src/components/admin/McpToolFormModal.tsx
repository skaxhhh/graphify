import { useEffect, useState } from "react";
import { GhostButton } from "@/components/shared/GhostButton";
import { PrimaryButton } from "@/components/shared/PrimaryButton";
import type { McpAuthType, McpRole, McpTool, McpToolUpsertPayload } from "@/types/mcpTool";

const AUTH_TYPES: McpAuthType[] = ["NONE", "API_KEY", "BEARER"];
const ALL_ROLES: McpRole[] = ["USER", "ADMIN", "PREMIUM"];

export interface McpToolFormValues {
  name: string;
  description: string;
  endpointUrl: string;
  authType: McpAuthType;
  authSecret: string;
  schemaJson: string;
  enabled: boolean;
  allowedRoles: McpRole[];
}

interface McpToolFormModalProps {
  open: boolean;
  mode: "create" | "edit";
  initial?: McpTool | null;
  saving?: boolean;
  onClose: () => void;
  onSave: (payload: McpToolUpsertPayload) => void;
}

function emptyForm(): McpToolFormValues {
  return {
    name: "",
    description: "",
    endpointUrl: "",
    authType: "NONE",
    authSecret: "",
    schemaJson: "",
    enabled: true,
    allowedRoles: ["USER"],
  };
}

function fromTool(tool: McpTool): McpToolFormValues {
  return {
    name: tool.name,
    description: tool.description ?? "",
    endpointUrl: tool.endpointUrl,
    authType: tool.authType,
    authSecret: "",
    schemaJson: tool.schemaJson ?? "",
    enabled: tool.enabled,
    allowedRoles: [...tool.allowedRoles],
  };
}

export function McpToolFormModal({
  open,
  mode,
  initial,
  saving = false,
  onClose,
  onSave,
}: McpToolFormModalProps) {
  const [form, setForm] = useState<McpToolFormValues>(emptyForm);

  useEffect(() => {
    if (!open) return;
    setForm(initial ? fromTool(initial) : emptyForm());
  }, [open, initial]);

  useEffect(() => {
    if (!open) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [open, onClose]);

  if (!open) return null;

  const toggleRole = (role: McpRole) => {
    setForm((prev) => {
      const has = prev.allowedRoles.includes(role);
      const next = has
        ? prev.allowedRoles.filter((r) => r !== role)
        : [...prev.allowedRoles, role];
      return { ...prev, allowedRoles: next.length > 0 ? next : ["USER"] };
    });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const payload: McpToolUpsertPayload = {
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      endpointUrl: form.endpointUrl.trim(),
      authType: form.authType,
      schemaJson: form.schemaJson.trim() || undefined,
      enabled: form.enabled,
      allowedRoles: form.allowedRoles,
    };
    if (form.authSecret.trim()) {
      payload.authSecret = form.authSecret.trim();
    }
    onSave(payload);
  };

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center p-4 animate-[fadeIn_200ms_ease-out]"
      role="dialog"
      aria-modal="true"
      aria-labelledby="mcp-tool-form-title"
    >
      <button
        type="button"
        className="absolute inset-0 bg-charcoal/25"
        aria-label="닫기"
        onClick={onClose}
      />
      <form
        onSubmit={handleSubmit}
        className="relative z-10 flex max-h-[90vh] w-full max-w-[520px] flex-col overflow-hidden rounded-xl border border-warm-border bg-cream shadow-focus"
      >
        <div className="border-b border-warm-border px-6 py-4">
          <h2 id="mcp-tool-form-title" className="text-lg font-semibold text-charcoal">
            {mode === "create" ? "MCP 도구 등록" : "MCP 도구 수정"}
          </h2>
        </div>
        <div className="flex-1 space-y-4 overflow-y-auto px-6 py-4">
          <label className="block text-sm">
            <span className="text-charcoal">이름</span>
            <input
              required
              value={form.name}
              onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
              className="mt-1 h-11 w-full rounded-md border border-warm-border px-3 text-sm"
            />
          </label>
          <label className="block text-sm">
            <span className="text-charcoal">설명</span>
            <textarea
              value={form.description}
              onChange={(e) => setForm((p) => ({ ...p, description: e.target.value }))}
              className="mt-1 min-h-[72px] w-full rounded-md border border-warm-border px-3 py-2 text-sm"
            />
          </label>
          <label className="block text-sm">
            <span className="text-charcoal">엔드포인트 URL</span>
            <input
              required
              type="url"
              value={form.endpointUrl}
              onChange={(e) => setForm((p) => ({ ...p, endpointUrl: e.target.value }))}
              className="mt-1 h-11 w-full rounded-md border border-warm-border px-3 font-mono text-sm"
            />
          </label>
          <label className="block text-sm">
            <span className="text-charcoal">인증 방식</span>
            <select
              value={form.authType}
              onChange={(e) =>
                setForm((p) => ({ ...p, authType: e.target.value as McpAuthType }))
              }
              className="mt-1 h-11 w-full rounded-md border border-warm-border px-3 text-sm"
            >
              {AUTH_TYPES.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </label>
          {form.authType !== "NONE" ? (
            <label className="block text-sm">
              <span className="text-charcoal">인증 시크릿</span>
              <input
                type="password"
                value={form.authSecret}
                onChange={(e) => setForm((p) => ({ ...p, authSecret: e.target.value }))}
                placeholder={mode === "edit" ? "변경 시에만 입력" : ""}
                className="mt-1 h-11 w-full rounded-md border border-warm-border px-3 text-sm"
              />
            </label>
          ) : null}
          <label className="block text-sm">
            <span className="text-charcoal">스키마 JSON (선택)</span>
            <textarea
              value={form.schemaJson}
              onChange={(e) => setForm((p) => ({ ...p, schemaJson: e.target.value }))}
              className="mt-1 min-h-[100px] w-full rounded-md border border-warm-border px-3 py-2 font-mono text-xs"
            />
          </label>
          <fieldset>
            <legend className="text-sm text-charcoal">허용 역할</legend>
            <div className="mt-2 flex flex-wrap gap-3">
              {ALL_ROLES.map((role) => (
                <label key={role} className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={form.allowedRoles.includes(role)}
                    onChange={() => toggleRole(role)}
                  />
                  {role}
                </label>
              ))}
            </div>
          </fieldset>
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={form.enabled}
              onChange={(e) => setForm((p) => ({ ...p, enabled: e.target.checked }))}
            />
            활성화
          </label>
        </div>
        <div className="flex flex-col gap-2 border-t border-warm-border px-6 py-4 sm:flex-row sm:justify-end">
          <GhostButton type="button" onClick={onClose} disabled={saving}>
            취소
          </GhostButton>
          <PrimaryButton type="submit" className="!w-auto sm:min-w-[120px]" loading={saving}>
            저장
          </PrimaryButton>
        </div>
      </form>
    </div>
  );
}
