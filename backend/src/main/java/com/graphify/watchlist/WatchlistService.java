package com.graphify.watchlist;

import com.graphify.common.dto.ApiResponse;
import com.graphify.common.exception.GraphifyException;
import com.graphify.company.Company;
import com.graphify.company.CompanyRepository;
import com.graphify.history.HistoryService;
import com.graphify.watchlist.dto.AddWatchlistRequest;
import com.graphify.watchlist.dto.WatchlistDataDto;
import com.graphify.watchlist.dto.WatchlistItemDto;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WatchlistService {

    private final WatchlistItemRepository watchlistItemRepository;
    private final CompanyRepository companyRepository;

    public WatchlistService(
            WatchlistItemRepository watchlistItemRepository,
            CompanyRepository companyRepository
    ) {
        this.watchlistItemRepository = watchlistItemRepository;
        this.companyRepository = companyRepository;
    }

    public ApiResponse<WatchlistDataDto> getMyWatchlist() {
        Long userId = HistoryService.requireCurrentUserId();
        List<WatchlistItemDto> items = watchlistItemRepository.findByUserIdOrderByAddedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
        return ApiResponse.ok(new WatchlistDataDto(items));
    }

    @Transactional
    public ApiResponse<WatchlistItemDto> addItem(AddWatchlistRequest request) {
        Long userId = HistoryService.requireCurrentUserId();
        if (request.companyId() == null || request.companyId() <= 0) {
            throw new GraphifyException(
                    "ERR_WATCHLIST_001",
                    "유효하지 않은 기업입니다.",
                    HttpStatus.BAD_REQUEST
            );
        }

        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new GraphifyException(
                        "ERR_COMPANY_001",
                        "기업 정보를 찾을 수 없습니다.",
                        HttpStatus.NOT_FOUND
                ));

        if (watchlistItemRepository.findByUserIdAndCompanyId(userId, company.getId()).isPresent()) {
            return ApiResponse.ok(toDto(
                    watchlistItemRepository.findByUserIdAndCompanyId(userId, company.getId()).orElseThrow()
            ));
        }

        WatchlistItem item = new WatchlistItem(userId, company.getId());
        watchlistItemRepository.save(item);
        return ApiResponse.ok(new WatchlistItemDto(
                company.getId(),
                company.getName(),
                company.getIndustry(),
                company.getTicker(),
                item.getAddedAt()
        ));
    }

    @Transactional
    public ApiResponse<Void> removeItem(Long companyId) {
        Long userId = HistoryService.requireCurrentUserId();
        if (companyId == null || companyId <= 0) {
            throw new GraphifyException(
                    "ERR_WATCHLIST_001",
                    "유효하지 않은 기업입니다.",
                    HttpStatus.BAD_REQUEST
            );
        }
        watchlistItemRepository.deleteByUserIdAndCompanyId(userId, companyId);
        return ApiResponse.ok(null);
    }

    public boolean isWatched(Long userId, Long companyId) {
        return watchlistItemRepository.findByUserIdAndCompanyId(userId, companyId).isPresent();
    }

    private WatchlistItemDto toDto(WatchlistItem item) {
        Company company = companyRepository.findById(item.getCompanyId())
                .orElseThrow(() -> new GraphifyException(
                        "ERR_COMPANY_001",
                        "기업 정보를 찾을 수 없습니다.",
                        HttpStatus.NOT_FOUND
                ));
        return new WatchlistItemDto(
                company.getId(),
                company.getName(),
                company.getIndustry(),
                company.getTicker(),
                item.getAddedAt()
        );
    }
}
