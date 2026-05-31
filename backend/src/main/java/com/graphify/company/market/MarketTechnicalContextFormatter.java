package com.graphify.company.market;

import com.graphify.company.dto.CompanyMarketTechnicalDto;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class MarketTechnicalContextFormatter {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter QUOTE_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(KST);

    private MarketTechnicalContextFormatter() {
    }

    public static String format(CompanyMarketTechnicalDto dto) {
        if (dto == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("## 시장·기술 지표\n");
        if (dto.priceSource() != null || dto.historySource() != null) {
            sb.append("- 데이터: 장중 ")
                    .append(sourceLabel(dto.priceSource()))
                    .append(" · 일봉/지표 ")
                    .append(sourceLabel(dto.historySource()))
                    .append('\n');
        }
        if (dto.yahooSymbol() != null) {
            sb.append("- 심볼: ").append(dto.yahooSymbol()).append('\n');
        }
        if (dto.price() != null) {
            String label = dto.priceLabel() != null ? dto.priceLabel() : "시세";
            sb.append("- ").append(label).append(": ").append(formatNumber(dto.price()));
            if ("KRW".equals(dto.currency())) {
                sb.append("원");
            } else if (dto.currency() != null) {
                sb.append(' ').append(dto.currency());
            }
            if (dto.changePercent() != null) {
                sb.append(" (전일 대비 ").append(formatSignedPercent(dto.changePercent())).append(')');
            }
            sb.append('\n');
        }
        if (dto.quoteTime() != null) {
            sb.append("- 시세 시각: ").append(QUOTE_TIME_FMT.format(dto.quoteTime())).append(" (KST)\n");
        }
        if (dto.tradingDate() != null) {
            sb.append("- 거래일: ").append(dto.tradingDate()).append('\n');
        }
        if (dto.previousClose() != null) {
            sb.append("- 전일 종가: ").append(formatNumber(dto.previousClose()));
            if ("KRW".equals(dto.currency())) {
                sb.append("원");
            }
            sb.append('\n');
        }
        if (dto.rsi14() != null) {
            sb.append("- RSI(14): ").append(dto.rsi14()).append('\n');
        }
        sb.append("- 60·120·240 추세: ").append(alignmentLabel(dto.maAlignment())).append('\n');
        sb.append("- 초단기(5일선): ")
                .append(dto.shortTermRise5() ? "상승 (종가 > MA5)" : "해당 없음/하락")
                .append('\n');
        sb.append("- 단기(20일선): ")
                .append(dto.shortTermRise20() ? "상승 (종가>MA20, MA5>MA20)" : "해당 없음/하락")
                .append('\n');
        sb.append("- MA5: ").append(formatMa(dto.ma5()))
                .append(", MA20: ").append(formatMa(dto.ma20()))
                .append(", MA60: ").append(formatMa(dto.ma60()))
                .append(", MA120: ").append(formatMa(dto.ma120()))
                .append(", MA240: ").append(formatMa(dto.ma240()))
                .append('\n');
        return sb.toString().trim();
    }

    private static String sourceLabel(String source) {
        if (source == null) {
            return "—";
        }
        return switch (source) {
            case "NAVER" -> "네이버 금융";
            case "YAHOO" -> "Yahoo Finance";
            case "KRX" -> "KRX Open API";
            default -> source;
        };
    }

    private static String alignmentLabel(MaAlignment alignment) {
        if (alignment == null) {
            return "미확인";
        }
        return switch (alignment) {
            case BULLISH_ALIGN -> "정배열 (MA60 > MA120 > MA240, 상승 추세)";
            case BEARISH_ALIGN -> "역배열 (MA60 < MA120 < MA240, 하락 추세)";
            case MIXED -> "혼조 (정·역배열 아님)";
        };
    }

    private static String formatMa(Double value) {
        return value != null ? formatNumber(value) : "—";
    }

    private static String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.format("%,.0f", value);
        }
        return String.format("%,.2f", value);
    }

    private static String formatSignedPercent(double value) {
        String sign = value > 0 ? "+" : "";
        return sign + String.format("%.2f", value) + "%";
    }
}
