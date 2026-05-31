package com.graphify.watchlist;

import com.graphify.common.dto.ApiResponse;
import com.graphify.watchlist.dto.AddWatchlistRequest;
import com.graphify.watchlist.dto.WatchlistDataDto;
import com.graphify.watchlist.dto.WatchlistItemDto;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping("/me")
    public ApiResponse<WatchlistDataDto> myWatchlist() {
        return watchlistService.getMyWatchlist();
    }

    @PostMapping("/me")
    public ApiResponse<WatchlistItemDto> add(@RequestBody AddWatchlistRequest request) {
        return watchlistService.addItem(request);
    }

    @DeleteMapping("/me/{companyId}")
    public ApiResponse<Void> remove(@PathVariable Long companyId) {
        return watchlistService.removeItem(companyId);
    }
}
