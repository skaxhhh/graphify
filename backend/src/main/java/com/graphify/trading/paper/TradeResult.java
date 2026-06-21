package com.graphify.trading.paper;

/**
 * PaperExecutor.execute() 결과. executed=false 시 reason에 사유 기록.
 */
public record TradeResult(
        boolean executed,
        String signal,
        String symbol,
        Double fillPrice,
        Double qty,
        Double pnl,
        String reason   // null if executed=true; "ALREADY_HOLDS" | "NO_POSITION" | "INSUFFICIENT_CASH" | "ZERO_QTY" | "HOLD"
) {
    public static TradeResult skipped(String signal, String symbol, String reason) {
        return new TradeResult(false, signal, symbol, null, null, null, reason);
    }

    public static TradeResult filled(String signal, String symbol, double fillPrice, double qty, Double pnl) {
        return new TradeResult(true, signal, symbol, fillPrice, qty, pnl, null);
    }
}
