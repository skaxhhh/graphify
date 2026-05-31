const FROM_DETAIL_KEY = "graphify.graphFromDetail";
const HIGHLIGHT_KEY = "graphify.graphHighlightNodes";

export function setGraphNavigationFromDetail(companyId: number, highlightNodeIds?: string[]) {
  sessionStorage.setItem(FROM_DETAIL_KEY, String(companyId));
  if (highlightNodeIds && highlightNodeIds.length > 0) {
    sessionStorage.setItem(HIGHLIGHT_KEY, JSON.stringify(highlightNodeIds));
  } else {
    sessionStorage.removeItem(HIGHLIGHT_KEY);
  }
}

export function readGraphHighlightNodes(): string[] {
  const raw = sessionStorage.getItem(HIGHLIGHT_KEY);
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw) as unknown;
    return Array.isArray(parsed) ? parsed.filter((id): id is string => typeof id === "string") : [];
  } catch {
    return [];
  }
}

export function clearGraphHighlightNodes(): void {
  sessionStorage.removeItem(HIGHLIGHT_KEY);
}
