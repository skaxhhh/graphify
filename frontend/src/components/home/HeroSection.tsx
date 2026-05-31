export function HeroSection() {
  return (
    <section className="w-full max-w-[1200px] px-4 py-16 text-center md:py-24 lg:py-32">
      <h1 className="text-4xl font-semibold tracking-tight text-charcoal md:text-5xl lg:text-6xl lg:tracking-[-0.02em]">
        기업 관계를 한눈에,
        <br className="sm:hidden" /> graphify
      </h1>
      <p className="mx-auto mt-6 max-w-2xl text-lg leading-relaxed text-muted-gray md:text-xl">
        AI Agent와 MCP 도구로 기업 관계를 수집·분석하고, 투자 인사이트를
        직관적인 그래프로 확인하세요.
      </p>
    </section>
  );
}
