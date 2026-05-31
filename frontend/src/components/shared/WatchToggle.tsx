import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiRequestError } from "@/lib/apiClient";
import { addToWatchlist, fetchMyWatchlist, removeFromWatchlist } from "@/lib/watchlistApi";

interface WatchToggleProps {
  companyId: number;
  isAuthenticated: boolean;
  onGuestAttempt: () => void;
}

export function WatchToggle({ companyId, isAuthenticated, onGuestAttempt }: WatchToggleProps) {
  const queryClient = useQueryClient();
  const [active, setActive] = useState(false);

  const watchlistQuery = useQuery({
    queryKey: ["watchlist", "me"],
    queryFn: async () => {
      const response = await fetchMyWatchlist();
      return response.data;
    },
    enabled: isAuthenticated,
  });

  useEffect(() => {
    if (!isAuthenticated) {
      setActive(false);
      return;
    }
    const items = watchlistQuery.data?.items ?? [];
    setActive(items.some((item) => item.companyId === companyId));
  }, [companyId, isAuthenticated, watchlistQuery.data]);

  const addMutation = useMutation({
    mutationFn: () => addToWatchlist(companyId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["watchlist", "me"] });
      setActive(true);
    },
  });

  const removeMutation = useMutation({
    mutationFn: () => removeFromWatchlist(companyId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["watchlist", "me"] });
      setActive(false);
    },
  });

  const pending = addMutation.isPending || removeMutation.isPending;

  const handleClick = () => {
    if (!isAuthenticated) {
      onGuestAttempt();
      return;
    }
    if (pending) return;
    if (active) {
      removeMutation.mutate();
    } else {
      addMutation.mutate();
    }
  };

  const error = addMutation.error ?? removeMutation.error;
  const errorMessage = error instanceof ApiRequestError ? error.message : null;

  return (
    <div className="flex flex-col items-start gap-1">
      <button
        type="button"
        onClick={handleClick}
        disabled={pending || (isAuthenticated && watchlistQuery.isLoading)}
        aria-pressed={active}
        className="inline-flex h-11 items-center justify-center rounded-md border border-warm-border px-4 text-sm text-charcoal transition-colors hover:bg-charcoal/[0.03] disabled:opacity-60"
      >
        {pending ? "처리 중…" : active ? "관심 해제" : "관심 등록"}
      </button>
      {errorMessage ? (
        <span className="text-xs text-muted-gray">{errorMessage}</span>
      ) : null}
    </div>
  );
}
