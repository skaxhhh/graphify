import { useMemo, useState } from "react";
import type { TimelineEvent } from "@/types/history";

interface TimelineSliderProps {
  events: TimelineEvent[];
  selectedIndex: number;
  onSelectIndex: (index: number) => void;
}

export function TimelineSlider({ events, selectedIndex, onSelectIndex }: TimelineSliderProps) {
  const [selectedMarker, setSelectedMarker] = useState<number | null>(null);

  const maxIndex = Math.max(events.length - 1, 0);
  const safeIndex = Math.min(Math.max(selectedIndex, 0), maxIndex);
  const selectedEvent = events[safeIndex];

  const markerPositions = useMemo(() => {
    if (events.length <= 1) {
      return events.map((_, i) => (i === 0 ? 50 : 100));
    }
    return events.map((_, i) => (i / (events.length - 1)) * 100);
  }, [events]);

  return (
    <div className="w-full py-4">
      <div className="relative h-12 w-full">
        <div className="absolute inset-x-0 top-1/2 h-3 -translate-y-1/2 rounded-full bg-light-cream" />
        <input
          type="range"
          min={0}
          max={maxIndex}
          value={safeIndex}
          onChange={(e) => {
            setSelectedMarker(null);
            onSelectIndex(Number(e.target.value));
          }}
          className="absolute inset-0 z-10 h-12 w-full cursor-pointer opacity-0"
          aria-label="분석 시점 타임라인"
        />
        <div
          className="pointer-events-none absolute top-1/2 z-20 h-6 w-6 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-cream bg-charcoal shadow-[0_0_0_3px_rgba(45,45,45,0.15)] transition-[left] duration-150"
          style={{ left: `${markerPositions[safeIndex] ?? 0}%` }}
        />
        {events.map((event, index) => (
          <button
            key={`${event.eventType}-${event.t}`}
            type="button"
            className={`absolute top-1/2 z-30 h-3 w-3 -translate-x-1/2 -translate-y-1/2 rounded-full border border-cream transition-all duration-200 ${
              selectedMarker === index || safeIndex === index
                ? "scale-125 bg-charcoal shadow-[0_0_0_4px_rgba(45,45,45,0.12)]"
                : "bg-charcoal/70 hover:scale-110"
            }`}
            style={{ left: `${markerPositions[index]}%` }}
            onClick={() => {
              setSelectedMarker(index);
              onSelectIndex(index);
            }}
            aria-label={event.label}
          />
        ))}
      </div>
      {selectedEvent ? (
        <p className="mt-3 text-center text-xs text-muted-gray md:text-sm">
          {selectedEvent.label} · {new Date(selectedEvent.t).toLocaleString("ko-KR")}
        </p>
      ) : null}
      {selectedMarker != null && events[selectedMarker] ? (
        <EventDetailInline event={events[selectedMarker]} />
      ) : null}
    </div>
  );
}

function EventDetailInline({ event }: { event: TimelineEvent }) {
  return (
    <div
      className="mt-3 overflow-hidden rounded-xl border border-warm-border bg-cream px-4 py-3 text-sm text-charcoal transition-all duration-200 ease-out"
      role="status"
    >
      <p className="font-medium">{event.label}</p>
      <p className="mt-1 text-xs text-muted-gray">{event.eventType}</p>
    </div>
  );
}
