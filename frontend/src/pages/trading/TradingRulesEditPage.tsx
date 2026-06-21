import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiRequestError } from "@/lib/apiClient";
import { createPaperRule, fetchPaperRule, updatePaperRule } from "@/lib/ruleApi";
import type {
  RuleCondition,
  RuleDefinition,
  RuleIndicator,
  RuleLogic,
  RuleOperator,
  RuleUpsertRequest,
  SizingType,
} from "@/types/trading";

// ─── Internal state types ────────────────────────────────────────────────────

interface ConditionRowState {
  leftIndicator: RuleIndicator | "";
  leftPeriod: string;
  op: RuleOperator | "";
  rightType: "value" | "indicator";
  rightValue: string;
  rightIndicator: RuleIndicator | "";
  rightPeriod: string;
}

interface BuilderState {
  name: string;
  universeType: "symbols" | "volume_top_n";
  symbolsInput: string;
  topN: string;
  additionalSymbols: string;
  entryLogic: RuleLogic;
  entryConditions: ConditionRowState[];
  takeProfitPct: string;
  stopLossPct: string;
  exitLogic: RuleLogic;
  exitConditions: ConditionRowState[];
  sizingType: SizingType;
  sizingValue: string;
  cooldownBars: string;
  maxPositionsPerSymbol: string;
}

// ─── Helper constants & functions ────────────────────────────────────────────

const INDICATORS: RuleIndicator[] = ["PRICE", "SMA", "EMA", "RSI", "VOLUME"];
const OPERATORS: RuleOperator[] = [">", ">=", "<", "<=", "==", "crossAbove", "crossBelow"];
const PERIOD_INDICATORS: RuleIndicator[] = ["SMA", "EMA", "RSI"];

function needsPeriod(ind: RuleIndicator | ""): boolean {
  return PERIOD_INDICATORS.includes(ind as RuleIndicator);
}

function emptyConditionRow(): ConditionRowState {
  return {
    leftIndicator: "",
    leftPeriod: "",
    op: "",
    rightType: "value",
    rightValue: "",
    rightIndicator: "",
    rightPeriod: "",
  };
}

const DEFAULT_STATE: BuilderState = {
  name: "",
  universeType: "symbols",
  symbolsInput: "005930",
  topN: "10",
  additionalSymbols: "",
  entryLogic: "AND",
  entryConditions: [emptyConditionRow()],
  takeProfitPct: "",
  stopLossPct: "",
  exitLogic: "OR",
  exitConditions: [],
  sizingType: "cash",
  sizingValue: "1000000",
  cooldownBars: "1",
  maxPositionsPerSymbol: "1",
};

// ─── Serialization: BuilderState → RuleDefinition ────────────────────────────

function conditionRowToRuleCondition(row: ConditionRowState): RuleCondition | null {
  if (!row.leftIndicator || !row.op) return null;
  const left = {
    indicator: row.leftIndicator,
    params: needsPeriod(row.leftIndicator)
      ? { period: parseInt(row.leftPeriod) || undefined }
      : undefined,
  };
  const right =
    row.rightType === "value"
      ? { value: parseFloat(row.rightValue) }
      : {
          indicator: row.rightIndicator || undefined,
          params:
            row.rightIndicator && needsPeriod(row.rightIndicator)
              ? { period: parseInt(row.rightPeriod) || undefined }
              : undefined,
        };
  return { left, op: row.op, right };
}

function toDefinition(s: BuilderState): RuleDefinition {
  const universe: RuleDefinition["universe"] =
    s.universeType === "volume_top_n"
      ? {
          type: "volume_top_n",
          market: "KOSPI",
          topN: parseInt(s.topN) || 10,
          additionalSymbols: s.additionalSymbols
            .split(",")
            .map((v) => v.trim())
            .filter(Boolean),
        }
      : {
          type: "symbols",
          symbols: s.symbolsInput
            .split(",")
            .map((v) => v.trim())
            .filter(Boolean),
        };

  const validEntry = s.entryConditions
    .map(conditionRowToRuleCondition)
    .filter((c): c is RuleCondition => c !== null);

  const validExit = s.exitConditions
    .map(conditionRowToRuleCondition)
    .filter((c): c is RuleCondition => c !== null);

  const takeProfitPct =
    s.takeProfitPct !== "" ? parseFloat(s.takeProfitPct) : undefined;
  const stopLossPct =
    s.stopLossPct !== "" ? parseFloat(s.stopLossPct) : undefined;

  const hasExit =
    (takeProfitPct !== undefined && !isNaN(takeProfitPct)) ||
    (stopLossPct !== undefined && !isNaN(stopLossPct)) ||
    validExit.length > 0;

  const exit: RuleDefinition["exit"] = hasExit
    ? {
        takeProfitPct:
          takeProfitPct !== undefined && !isNaN(takeProfitPct)
            ? takeProfitPct
            : undefined,
        stopLossPct:
          stopLossPct !== undefined && !isNaN(stopLossPct)
            ? stopLossPct
            : undefined,
        ...(validExit.length > 0
          ? { logic: s.exitLogic, conditions: validExit }
          : {}),
      }
    : undefined;

  return {
    version: 1,
    universe,
    entry: { logic: s.entryLogic, conditions: validEntry },
    exit,
    sizing: { type: s.sizingType, value: parseFloat(s.sizingValue) || 0 },
    constraints: {
      cooldownBars: parseInt(s.cooldownBars) || 1,
      maxPositionsPerSymbol: parseInt(s.maxPositionsPerSymbol) || 1,
    },
  };
}

// ─── Deserialization: RuleDefinition → BuilderState ──────────────────────────

function ruleConditionToRow(c: RuleCondition): ConditionRowState {
  return {
    leftIndicator: (c.left.indicator as RuleIndicator | undefined) ?? "",
    leftPeriod: c.left.params?.period !== undefined ? String(c.left.params.period) : "",
    op: c.op,
    rightType: c.right.value !== undefined ? "value" : "indicator",
    rightValue: c.right.value !== undefined ? String(c.right.value) : "",
    rightIndicator: (c.right.indicator as RuleIndicator | undefined) ?? "",
    rightPeriod: c.right.params?.period !== undefined ? String(c.right.params.period) : "",
  };
}

function fromDefinition(def: RuleDefinition, name: string): BuilderState {
  const entryRows =
    def.entry.conditions.length > 0
      ? def.entry.conditions.map(ruleConditionToRow)
      : [emptyConditionRow()];

  const exitRows = (def.exit?.conditions ?? []).map(ruleConditionToRow);

  return {
    name,
    universeType: def.universe.type === "volume_top_n" ? "volume_top_n" : "symbols",
    symbolsInput: (def.universe.symbols ?? []).join(","),
    topN: String(def.universe.topN ?? 10),
    additionalSymbols: (def.universe.additionalSymbols ?? []).join(","),
    entryLogic: def.entry.logic,
    entryConditions: entryRows,
    takeProfitPct: def.exit?.takeProfitPct !== undefined ? String(def.exit.takeProfitPct) : "",
    stopLossPct: def.exit?.stopLossPct !== undefined ? String(def.exit.stopLossPct) : "",
    exitLogic: def.exit?.logic ?? "OR",
    exitConditions: exitRows,
    sizingType: def.sizing.type,
    sizingValue: String(def.sizing.value),
    cooldownBars: String(def.constraints?.cooldownBars ?? 1),
    maxPositionsPerSymbol: String(def.constraints?.maxPositionsPerSymbol ?? 1),
  };
}

// ─── Condition row sub-component ─────────────────────────────────────────────

const INPUT_CLS =
  "w-full rounded-md border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500/50";
interface ConditionRowProps {
  row: ConditionRowState;
  index: number;
  canRemove: boolean;
  onChange: (patch: Partial<ConditionRowState>) => void;
  onRemove: () => void;
}

function ConditionRow({ row, index: _index, canRemove, onChange, onRemove }: ConditionRowProps) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      {/* Left indicator */}
      <select
        value={row.leftIndicator}
        onChange={(e) => onChange({ leftIndicator: e.target.value as RuleIndicator | "" })}
        className="rounded-md border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500/50"
      >
        <option value="">지표 선택</option>
        {INDICATORS.map((ind) => (
          <option key={ind} value={ind}>
            {ind}
          </option>
        ))}
      </select>

      {/* Left period (conditional) */}
      {needsPeriod(row.leftIndicator) && (
        <input
          type="number"
          placeholder="기간"
          value={row.leftPeriod}
          onChange={(e) => onChange({ leftPeriod: e.target.value })}
          className="w-20 rounded-md border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500/50"
        />
      )}

      {/* Operator */}
      <select
        value={row.op}
        onChange={(e) => onChange({ op: e.target.value as RuleOperator | "" })}
        className="rounded-md border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500/50"
      >
        <option value="">조건 선택</option>
        {OPERATORS.map((op) => (
          <option key={op} value={op}>
            {op}
          </option>
        ))}
      </select>

      {/* Right type toggle */}
      <select
        value={row.rightType}
        onChange={(e) =>
          onChange({ rightType: e.target.value as "value" | "indicator" })
        }
        className="rounded-md border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500/50"
      >
        <option value="value">값</option>
        <option value="indicator">지표</option>
      </select>

      {/* Right value or right indicator */}
      {row.rightType === "value" ? (
        <input
          type="number"
          placeholder="값"
          value={row.rightValue}
          onChange={(e) => onChange({ rightValue: e.target.value })}
          className="w-24 rounded-md border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500/50"
        />
      ) : (
        <>
          <select
            value={row.rightIndicator}
            onChange={(e) =>
              onChange({ rightIndicator: e.target.value as RuleIndicator | "" })
            }
            className="rounded-md border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500/50"
          >
            <option value="">지표 선택</option>
            {INDICATORS.map((ind) => (
              <option key={ind} value={ind}>
                {ind}
              </option>
            ))}
          </select>
          {needsPeriod(row.rightIndicator) && (
            <input
              type="number"
              placeholder="기간"
              value={row.rightPeriod}
              onChange={(e) => onChange({ rightPeriod: e.target.value })}
              className="w-20 rounded-md border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500/50"
            />
          )}
        </>
      )}

      {/* Remove button */}
      {canRemove && (
        <button
          type="button"
          onClick={onRemove}
          className="ml-1 rounded px-2 py-1 text-sm text-gray-400 hover:text-red-400"
          aria-label="조건 삭제"
        >
          ×
        </button>
      )}
    </div>
  );
}

// ─── Main component ───────────────────────────────────────────────────────────

export function TradingRulesEditPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = id !== undefined;
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [builderState, setBuilderState] = useState<BuilderState>(DEFAULT_STATE);
  const [tab, setTab] = useState<"builder" | "json">("builder");
  const [jsonText, setJsonText] = useState("");
  const [jsonError, setJsonError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  // Edit mode: load existing rule
  const { data: ruleData, isLoading } = useQuery({
    queryKey: ["trading", "paper", "rules", id],
    queryFn: () => fetchPaperRule(Number(id)),
    enabled: isEdit,
  });

  useEffect(() => {
    if (ruleData?.data) {
      setBuilderState(fromDefinition(ruleData.data.definition, ruleData.data.name));
    }
  }, [ruleData]);

  // ── Tab switching ──────────────────────────────────────────────────────────

  function switchToJson() {
    setJsonText(JSON.stringify(toDefinition(builderState), null, 2));
    setJsonError(null);
    setTab("json");
  }

  function switchToBuilder() {
    try {
      const parsed = JSON.parse(jsonText) as RuleDefinition;
      setBuilderState(fromDefinition(parsed, builderState.name));
      setJsonError(null);
      setTab("builder");
    } catch {
      setJsonError("JSON 형식이 올바르지 않습니다.");
    }
  }

  // ── Entry condition helpers ────────────────────────────────────────────────

  const addEntryCondition = () =>
    setBuilderState((s) => ({
      ...s,
      entryConditions: [...s.entryConditions, emptyConditionRow()],
    }));

  const removeEntryCondition = (i: number) =>
    setBuilderState((s) => ({
      ...s,
      entryConditions: s.entryConditions.filter((_, idx) => idx !== i),
    }));

  const updateEntryCondition = (i: number, patch: Partial<ConditionRowState>) =>
    setBuilderState((s) => ({
      ...s,
      entryConditions: s.entryConditions.map((c, idx) =>
        idx === i ? { ...c, ...patch } : c
      ),
    }));

  // ── Exit condition helpers ─────────────────────────────────────────────────

  const addExitCondition = () =>
    setBuilderState((s) => ({
      ...s,
      exitConditions: [...s.exitConditions, emptyConditionRow()],
    }));

  const removeExitCondition = (i: number) =>
    setBuilderState((s) => ({
      ...s,
      exitConditions: s.exitConditions.filter((_, idx) => idx !== i),
    }));

  const updateExitCondition = (i: number, patch: Partial<ConditionRowState>) =>
    setBuilderState((s) => ({
      ...s,
      exitConditions: s.exitConditions.map((c, idx) =>
        idx === i ? { ...c, ...patch } : c
      ),
    }));

  // ── Save ──────────────────────────────────────────────────────────────────

  const saveMutation = useMutation({
    mutationFn: (payload: RuleUpsertRequest) =>
      isEdit ? updatePaperRule(Number(id), payload) : createPaperRule(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["trading", "paper", "rules"] });
      navigate("/trading/paper/rules");
    },
    onError: (err) => {
      setFormError(
        err instanceof ApiRequestError ? err.message : "저장에 실패했습니다."
      );
    },
  });

  function handleSave() {
    if (!builderState.name.trim()) {
      setFormError("룰 이름을 입력하세요.");
      return;
    }
    const validEntry = builderState.entryConditions.filter(
      (c) => c.leftIndicator && c.op
    );
    if (validEntry.length === 0) {
      setFormError("진입 조건을 최소 1개 입력하세요.");
      return;
    }
    const sizingVal = parseFloat(builderState.sizingValue);
    if (isNaN(sizingVal)) {
      setFormError("사이징 값을 올바르게 입력하세요.");
      return;
    }
    setFormError(null);
    const def = toDefinition(builderState);
    saveMutation.mutate({ name: builderState.name, definition: def });
  }

  // ── Loading state ──────────────────────────────────────────────────────────

  if (isEdit && isLoading) {
    return (
      <div className="mx-auto max-w-3xl p-4">
        <p className="text-sm text-gray-400">불러오는 중...</p>
      </div>
    );
  }

  // ── Render ─────────────────────────────────────────────────────────────────

  return (
    <div className="mx-auto max-w-3xl space-y-6 p-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-white">
          {isEdit ? "룰 편집" : "새 룰"}
        </h1>
        <button
          type="button"
          onClick={() => navigate("/trading/paper/rules")}
          className="rounded-md px-3 py-1.5 text-sm text-gray-400 hover:text-white"
        >
          ← 돌아가기
        </button>
      </div>

      {/* Tab bar */}
      <div className="flex gap-2 border-b border-white/10">
        {(["builder", "json"] as const).map((t) => (
          <button
            key={t}
            type="button"
            onClick={() => (t === "json" ? switchToJson() : switchToBuilder())}
            className={`px-4 py-2 text-sm font-medium transition-colors ${
              tab === t
                ? "border-b-2 border-emerald-500 text-white"
                : "text-gray-400 hover:text-white"
            }`}
          >
            {t === "builder" ? "빌더" : "JSON"}
          </button>
        ))}
      </div>

      {/* ── Builder tab ── */}
      {tab === "builder" && (
        <div className="space-y-4">
          {/* 1. 룰 이름 */}
          <section className="rounded-lg border border-white/10 bg-gray-900/50 p-5">
            <h2 className="mb-3 text-sm font-medium text-gray-300">룰 이름</h2>
            <input
              type="text"
              placeholder="예: KOSPI Top10 모멘텀 전략"
              value={builderState.name}
              onChange={(e) =>
                setBuilderState((s) => ({ ...s, name: e.target.value }))
              }
              className={INPUT_CLS}
            />
          </section>

          {/* 2. 유니버스 */}
          <section className="rounded-lg border border-white/10 bg-gray-900/50 p-5">
            <h2 className="mb-3 text-sm font-medium text-gray-300">유니버스</h2>
            <div className="mb-3 flex gap-4">
              {(["symbols", "volume_top_n"] as const).map((ut) => (
                <label key={ut} className="flex cursor-pointer items-center gap-2 text-sm text-gray-300">
                  <input
                    type="radio"
                    name="universeType"
                    value={ut}
                    checked={builderState.universeType === ut}
                    onChange={() =>
                      setBuilderState((s) => ({ ...s, universeType: ut }))
                    }
                    className="accent-emerald-500"
                  />
                  {ut === "symbols" ? "종목 코드 직접 입력" : "거래량 상위 N종목"}
                </label>
              ))}
            </div>

            {builderState.universeType === "symbols" ? (
              <div>
                <label className="mb-1 block text-xs text-gray-400">
                  종목 코드 (쉼표 구분)
                </label>
                <input
                  type="text"
                  placeholder="005930,000660,035720"
                  value={builderState.symbolsInput}
                  onChange={(e) =>
                    setBuilderState((s) => ({ ...s, symbolsInput: e.target.value }))
                  }
                  className={INPUT_CLS}
                />
              </div>
            ) : (
              <div className="space-y-3">
                <div>
                  <label className="mb-1 block text-xs text-gray-400">
                    상위 N종목 (KOSPI)
                  </label>
                  <input
                    type="number"
                    min={1}
                    max={100}
                    value={builderState.topN}
                    onChange={(e) =>
                      setBuilderState((s) => ({ ...s, topN: e.target.value }))
                    }
                    className={INPUT_CLS}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs text-gray-400">
                    추가 종목 코드 (선택, 쉼표 구분)
                  </label>
                  <input
                    type="text"
                    placeholder="005930,000660"
                    value={builderState.additionalSymbols}
                    onChange={(e) =>
                      setBuilderState((s) => ({
                        ...s,
                        additionalSymbols: e.target.value,
                      }))
                    }
                    className={INPUT_CLS}
                  />
                </div>
              </div>
            )}
          </section>

          {/* 3. 진입 조건 */}
          <section className="rounded-lg border border-white/10 bg-gray-900/50 p-5">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-sm font-medium text-gray-300">진입 조건</h2>
              <div className="flex items-center gap-2">
                <span className="text-xs text-gray-400">논리:</span>
                <select
                  value={builderState.entryLogic}
                  onChange={(e) =>
                    setBuilderState((s) => ({
                      ...s,
                      entryLogic: e.target.value as RuleLogic,
                    }))
                  }
                  className="rounded-md border border-white/10 bg-gray-800 px-2 py-1 text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500/50"
                >
                  <option value="AND">AND</option>
                  <option value="OR">OR</option>
                </select>
              </div>
            </div>

            <div className="space-y-2">
              {builderState.entryConditions.map((row, i) => (
                <ConditionRow
                  key={i}
                  row={row}
                  index={i}
                  canRemove={builderState.entryConditions.length > 1}
                  onChange={(patch) => updateEntryCondition(i, patch)}
                  onRemove={() => removeEntryCondition(i)}
                />
              ))}
            </div>

            <button
              type="button"
              onClick={addEntryCondition}
              className="mt-3 text-sm text-emerald-400 hover:text-emerald-300"
            >
              + 조건 추가
            </button>
          </section>

          {/* 4. 청산 조건 */}
          <section className="rounded-lg border border-white/10 bg-gray-900/50 p-5">
            <h2 className="mb-3 text-sm font-medium text-gray-300">청산 조건</h2>

            {/* 익절 / 손절 */}
            <div className="mb-4 flex gap-4">
              <div className="flex-1">
                <label className="mb-1 block text-xs text-gray-400">익절 %</label>
                <input
                  type="number"
                  placeholder="예: 5"
                  value={builderState.takeProfitPct}
                  onChange={(e) =>
                    setBuilderState((s) => ({
                      ...s,
                      takeProfitPct: e.target.value,
                    }))
                  }
                  className={INPUT_CLS}
                />
              </div>
              <div className="flex-1">
                <label className="mb-1 block text-xs text-gray-400">손절 %</label>
                <input
                  type="number"
                  placeholder="예: 3"
                  value={builderState.stopLossPct}
                  onChange={(e) =>
                    setBuilderState((s) => ({
                      ...s,
                      stopLossPct: e.target.value,
                    }))
                  }
                  className={INPUT_CLS}
                />
              </div>
            </div>

            {/* Exit indicator conditions */}
            {builderState.exitConditions.length > 0 && (
              <div className="mb-2 flex items-center gap-2">
                <span className="text-xs text-gray-400">논리:</span>
                <select
                  value={builderState.exitLogic}
                  onChange={(e) =>
                    setBuilderState((s) => ({
                      ...s,
                      exitLogic: e.target.value as RuleLogic,
                    }))
                  }
                  className="rounded-md border border-white/10 bg-gray-800 px-2 py-1 text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500/50"
                >
                  <option value="AND">AND</option>
                  <option value="OR">OR</option>
                </select>
              </div>
            )}

            <div className="space-y-2">
              {builderState.exitConditions.map((row, i) => (
                <ConditionRow
                  key={i}
                  row={row}
                  index={i}
                  canRemove
                  onChange={(patch) => updateExitCondition(i, patch)}
                  onRemove={() => removeExitCondition(i)}
                />
              ))}
            </div>

            <button
              type="button"
              onClick={addExitCondition}
              className="mt-3 text-sm text-emerald-400 hover:text-emerald-300"
            >
              + 청산 지표 조건 추가
            </button>
          </section>

          {/* 5. 사이징 */}
          <section className="rounded-lg border border-white/10 bg-gray-900/50 p-5">
            <h2 className="mb-3 text-sm font-medium text-gray-300">사이징</h2>
            <div className="flex gap-3">
              <select
                value={builderState.sizingType}
                onChange={(e) =>
                  setBuilderState((s) => ({
                    ...s,
                    sizingType: e.target.value as SizingType,
                  }))
                }
                className="rounded-md border border-white/10 bg-gray-800 px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500/50"
              >
                <option value="cash">현금 (원)</option>
                <option value="percent">비중 (%)</option>
                <option value="qty">수량 (주)</option>
              </select>
              <input
                type="number"
                placeholder="값"
                value={builderState.sizingValue}
                onChange={(e) =>
                  setBuilderState((s) => ({ ...s, sizingValue: e.target.value }))
                }
                className={INPUT_CLS}
              />
            </div>
          </section>

          {/* 6. 제약 */}
          <section className="rounded-lg border border-white/10 bg-gray-900/50 p-5">
            <h2 className="mb-3 text-sm font-medium text-gray-300">제약</h2>
            <div className="flex gap-4">
              <div className="flex-1">
                <label className="mb-1 block text-xs text-gray-400">
                  쿨다운 (봉)
                </label>
                <input
                  type="number"
                  min={0}
                  value={builderState.cooldownBars}
                  onChange={(e) =>
                    setBuilderState((s) => ({ ...s, cooldownBars: e.target.value }))
                  }
                  className={INPUT_CLS}
                />
              </div>
              <div className="flex-1">
                <label className="mb-1 block text-xs text-gray-400">
                  최대 포지션 (종목당)
                </label>
                <input
                  type="number"
                  min={1}
                  value={builderState.maxPositionsPerSymbol}
                  onChange={(e) =>
                    setBuilderState((s) => ({
                      ...s,
                      maxPositionsPerSymbol: e.target.value,
                    }))
                  }
                  className={INPUT_CLS}
                />
              </div>
            </div>
          </section>
        </div>
      )}

      {/* ── JSON tab ── */}
      {tab === "json" && (
        <div className="space-y-2">
          <textarea
            rows={20}
            value={jsonText}
            onChange={(e) => setJsonText(e.target.value)}
            spellCheck={false}
            className="w-full rounded-md border border-white/10 bg-gray-900 px-3 py-2 font-mono text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500/50"
          />
          {jsonError && (
            <p className="text-xs text-red-400">{jsonError}</p>
          )}
        </div>
      )}

      {/* Footer */}
      <div className="flex items-center justify-between border-t border-white/10 pt-4">
        <div>
          {formError && (
            <p className="text-xs text-red-400">{formError}</p>
          )}
        </div>
        <div className="flex gap-3">
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="rounded-md border border-white/10 px-4 py-2 text-sm text-gray-300 hover:bg-white/5"
          >
            취소
          </button>
          <button
            type="button"
            onClick={handleSave}
            disabled={saveMutation.isPending}
            className="rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-500 disabled:opacity-60"
          >
            {saveMutation.isPending ? "저장 중..." : "저장"}
          </button>
        </div>
      </div>
    </div>
  );
}

