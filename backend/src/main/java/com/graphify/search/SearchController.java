package com.graphify.search;

import com.graphify.common.dto.ApiResponse;
import com.graphify.common.exception.GraphifyException;
import com.graphify.search.dto.AutocompleteItemDto;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/autocomplete")
    public ApiResponse<List<AutocompleteItemDto>> autocomplete(@RequestParam("q") String q) {
        if (q == null || q.trim().length() < 2) {
            throw new GraphifyException(
                    "ERR_SEARCH_001",
                    "검색어는 2자 이상이어야 합니다.",
                    HttpStatus.BAD_REQUEST
            );
        }
        return ApiResponse.ok(searchService.autocomplete(q));
    }
}
