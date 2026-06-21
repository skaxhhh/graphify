package com.graphify.trading.paper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphify.trading.paper.dto.MonitorDto;
import com.graphify.trading.paper.dto.SignalLogItem;
import com.graphify.trading.paper.dto.TradeItem;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PaperMonitorService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PaperAccountRepository accountRepo;
    private final PaperSignalLogRepository signalLogRepo;
    private final PaperTradeRepository tradeRepo;
    private final ObjectMapper objectMapper;

    public PaperMonitorService(
            PaperAccountRepository accountRepo,
            PaperSignalLogRepository signalLogRepo,
            PaperTradeRepository tradeRepo,
            ObjectMapper objectMapper) {
        this.accountRepo = accountRepo;
        this.signalLogRepo = signalLogRepo;
        this.tradeRepo = tradeRepo;
        this.objectMapper = objectMapper;
    }

    public MonitorDto getMonitor(Long userId) {
        // Scheduler last run: max ts from recent signal logs
        List<PaperSignalLog> logs = signalLogRepo.findTop50ByOrderByTsDesc();
        Instant schedulerLastRun = logs.stream()
                .map(PaperSignalLog::getTs)
                .max(Instant::compareTo)
                .orElse(null);

        // Market status: KST weekday 09:00–15:30 = OPEN
        String marketStatus = resolveMarketStatus();

        // Recent signals: parse indicator_snapshot JSON
        List<SignalLogItem> recentSignals = logs.stream()
                .map(this::toSignalLogItem)
                .toList();

        // Today's trades: filter to today KST midnight onwards
        Optional<PaperAccount> accountOpt = accountRepo.findByUserId(userId);
        List<TradeItem> todayTrades;
        if (accountOpt.isEmpty()) {
            todayTrades = Collections.emptyList();
        } else {
            Instant todayMidnightKst = ZonedDateTime.now(KST)
                    .toLocalDate()
                    .atStartOfDay(KST)
                    .toInstant();
            todayTrades = tradeRepo.findByAccountIdOrderByTradedAtDesc(accountOpt.get().getId())
                    .stream()
                    .filter(t -> !t.getTradedAt().isBefore(todayMidnightKst))
                    .map(this::toTradeItem)
                    .toList();
        }

        return new MonitorDto(schedulerLastRun, marketStatus, recentSignals, todayTrades);
    }

    private String resolveMarketStatus() {
        ZonedDateTime now = ZonedDateTime.now(KST);
        int dow = now.getDayOfWeek().getValue(); // 1=Mon, 7=Sun
        if (dow >= 6) return "CLOSED"; // Saturday or Sunday

        int hour = now.getHour();
        int minute = now.getMinute();
        int totalMinutes = hour * 60 + minute;
        // 09:00 = 540, 15:30 = 930
        if (totalMinutes >= 540 && totalMinutes < 930) {
            return "OPEN";
        }
        return "CLOSED";
    }

    private SignalLogItem toSignalLogItem(PaperSignalLog log) {
        Double rsi14 = null;
        Double sma20 = null;
        Double price = null;

        String snapshot = log.getIndicatorSnapshot();
        if (snapshot != null && !snapshot.isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(snapshot);
                if (node.has("rsi14") && !node.get("rsi14").isNull()) {
                    rsi14 = node.get("rsi14").asDouble();
                }
                if (node.has("sma20") && !node.get("sma20").isNull()) {
                    sma20 = node.get("sma20").asDouble();
                }
                if (node.has("price") && !node.get("price").isNull()) {
                    price = node.get("price").asDouble();
                }
            } catch (JsonProcessingException e) {
                // parse failure — leave nulls, non-fatal
            }
        }

        return new SignalLogItem(
                log.getId(), log.getRuleId(), log.getSymbol(), log.getTs(),
                log.getSignal(), log.isExecuted(), rsi14, sma20, price
        );
    }

    private TradeItem toTradeItem(PaperTrade trade) {
        return new TradeItem(
                trade.getId(),
                trade.getSymbol(),
                trade.getSide(),
                trade.getQty().doubleValue(),
                trade.getPrice().doubleValue(),
                trade.getPnl() != null ? trade.getPnl().doubleValue() : null,
                trade.getTradedAt()
        );
    }
}
