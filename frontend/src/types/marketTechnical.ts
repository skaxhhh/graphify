export type MaAlignment = "BULLISH_ALIGN" | "BEARISH_ALIGN" | "MIXED";

export type PriceDisplayKind = "LIVE" | "TODAY_CLOSE" | "LAST_CLOSE";

export interface CompanyMarketTechnical {
  yahooSymbol: string;
  currency: string | null;
  price: number | null;
  changePercent: number | null;
  previousClose: number | null;
  ma5: number | null;
  ma20: number | null;
  ma60: number | null;
  ma120: number | null;
  ma240: number | null;
  rsi14: number | null;
  maAlignment: MaAlignment;
  shortTermRise5: boolean;
  shortTermRise20: boolean;
  /** Yahoo 시세 기준 시각 (ISO) */
  quoteTime: string;
  /** 거래일 (yyyy-MM-dd, 거래소 시간대) */
  tradingDate: string;
  priceKind: PriceDisplayKind;
  /** 현재가 / 금일 종가 / 최근 종가 (MM/DD) */
  priceLabel: string;
  /** API 조회 시각 */
  asOf: string;
  /** 장중 시세 출처: NAVER | YAHOO */
  priceSource?: string | null;
  /** 일봉·지표 출처: YAHOO | KRX */
  historySource?: string | null;
}
