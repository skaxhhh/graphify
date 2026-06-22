-- Phase 6.7: ETF/ETN/우선주 제외를 위한 종목 구분 컬럼 추가 (DATA-06)
-- 값 도메인: COMMON_STOCK / ETF / ETN / PREFERRED / SPAC / ETC
-- 기존 companies 행은 COMMON_STOCK 기본값으로 분류 (RESEARCH Open Question 3 — 전체 KOSPI 800 시드는 범위 외)
ALTER TABLE companies ADD COLUMN IF NOT EXISTS instrument_type VARCHAR(20) NOT NULL DEFAULT 'COMMON_STOCK';
