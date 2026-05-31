package com.graphify.company.market;

import com.graphify.company.dto.CompanyMarketTechnicalDto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 장중 시세(네이버) + 일봉 히스토리(Yahoo, 추후 KRX 캐시) 병합.
 */
public final class CompositeMarketTechnicalBuilder {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private CompositeMarketTechnicalBuilder() {
    }

    public static CompanyMarketTechnicalDto build(
            String yahooSymbol,
            YahooChartData yahooChart,
            NaverFinanceQuote naverQuote,
            String priceSource,
            String historySource
    ) {
        List<Double> closes = yahooChart.dailyCloses();
        double price = yahooChart.regularMarketPrice();
        Instant quoteTime = yahooChart.quoteTime();
        double previousClose = yahooChart.previousClose();
        double changePercent = TechnicalIndicatorCalculator.changePercent(price, previousClose);
        PriceDisplayKind priceKind = MarketQuoteResolver.resolvePriceKind(quoteTime, yahooChart.exchangeZone());
        String priceLabel = MarketQuoteResolver.priceLabelKo(priceKind, quoteTime.atZone(yahooChart.exchangeZone()).toLocalDate());

        if (naverQuote != null) {
            price = naverQuote.price();
            quoteTime = naverQuote.quoteTime();
            changePercent = naverQuote.changePercent();
            previousClose = price / (1 + changePercent / 100.0);
            priceKind = MarketQuoteResolver.resolvePriceKind(quoteTime, KST);
            priceLabel = "현재가 (네이버·" + naverQuote.marketLabel() + ")";
        }

        double ma5 = TechnicalIndicatorCalculator.sma(closes, 5);
        double ma20 = TechnicalIndicatorCalculator.sma(closes, 20);
        double ma60 = TechnicalIndicatorCalculator.sma(closes, 60);
        double ma120 = TechnicalIndicatorCalculator.sma(closes, 120);
        double ma240 = TechnicalIndicatorCalculator.sma(closes, 240);
        double rsi14 = TechnicalIndicatorCalculator.rsi(closes, 14);
        MaAlignment alignment = TechnicalIndicatorCalculator.resolveMaAlignment(ma60, ma120, ma240);
        boolean short5 = !Double.isNaN(ma5) && price > ma5;
        boolean short20 = !Double.isNaN(ma20) && !Double.isNaN(ma5) && price > ma20 && ma5 > ma20;

        LocalDate tradingDate = quoteTime.atZone(KST).toLocalDate();

        return new CompanyMarketTechnicalDto(
                yahooSymbol,
                yahooChart.currency(),
                round(price),
                round(changePercent),
                round(previousClose),
                round(ma5),
                round(ma20),
                round(ma60),
                round(ma120),
                round(ma240),
                round(rsi14),
                alignment,
                short5,
                short20,
                quoteTime,
                tradingDate.toString(),
                priceKind.name(),
                priceLabel,
                Instant.now(),
                priceSource,
                historySource
        );
    }

    private static Double round(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return null;
        }
        return Math.round(value * 100.0) / 100.0;
    }
}
