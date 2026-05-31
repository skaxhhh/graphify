package com.graphify.company.market;

import java.time.LocalDate;

public record DailyBar(LocalDate tradingDate, double close) {
}
