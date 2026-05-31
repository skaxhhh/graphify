import { useState } from "react";
import { GhostButton } from "@/components/shared/GhostButton";

interface ExportMenuProps {
  isPremium?: boolean;
}

export function ExportMenu({ isPremium = false }: ExportMenuProps) {
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const handleExport = async (format: "csv" | "pdf") => {
    if (!isPremium) {
      setMessage("Premium 플랜에서 이용할 수 있습니다.");
      return;
    }
    setLoading(true);
    setMessage(null);
    await new Promise((resolve) => window.setTimeout(resolve, 800));
    setLoading(false);
    setMessage(`${format.toUpperCase()}보내기 요청이 접수되었습니다.`);
  };

  return (
    <div className="flex flex-col items-end gap-1">
      <div className="flex items-center gap-2">
        <span className="rounded border border-charcoal px-2 py-0.5 text-[10px] text-muted-gray">
          Premium
        </span>
        <GhostButton
          className="!h-9 !py-0 text-xs"
          disabled={loading}
          onClick={() => handleExport("csv")}
        >
          {loading ? "처리 중…" : "CSV"}
        </GhostButton>
        <GhostButton
          className="!h-9 !py-0 text-xs"
          disabled={loading}
          onClick={() => handleExport("pdf")}
        >
          PDF
        </GhostButton>
      </div>
      {message ? <p className="text-xs text-muted-gray">{message}</p> : null}
    </div>
  );
}
