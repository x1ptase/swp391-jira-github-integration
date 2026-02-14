package com.swp391.backend.service.impl;

import com.swp391.backend.dto.request.CreateUserRequest;
import com.swp391.backend.dto.request.UpdateUserRequest;
import com.swp391.backend.dto.response.UserResponse;
import com.swp391.backend.entity.Role;
import com.swp391.backend.entity.User;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.RoleRepository;
import com.swp391.backend.repository.UserRepository;
import com.swp391.backend.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        String username = safeTrim(request.getUsername());
        String email = safeTrim(request.getEmail());
        String fullName = safeTrim(request.getFullName());
        String roleCode = safeTrim(request.getRoleCode()).toUpperCase();
        String githubUsername = safeTrim(request.getGithubUsername());
        String jiraEmail = safeTrim(request.getJiraEmail());

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException("Username already exists: " + username, 409);
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email already exists: " + email, 409);
        }
        if (!githubUsername.isEmpty() && userRepository.existsByGithubUsernameIgnoreCase(githubUsername)) {
            throw new BusinessException("Github username already exists: " + githubUsername, 409);
        }
        if (!jiraEmail.isEmpty() && userRepository.existsByJiraEmailIgnoreCase(jiraEmail)) {
            throw new BusinessException("Jira email already exists: " + jiraEmail, 409);
        }

        String rawPassword = safeTrim(request.getPassword());
        if (rawPassword.isEmpty()) {
            throw new BusinessException("Password is required when creating a user", 400);
        }
        if (rawPassword.length() < 6 || rawPassword.length() > 72) {
            throw new BusinessException("Password length must be 6-72", 400);
        }

        Role role = getRoleOrThrow(roleCode);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setGithubUsername(githubUsername.isEmpty() ? null : githubUsername);
        user.setJiraEmail(jiraEmail.isEmpty() ? null : jiraEmail);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));

        try {
            User saved = userRepository.save(user);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Unique constraint violated (username/email/github/jira)", 409);
        }
    }

    @Override
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = getUserOrThrow(userId);

        String newEmail = safeTrim(request.getEmail());
        String newFullName = safeTrim(request.getFullName());
        String newRoleCode = safeTrim(request.getRoleCode()).toUpperCase();
        String newGithub = safeTrim(request.getGithubUsername());
        String newJira = safeTrim(request.getJiraEmail());

        if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(newEmail)) {
            throw new BusinessException("Email already exists: " + newEmail, 409);
        }

        if (!newGithub.isEmpty()) {
            String oldGithub = user.getGithubUsername() == null ? "" : user.getGithubUsername();
            if (!newGithub.equalsIgnoreCase(oldGithub) && userRepository.existsByGithubUsernameIgnoreCase(newGithub)) {
                throw new BusinessException("Github username already exists: " + newGithub, 409);
            }
        }

        if (!newJira.isEmpty()) {
            String oldJira = user.getJiraEmail() == null ? "" : user.getJiraEmail();
            if (!newJira.equalsIgnoreCase(oldJira) && userRepository.existsByJiraEmailIgnoreCase(newJira)) {
                throw new BusinessException("Jira email already exists: " + newJira, 409);
            }
        }

        Role role = getRoleOrThrow(newRoleCode);

        user.setEmail(newEmail);
        user.setFullName(newFullName);
        user.setRole(role);
        user.setGithubUsername(newGithub.isEmpty() ? null : newGithub);
        user.setJiraEmail(newJira.isEmpty() ? null : newJira);

        try {
            User saved = userRepository.save(user);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Unique constraint violated (email/github/jira)", 409);
        }
    }

    @Override
    public UserResponse getUserById(Long userId) {
        User user = getUserOrThrow(userId);
        return toResponse(user);
    }

    @Override
    public Page<UserResponse> listUsers(String keyword, Pageable pageable) {
        return listUsers(keyword, null, pageable);
    }

    @Override
    public Page<UserResponse> listUsers(String keyword, String roleCode, Pageable pageable) {
        Page<User> page = userRepository.searchWithRole(keyword, roleCode, pageable);
        List<User> users = page.getContent();

        List<UserResponse> dtoList = new ArrayList<UserResponse>();
        for (int i = 0; i < users.size(); i++) {
            dtoList.add(toResponse(users.get(i)));
        }

        return new PageImpl<UserResponse>(dtoList, pageable, page.getTotalElements());
    }

    @Override
    public void deleteUser(Long userId) {
        // Nếu user đang dính FK (GroupMember, LecturerAssignment, ...) -> DB sẽ chặn
        User user = getUserOrThrow(userId);
        try {
            userRepository.delete(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Cannot delete user because it is referenced by other records", 409);
        }
    }

    private User getUserOrThrow(Long id) {
        Optional<User> opt = userRepository.findById(id);
        if (!opt.isPresent()) {
            throw new BusinessException("User not found: " + id, 404);
        }
        return opt.get();
    }

    private Role getRoleOrThrow(String roleCode) {
        Optional<Role> roleOpt = roleRepository.findByRoleCode(roleCode);
        if (!roleOpt.isPresent()) {
            throw new BusinessException("Role not found: " + roleCode, 400);
        }
        return roleOpt.get();
    }

    private UserResponse toResponse(User u) {
        UserResponse r = new UserResponse();
        r.setUserId(u.getUserId());
        r.setUsername(u.getUsername());
        r.setFullName(u.getFullName());
        r.setEmail(u.getEmail());
        r.setGithubUsername(u.getGithubUsername());
        r.setJiraEmail(u.getJiraEmail());
        if (u.getRole() != null) {
            r.setRoleCode(u.getRole().getRoleCode());
        }
        r.setCreatedAt(u.getCreatedAt());
        return r;
    }

    private String safeTrim(String s) {
        if (s == null) return "";
        return s.trim();
    }
}
