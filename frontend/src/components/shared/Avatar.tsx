interface AvatarProps {
  displayName: string;
  className?: string;
}

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length >= 2) {
    const first = parts[0]?.[0] ?? "";
    const second = parts[1]?.[0] ?? "";
    return (first + second).toUpperCase() || "?";
  }
  return (name.slice(0, 2) || "?").toUpperCase();
}

export function Avatar({ displayName, className = "" }: AvatarProps) {
  return (
    <div
      className={`flex h-14 w-14 shrink-0 items-center justify-center rounded-full border border-warm-border bg-charcoal/[0.06] text-sm font-semibold text-charcoal ${className}`}
      aria-hidden
    >
      {initials(displayName)}
    </div>
  );
}
