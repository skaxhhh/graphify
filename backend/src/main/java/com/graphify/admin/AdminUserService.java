package com.graphify.admin;

import com.graphify.admin.dto.AdminCreateUserRequest;
import com.graphify.admin.dto.AdminUserDto;
import com.graphify.common.dto.ApiResponse;
import com.graphify.common.exception.GraphifyException;
import com.graphify.user.User;
import com.graphify.user.UserRepository;
import com.graphify.user.UserRole;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ApiResponse<List<AdminUserDto>> getAllUsers() {
        List<AdminUserDto> users = userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
        return ApiResponse.ok(users);
    }

    private AdminUserDto toDto(User u) {
        return new AdminUserDto(
                u.getId(),
                u.getEmail(),
                u.getDisplayName(),
                u.getRole().name(),
                u.isTradingEnabled(),
                u.isTermsAccepted(),
                u.getCreatedAt()
        );
    }

    @Transactional
    public ApiResponse<AdminUserDto> createUser(AdminCreateUserRequest request) {
        if (userRepository.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new GraphifyException("ERR_USER_DUPLICATE", "이미 존재하는 이메일입니다.", HttpStatus.CONFLICT);
        }
        User user = new User();
        user.setEmail(request.email());
        user.setDisplayName(request.displayName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole("ADMIN".equalsIgnoreCase(request.role()) ? UserRole.ADMIN : UserRole.USER);
        user.setTermsAccepted(true);
        userRepository.save(user);
        return ApiResponse.ok(toDto(user));
    }

    @Transactional
    public ApiResponse<AdminUserDto> setTradingAccess(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GraphifyException(
                        "ERR_USER_NOT_FOUND",
                        "사용자를 찾을 수 없습니다.",
                        HttpStatus.NOT_FOUND
                ));
        user.setTradingEnabled(enabled);
        userRepository.save(user);
        return ApiResponse.ok(toDto(user));
    }
}
