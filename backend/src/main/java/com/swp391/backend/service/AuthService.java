package com.swp391.backend.service;

import com.swp391.backend.dto.request.LoginRequest;
import com.swp391.backend.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest loginRequest);
}
