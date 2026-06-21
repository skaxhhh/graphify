package com.graphify.market;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KrxMarketCalendarTest {

    @Mock
    MarketHolidayRepository holidayRepository;

    @InjectMocks
    KrxMarketCalendar calendar;

    @Test
    void weekday_not_holiday_is_trading_day() {
        // 2026-06-22 is a Monday
        LocalDate monday = LocalDate.of(2026, 6, 22);
        when(holidayRepository.existsByHolidayDate(monday)).thenReturn(false);
        assertThat(calendar.isTradingDay(monday)).isTrue();
    }

    @Test
    void saturday_is_not_trading_day() {
        LocalDate saturday = LocalDate.of(2026, 6, 20);
        assertThat(calendar.isTradingDay(saturday)).isFalse();
        verifyNoInteractions(holidayRepository);
    }

    @Test
    void sunday_is_not_trading_day() {
        LocalDate sunday = LocalDate.of(2026, 6, 21);
        assertThat(calendar.isTradingDay(sunday)).isFalse();
        verifyNoInteractions(holidayRepository);
    }

    @Test
    void weekday_in_market_holidays_is_not_trading_day() {
        // 2026-01-01 신정 — Thursday
        LocalDate holiday = LocalDate.of(2026, 1, 1);
        when(holidayRepository.existsByHolidayDate(holiday)).thenReturn(true);
        assertThat(calendar.isTradingDay(holiday)).isFalse();
    }

    @Test
    void weekday_not_in_market_holidays_is_trading_day() {
        LocalDate regularDay = LocalDate.of(2026, 6, 19); // Friday
        when(holidayRepository.existsByHolidayDate(regularDay)).thenReturn(false);
        assertThat(calendar.isTradingDay(regularDay)).isTrue();
    }
}
