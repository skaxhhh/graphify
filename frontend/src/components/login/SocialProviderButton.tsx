type OAuthProvider = "google" | "naver" | "kakao";

interface SocialProviderButtonProps {
  provider: OAuthProvider;
  label: string;
  onClick: () => void;
  disabled?: boolean;
}

const providerStyles: Record<OAuthProvider, string> = {
  google: "border-charcoal/40 bg-cream text-charcoal",
  naver: "border-[#03C75A] bg-[#03C75A] text-white",
  kakao: "border-[#FEE500] bg-[#FEE500] text-[#191919]",
};

export function SocialProviderButton({
  provider,
  label,
  onClick,
  disabled = false,
}: SocialProviderButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={`flex h-11 w-full items-center justify-center gap-2 rounded-md border px-4 text-sm font-medium transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60 ${providerStyles[provider]}`}
    >
      <span aria-hidden className="text-base">
        {provider === "google" ? "G" : provider === "naver" ? "N" : "K"}
      </span>
      {label}
    </button>
  );
}
