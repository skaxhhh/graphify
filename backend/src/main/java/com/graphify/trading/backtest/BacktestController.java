package com.graphify.trading.backtest;

import com.graphify.common.dto.ApiResponse;
import com.graphify.market.MarketBarIntradayRepository;
import com.graphify.trading.backtest.dto.BacktestRequest;
import com.graphify.trading.backtest.dto.BacktestResult;
import com.graphify.trading.backtest.dto.CandleBarDto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trading/paper/backtest")
public class BacktestController {

    private final BacktestService backtestService;
    private final MarketBarIntradayRepository intradayRepository;

    public BacktestController(BacktestService backtestService,
                              MarketBarIntradayRepository intradayRepository) {
        this.backtestService = backtestService;
        this.intradayRepository = intradayRepository;
    }

    @PostMapping
    public ApiResponse<BacktestResult> run(@RequestBody BacktestRequest request) {
        return backtestService.run(request);
    }

    @GetMapping("/bars")
    public ApiResponse<List<CandleBarDto>> bars(
            @RequestParam String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        ZoneId kst = ZoneId.of("Asia/Seoul");
        Instant from = date.atStartOfDay(kst).toInstant();
        Instant to   = date.atTime(23, 59, 59).atZone(kst).toInstant();
        List<CandleBarDto> dtos = intradayRepository.findBySymbolAndRange(symbol, from, to)
                .stream().map(CandleBarDto::from).toList();
        return ApiResponse.ok(dtos);
    }
}
