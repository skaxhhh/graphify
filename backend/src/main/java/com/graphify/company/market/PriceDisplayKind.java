package com.graphify.company.market;

public enum PriceDisplayKind {
    /** 장중 최신 호가/체결가 */
    LIVE,
    /** 당일 장 마감 종가 */
    TODAY_CLOSE,
    /** 당일이 아닌 가장 최근 거래일 종가 */
    LAST_CLOSE
}
