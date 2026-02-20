package com.swp391.backend.service.impl;

import com.swp391.backend.dto.request.LoginRequest;
import com.swp391.backend.dto.request.RegisterRequest;
import com.swp391.backend.dto.response.LoginResponse;
import com.swp391.backend.entity.Role;
import com.swp391.backend.entity.User;
import com.swp391.backend.repository.RoleRepository;
import com.swp391.backend.repository.UserRepository;
import com.swp391.backend.security.JwtProvider;
import com.swp391.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

        private final AuthenticationManager authenticationManager;
        private final JwtProvider jwtProvider;
        private final PasswordEncoder passwordEncoder;
        private final UserRepository userRepository;
        private final RoleRepository roleRepository;

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

        @Override
        public void register(RegisterRequest registerRequest) {
                if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
                        throw new RuntimeException("Error: Username is already taken!");
                }
                if (userRepository.existsByEmailIgnoreCase(registerRequest.getEmail())) {
                        throw new RuntimeException("Error: Email is already taken!");
                }
                if (registerRequest.getGithubUsername() != null && !registerRequest.getGithubUsername().trim().isEmpty()
                                && userRepository.existsByGithubUsernameIgnoreCase(
                                                registerRequest.getGithubUsername())) {
                        throw new RuntimeException("Error: GitHub Username is already taken!");
                }
                if (registerRequest.getJiraAccountId() != null && !registerRequest.getJiraAccountId().trim().isEmpty()
                                && userRepository.existsByJiraAccountIdIgnoreCase(registerRequest.getJiraAccountId())) {
                        throw new RuntimeException("Error: Jira Account Id is already taken!");
                }

                // Determine and validate role
                String roleCode = registerRequest.getRoleCode();
                if (roleCode == null || roleCode.trim().isEmpty()) {
                        roleCode = "STUDENT";
                } else {
                        roleCode = roleCode.toUpperCase();
                        if (roleCode.equals("ADMIN")) {
                                throw new RuntimeException("Error: Cannot register as ADMIN via this API!");
                        }
                        if (!roleCode.equals("STUDENT") && !roleCode.equals("LECTURER")) {
                                throw new RuntimeException(
                                                "Error: Invalid role selection. Allowed roles: STUDENT, LECTURER");
                        }
                }

                Role userRole = roleRepository.findByRoleCode(roleCode)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

                User user = User.builder()
                                .username(registerRequest.getUsername())
                                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                                .fullName(registerRequest.getFullName())
                                .email(registerRequest.getEmail())
                                .githubUsername(registerRequest.getGithubUsername())
                                .jiraAccountId(registerRequest.getJiraAccountId())
                                .role(userRole)
                                .build();

                userRepository.save(user);
        }
}
