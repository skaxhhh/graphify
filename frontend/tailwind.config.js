/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        cream: "#f7f4ed",
        "cream-surface": "#f7f4ed",
        charcoal: "#1c1c1c",
        "off-white": "#fcfbf8",
        "muted-gray": "#5f5f5d",
        "light-cream": "#eceae4",
        "warm-border": "#b8aea0",
        "ring-blue": "rgba(59, 130, 246, 0.5)",

        // ── Trading 콘솔 다크 테마 (Binance design system, DESIGN-binance.md) ──
        // `trade-*` 네임스페이스로 격리 — 기존 cream 앱 테마와 충돌 없음.
        // 적용 범위: /trading/** 화면 전용. 하드코딩 hex 금지, 이 토큰만 사용.
        trade: {
          // Brand / accent — 단일 옐로우가 모든 primary CTA·브랜드 voltage 담당
          primary: "#fcd535",
          "primary-active": "#f0b90b",
          "primary-disabled": "#3a3a1f",
          // Canvas / surface (다크) — 색상 블록 대비로 elevation 표현 (그림자 X)
          bg: "#0b0e11", // canvas-dark · 페이지 바닥
          surface: "#1e2329", // surface-card-dark · 카드·테이블·secondary 버튼
          elevated: "#2b3139", // surface-elevated-dark · 중첩 카드·hover·차트 패널
          hairline: "#2b3139", // 다크 1px 보더 (surface step과 동일 hex)
          // Text
          ink: "#181a20", // on-primary (옐로우 위 검정 텍스트) · on-light
          body: "#eaecef", // 다크 캔버스 기본 본문 (순백 X)
          muted: "#707a8a", // 캡션·테이블 헤더·breadcrumb
          "muted-strong": "#929aa5", // 강조 라벨 2티어
          "on-dark": "#ffffff", // 고대비 헤드라인
          // Trading 시그널 — 가격 방향 (텍스트 색 용도, 카드 배경 금지)
          up: "#0ecb81", // 매수·수익·상승
          down: "#f6465d", // 매도·손실·하락
          "up-soft": "#0e1f1a", // 매수 배지 배경 (희미한 그린 틴트)
          "down-soft": "#21141a", // 매도/경고 배지 배경 (희미한 레드 틴트)
          // Info / focus
          info: "#3b82f6",
        },
      },
      fontFamily: {
        sans: [
          "ui-sans-serif",
          "system-ui",
          "-apple-system",
          "Segoe UI",
          "Roboto",
          "sans-serif",
        ],
        // Trading 콘솔 — BinanceNova 대체: Inter(본문/UI), BinancePlex 대체: JetBrains Mono(숫자/가격)
        "trade-sans": [
          "Inter",
          "-apple-system",
          "BlinkMacSystemFont",
          "Segoe UI",
          "Roboto",
          "sans-serif",
        ],
        "trade-mono": [
          "JetBrains Mono",
          "ui-monospace",
          "SFMono-Regular",
          "Menlo",
          "monospace",
        ],
      },
      boxShadow: {
        focus: "rgba(0, 0, 0, 0.1) 0px 4px 12px",
        "btn-inset":
          "rgba(255,255,255,0.2) 0px 0.5px 0px 0px inset, rgba(0,0,0,0.2) 0px 0px 0px 0.5px inset, rgba(0,0,0,0.05) 0px 1px 2px 0px",
      },
    },
  },
  plugins: [],
};
