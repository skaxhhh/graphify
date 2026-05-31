package com.graphify.company.market;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class MarketQuoteResolver {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 0);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(15, 30);

    private MarketQuoteResolver() {
    }

    public static PriceDisplayKind resolvePriceKind(Instant quoteTime, ZoneId exchangeZone) {
        ZoneId zone = exchangeZone != null ? exchangeZone : KST;
        ZonedDateTime quote = quoteTime.atZone(zone);
        LocalDate quoteDate = quote.toLocalDate();
        LocalDate today = LocalDate.now(zone);

        if (!quoteDate.equals(today)) {
            return PriceDisplayKind.LAST_CLOSE;
        }
        if (!isWeekday(quoteDate) || quote.toLocalTime().isBefore(MARKET_OPEN)) {
            return PriceDisplayKind.LAST_CLOSE;
        }
        if (!quote.toLocalTime().isBefore(MARKET_CLOSE)) {
            return PriceDisplayKind.TODAY_CLOSE;
        }
        return PriceDisplayKind.LIVE;
    }

    public static String priceLabelKo(PriceDisplayKind kind, LocalDate tradingDate) {
        return switch (kind) {
            case LIVE -> "현재가";
            case TODAY_CLOSE -> "금일 종가";
            case LAST_CLOSE -> "최근 종가 (" + tradingDate.getMonthValue() + "/"
                    + tradingDate.getDayOfMonth() + ")";
        };
    }

    private static boolean isWeekday(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }
}
