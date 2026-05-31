import { BuzzCompaniesPanel } from "@/components/home/BuzzCompaniesPanel";
import { MarketNewsPanel } from "@/components/home/MarketNewsPanel";
import { SentimentIndexPanel } from "@/components/home/SentimentIndexPanel";
import { TrendingCompaniesPanel } from "@/components/home/TrendingCompaniesPanel";

export function HomeInsightsSection() {
  return (
    <section
      className="mt-10 w-full px-3 pb-16 sm:px-4 md:pb-24 lg:px-6"
      aria-label="홈 인사이트"
    >
      <div className="grid grid-cols-1 gap-4 min-[900px]:grid-cols-2 min-[1400px]:grid-cols-4 min-[1400px]:items-start">
        <TrendingCompaniesPanel />
        <BuzzCompaniesPanel />
        <MarketNewsPanel />
        <SentimentIndexPanel />
      </div>
    </section>
  );
}
