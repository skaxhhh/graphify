package com.graphify.company.market;

import com.graphify.common.dto.ApiResponse;
import com.graphify.common.exception.GraphifyException;
import com.graphify.company.Company;
import com.graphify.company.CompanyRepository;
import com.graphify.company.dto.CompanyMarketTechnicalDto;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyMarketTechnicalService {

    private final CompanyRepository companyRepository;
    private final YahooFinanceChartClient yahooFinanceChartClient;
    private final NaverFinanceQuoteClient naverFinanceQuoteClient;

    public CompanyMarketTechnicalService(
            CompanyRepository companyRepository,
            YahooFinanceChartClient yahooFinanceChartClient,
            NaverFinanceQuoteClient naverFinanceQuoteClient
    ) {
        this.companyRepository = companyRepository;
        this.yahooFinanceChartClient = yahooFinanceChartClient;
        this.naverFinanceQuoteClient = naverFinanceQuoteClient;
    }

    @Transactional(readOnly = true)
    public ApiResponse<CompanyMarketTechnicalDto> getMarketTechnical(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new GraphifyException(
                        "ERR_COMPANY_001",
                        "기업을 찾을 수 없습니다.",
                        HttpStatus.NOT_FOUND
                ));

        return ApiResponse.ok(resolve(company).orElseThrow(() -> new GraphifyException(
                "ERR_COMPANY_007",
                "시세 데이터를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.",
                HttpStatus.BAD_GATEWAY
        )));
    }

    public Optional<CompanyMarketTechnicalDto> resolveOptional(Company company) {
        if (company == null) {
            return Optional.empty();
        }
        return resolve(company);
    }

    private Optional<CompanyMarketTechnicalDto> resolve(Company company) {
        Optional<String> yahooSymbol = YahooSymbolResolver.resolve(company.getTicker(), company.getMarket());
        Optional<YahooChartData> yahoo = yahooSymbol.flatMap(yahooFinanceChartClient::fetchDailyChartWithLiveQuote);
        Optional<NaverFinanceQuote> naver = naverFinanceQuoteClient.fetchQuote(company.getTicker());

        if (yahoo.isEmpty() && naver.isEmpty()) {
            return Optional.empty();
        }

        if (yahoo.isEmpty()) {
            return Optional.empty();
        }

        String priceSource = naver.isPresent() ? "NAVER" : "YAHOO";
        String historySource = "YAHOO";

        return Optional.of(CompositeMarketTechnicalBuilder.build(
                yahooSymbol.orElse(company.getTicker()),
                yahoo.get(),
                naver.orElse(null),
                priceSource,
                historySource
        ));
    }
}
