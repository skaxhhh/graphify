package com.graphify.market;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Service;

/**
 * KRX 장 중 판단 서비스.
 * 주말 및 market_holidays 테이블에 등록된 날짜를 비거래일로 처리.
 * Phase 3 평가 엔진에서도 재사용한다.
 */
@Service
public class KrxMarketCalendar {

    private static final LocalTime OPERATING_OPEN  = LocalTime.of(9, 0);
    private static final LocalTime OPERATING_CLOSE = LocalTime.of(18, 0);

    private final MarketHolidayRepository holidayRepository;

    public KrxMarketCalendar(MarketHolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    /**
     * 거래일 여부 반환.
     * @param date KST 기준 날짜
     * @return true if weekday and not in market_holidays
     */
    public boolean isTradingDay(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return false;
        }
        return !holidayRepository.existsByHolidayDate(date);
    }

    /**
     * 전략 운영 창 개장 여부 — 거래일(평일−공휴일) AND 09:00–18:00 KST.
     * KRX 정규장(09:00–15:30)과 별개의 "운영" 시간대: 전략 시작 가능/자동 중지 기준.
     */
    public boolean isOperatingWindowOpen(ZonedDateTime now) {
        if (!isTradingDay(now.toLocalDate())) return false;
        LocalTime t = now.toLocalTime();
        return !t.isBefore(OPERATING_OPEN) && t.isBefore(OPERATING_CLOSE);
    }
}
