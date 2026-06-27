package com.graphify.trading.paper.dto;

import java.util.List;

/**
 * 모의 룰 시작 요청(선택 바디). overrideSymbols가 비어있지 않으면
 * 유니버스 해석 대신 사용자가 직접 선택한 종목으로 시작한다.
 */
public record StartRuleRequest(
        List<String> overrideSymbols
) {}
