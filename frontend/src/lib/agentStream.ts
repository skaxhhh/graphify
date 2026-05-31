import type { AgentStreamEvent } from "@/types/graph";

const baseUrl = import.meta.env.VITE_API_BASE_URL ?? "";

export function subscribeAgentStream(
  sessionId: string,
  handlers: {
    onEvent: (event: AgentStreamEvent) => void;
    onError?: () => void;
    onComplete?: () => void;
  }
): () => void {
  const source = new EventSource(`${baseUrl}/api/v1/agent/stream/${sessionId}`);

  source.addEventListener("progress", (message) => {
    try {
      const payload = JSON.parse(message.data) as AgentStreamEvent;
      handlers.onEvent(payload);
      if (payload.stage === "DONE") {
        source.close();
        handlers.onComplete?.();
      }
    } catch {
      handlers.onError?.();
    }
  });

  source.onerror = () => {
    handlers.onError?.();
    source.close();
  };

  return () => source.close();
}
