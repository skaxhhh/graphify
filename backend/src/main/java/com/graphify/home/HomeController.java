package com.graphify.home;

import com.graphify.common.dto.ApiResponse;
import com.graphify.home.dto.BuzzCompanyDto;
import com.graphify.home.dto.MarketNewsItemDto;
import com.graphify.home.dto.MarketSentimentDto;
import com.graphify.home.dto.TrendingCompanyDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/trending-companies")
    public ApiResponse<List<TrendingCompanyDto>> trendingCompanies(
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return homeService.getTrendingCompanies(limit);
    }

    @GetMapping("/market-news")
    public ApiResponse<List<MarketNewsItemDto>> marketNews(
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return homeService.getMarketNews(limit);
    }

    @GetMapping("/buzz-companies")
    public ApiResponse<List<BuzzCompanyDto>> buzzCompanies(
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return homeService.getBuzzCompanies(limit);
    }

    @GetMapping("/market-sentiment")
    public ApiResponse<MarketSentimentDto> marketSentiment() {
        return homeService.getMarketSentiment();
    }
}
