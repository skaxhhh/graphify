import { useEffect } from "react";
import { useAuthStore } from "@/stores/authStore";

export function useAuthHydration() {
  const hydrated = useAuthStore((s) => s.hydrated);
  const hydrate = useAuthStore((s) => s.hydrate);

  useEffect(() => {
    if (!hydrated) {
      hydrate();
    }
  }, [hydrate, hydrated]);

  return hydrated;
}
