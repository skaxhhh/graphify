import { Avatar } from "@/components/shared/Avatar";
import type { UserMe } from "@/types/user";

const PROVIDER_LABEL: Record<string, string> = {
  email: "이메일",
  google: "Google",
  naver: "네이버",
  kakao: "카카오",
};

interface ProfileSummaryCardProps {
  user: UserMe;
}

export function ProfileSummaryCard({ user }: ProfileSummaryCardProps) {
  return (
    <section className="flex items-center gap-4 rounded-xl border border-warm-border bg-cream p-6">
      <Avatar displayName={user.displayName} />
      <div className="min-w-0 flex-1">
        <h1 className="text-xl font-semibold text-charcoal">{user.displayName}</h1>
        <p className="mt-1 text-sm text-muted-gray">{user.email}</p>
        <p className="mt-2 text-xs text-muted-gray">
          {PROVIDER_LABEL[user.authProvider] ?? user.authProvider} 로그인
          {user.isPremium ? " · Premium" : ""}
        </p>
      </div>
    </section>
  );
}
