package com.graphify.company.registry.dart;

record DartCorpCodeEntry(
        String corpCode,
        String corpName,
        String stockCode,
        String modifyDate
) {
    boolean isListed() {
        return stockCode != null && !stockCode.isBlank();
    }
}
