package com.swp391.backend.controller;

import com.swp391.backend.dto.response.ApiResponse;
import com.swp391.backend.dto.request.UpdateProfileRequest;
import com.swp391.backend.dto.response.UserResponse;
import com.swp391.backend.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    // user login là gọi được
    @PutMapping("/profile")
    public ApiResponse<UserResponse> updateProfile(@RequestBody UpdateProfileRequest request) {
        UserResponse updated = userService.updateProfile(request);
        return ApiResponse.success(updated);
    }
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe() {
        return ApiResponse.success(userService.getMe());
    }
}