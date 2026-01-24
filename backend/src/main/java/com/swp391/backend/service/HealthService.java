package com.swp391.backend.service;

import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public String getStatus() {
        return "Backend is running";
    }
}
