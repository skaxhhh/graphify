package com.graphify.auth;

import com.graphify.auth.dto.ConsentRequestDto;
import com.graphify.auth.dto.ConsentResponseDto;
import com.graphify.auth.dto.UserDto;
import com.graphify.common.exception.GraphifyException;
import com.graphify.terms.TermsAcceptance;
import com.graphify.terms.TermsAcceptanceRepository;
import com.graphify.terms.TermsDocument;
import com.graphify.terms.TermsDocumentRepository;
import com.graphify.user.UserAuthProviderRepository;
import com.graphify.terms.dto.TermItemDto;
import com.graphify.terms.dto.TermsLatestDto;
import com.graphify.user.User;
import com.graphify.user.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsentService {

    private final UserRepository userRepository;
    private final TermsDocumentRepository termsDocumentRepository;
    private final TermsAcceptanceRepository termsAcceptanceRepository;
    private final com.graphify.terms.TermsService termsService;
    private final UserAuthProviderRepository userAuthProviderRepository;

    public ConsentService(
            UserRepository userRepository,
            TermsDocumentRepository termsDocumentRepository,
            TermsAcceptanceRepository termsAcceptanceRepository,
            com.graphify.terms.TermsService termsService,
            UserAuthProviderRepository userAuthProviderRepository
    ) {
        this.userRepository = userRepository;
        this.termsDocumentRepository = termsDocumentRepository;
        this.termsAcceptanceRepository = termsAcceptanceRepository;
        this.termsService = termsService;
        this.userAuthProviderRepository = userAuthProviderRepository;
    }

    @Transactional
    public ConsentResponseDto submitConsent(Long userId, ConsentRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GraphifyException(
                        "ERR_AUTH_005",
                        "사용자를 찾을 수 없습니다.",
                        HttpStatus.UNAUTHORIZED
                ));

        TermsLatestDto latest = termsService.getLatest();
        if (!latest.version().equals(request.version())) {
            throw new GraphifyException(
                    "ERR_TERMS_001",
                    "약관 버전이 일치하지 않습니다. 새로고침 후 다시 시도해 주세요.",
                    HttpStatus.BAD_REQUEST
            );
        }

        List<TermItemDto> requiredTerms = latest.terms().stream()
                .filter(TermItemDto::required)
                .toList();

        Set<Long> acceptedIds = new HashSet<>(request.acceptedTermIds());
        boolean allRequiredAccepted = requiredTerms.stream()
                .allMatch(term -> acceptedIds.contains(term.id()));

        if (!allRequiredAccepted) {
            throw new GraphifyException(
                    "ERR_TERMS_002",
                    "필수 약관에 모두 동의해 주세요.",
                    HttpStatus.BAD_REQUEST
            );
        }

        for (Long termId : acceptedIds) {
            TermsDocument document = termsDocumentRepository.findById(termId)
                    .orElseThrow(() -> new GraphifyException(
                            "ERR_TERMS_003",
                            "유효하지 않은 약관입니다.",
                            HttpStatus.BAD_REQUEST
                    ));
            termsAcceptanceRepository.save(
                    TermsAcceptance.of(userId, document.getId(), document.getVersion())
            );
        }

        user.setTermsAccepted(true);

        String authProvider = userAuthProviderRepository.findFirstByUser_Id(userId)
                .map(link -> link.getProvider())
                .orElse("email");

        UserDto userDto = new UserDto(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                true,
                false,
                authProvider,
                user.getRole().name()
        );

        return new ConsentResponseDto(userDto);
    }
}
