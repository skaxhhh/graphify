import { useCallback, useEffect, useRef, useState } from "react";

/**
 * 장시간 API 대기 중에도 진행률이 서서히 오르는 UI용 훅.
 * cap을 단계마다 올리면 해당 구간까지 천천히 증가합니다.
 */
export function useSmoothProgress() {
  const [percent, setPercent] = useState(0);
  const [label, setLabel] = useState("");
  const capRef = useRef(0);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const stopInterval = useCallback(() => {
    if (intervalRef.current != null) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  }, []);

  const ensureInterval = useCallback(() => {
    if (intervalRef.current != null) return;
    intervalRef.current = setInterval(() => {
      setPercent((prev) => {
        const cap = capRef.current;
        if (prev >= cap) return prev;
        const step = prev < 25 ? 2 : prev < 55 ? 1.5 : prev < 80 ? 1 : 0.5;
        return Math.min(cap, Math.round(prev + step));
      });
    }, 180);
  }, []);

  const reset = useCallback(() => {
    stopInterval();
    capRef.current = 0;
    setPercent(0);
    setLabel("");
  }, [stopInterval]);

  const begin = useCallback(
    (phaseLabel: string, cap: number) => {
      setLabel(phaseLabel);
      capRef.current = Math.max(capRef.current, cap);
      setPercent((prev) => (prev === 0 ? 3 : prev));
      ensureInterval();
    },
    [ensureInterval]
  );

  const setCap = useCallback(
    (cap: number, phaseLabel?: string) => {
      capRef.current = Math.max(capRef.current, cap);
      if (phaseLabel) setLabel(phaseLabel);
      ensureInterval();
    },
    [ensureInterval]
  );

  const finish = useCallback(() => {
    stopInterval();
    capRef.current = 100;
    setPercent(100);
    setLabel("완료");
  }, [stopInterval]);

  useEffect(() => () => stopInterval(), [stopInterval]);

  return { percent, label, reset, begin, setCap, finish };
}
