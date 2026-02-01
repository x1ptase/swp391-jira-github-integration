package com.swp391.backend.service.impl;

import com.swp391.backend.dto.request.LoginRequest;
import com.swp391.backend.dto.response.LoginResponse;
import com.swp391.backend.security.JwtProvider;
import com.swp391.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

        private final AuthenticationManager authenticationManager;
        private final JwtProvider jwtProvider;

        @Override
        public LoginResponse login(LoginRequest loginRequest) {
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),
                                                loginRequest.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                String jwt = jwtProvider.generateJwtToken(authentication);

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();

                return LoginResponse.builder()
                                .token(jwt)
                                .username(userDetails.getUsername())
                                .role(userDetails.getAuthorities().iterator().next().getAuthority())
                                .build();
        }
}
