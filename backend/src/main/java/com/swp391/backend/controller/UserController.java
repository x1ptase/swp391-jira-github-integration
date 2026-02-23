package com.swp391.backend.controller;

import com.swp391.backend.common.ApiResponse;
import com.swp391.backend.dto.request.CreateUserRequest;
import com.swp391.backend.dto.request.UpdateUserRequest;
import com.swp391.backend.dto.response.UserResponse;
import com.swp391.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = userService.createUser(request);
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable("id") Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse updated = userService.updateUser(id, request);
        return ApiResponse.success(updated);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> detail(@PathVariable("id") Long id) {
        UserResponse searched = userService.getUserById(id);
        return ApiResponse.success(searched);
    }

    @GetMapping
    public ApiResponse<Page<UserResponse>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "roleCode", required = false) String roleCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserResponse> result = userService.listUsers(keyword, roleCode, pageable);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> delete(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return ApiResponse.success(null);
    }
}
