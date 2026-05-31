package com.graphify.user;

import com.graphify.common.dto.ApiResponse;
import com.graphify.user.dto.ChangePasswordRequest;
import com.graphify.user.dto.MessageResponseDto;
import com.graphify.user.dto.UpdatePromptRequest;
import com.graphify.user.dto.UserMeDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public ApiResponse<UserMeDto> me() {
        return userProfileService.getMe();
    }

    @PutMapping("/me/password")
    public ApiResponse<MessageResponseDto> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        return userProfileService.changePassword(request);
    }

    @PutMapping("/me/prompt")
    public ApiResponse<MessageResponseDto> updatePrompt(
            @Valid @RequestBody UpdatePromptRequest request
    ) {
        return userProfileService.updatePrompt(request);
    }
}
