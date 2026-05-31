import { useCallback, useSyncExternalStore } from "react";
import type { RecentSearchEntry } from "@/types/search";

const STORAGE_KEY = "graphify:recent-searches";
const MAX_ITEMS = 5;
const EMPTY_ENTRIES: RecentSearchEntry[] = [];

let cachedRaw: string | null = null;
let cachedSnapshot: RecentSearchEntry[] = EMPTY_ENTRIES;

function parseRaw(raw: string | null): RecentSearchEntry[] {
  if (!raw) return EMPTY_ENTRIES;
  try {
    const parsed = JSON.parse(raw) as RecentSearchEntry[];
    return Array.isArray(parsed) ? parsed : EMPTY_ENTRIES;
  } catch {
    return EMPTY_ENTRIES;
  }
}

function readStorage(): RecentSearchEntry[] {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (raw === cachedRaw) {
    return cachedSnapshot;
  }
  cachedRaw = raw;
  cachedSnapshot = parseRaw(raw);
  return cachedSnapshot;
}

function writeStorage(entries: RecentSearchEntry[]) {
  const next = entries.slice(0, MAX_ITEMS);
  const raw = JSON.stringify(next);
  localStorage.setItem(STORAGE_KEY, raw);
  cachedRaw = raw;
  cachedSnapshot = next;
  window.dispatchEvent(new Event("graphify-recent-searches"));
}

function subscribe(listener: () => void) {
  const handler = () => {
    cachedRaw = null;
    listener();
  };
  window.addEventListener("graphify-recent-searches", handler);
  window.addEventListener("storage", handler);
  return () => {
    window.removeEventListener("graphify-recent-searches", handler);
    window.removeEventListener("storage", handler);
  };
}

function getSnapshot() {
  return readStorage();
}

export function useRecentSearches() {
  const entries = useSyncExternalStore(subscribe, getSnapshot, () => EMPTY_ENTRIES);

  const addEntry = useCallback((companyId: number, label: string) => {
    const next: RecentSearchEntry[] = [
      { companyId, label, searchedAt: new Date().toISOString() },
      ...readStorage().filter((e) => e.companyId !== companyId),
    ].slice(0, MAX_ITEMS);
    writeStorage(next);
  }, []);

  const removeEntry = useCallback((companyId: number) => {
    writeStorage(readStorage().filter((e) => e.companyId !== companyId));
  }, []);

  const clearAll = useCallback(() => {
    writeStorage([]);
  }, []);

  return { entries, addEntry, removeEntry, clearAll };
}
