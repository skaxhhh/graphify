import { GhostButton } from "@/components/shared/GhostButton";

interface AccountActionsProps {
  onLogoutClick: () => void;
}

export function AccountActions({ onLogoutClick }: AccountActionsProps) {
  return (
    <section className="rounded-xl border border-warm-border bg-cream p-6">
      <h2 className="text-base font-semibold text-charcoal">계정</h2>
      <div className="mt-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <GhostButton type="button" onClick={onLogoutClick} className="w-full sm:w-auto">
          로그아웃
        </GhostButton>
        <button
          type="button"
          className="text-sm text-muted-gray underline hover:text-charcoal"
          disabled
          title="준비 중"
        >
          회원 탈퇴
        </button>
      </div>
    </section>
  );
}
