package com.swp391.backend.service;


import com.swp391.backend.dto.request.CreateUserRequest;
import com.swp391.backend.dto.request.UpdateUserRequest;
import com.swp391.backend.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

    public interface UserService {
        UserResponse createUser(CreateUserRequest request);
        UserResponse updateUser(Long userId, UpdateUserRequest request);
        UserResponse getUserById(Long userId);
        Page<UserResponse> listUsers(String keyword, Pageable pageable);
        void deleteUser(Long userId);
    }


