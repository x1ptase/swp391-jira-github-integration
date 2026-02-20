package com.swp391.backend.security;

import com.swp391.backend.entity.User;
import com.swp391.backend.repository.LecturerAssignmentRepository;
import com.swp391.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("securityService")
@RequiredArgsConstructor
public class SecurityService {

    private final LecturerAssignmentRepository lecturerAssignmentRepository;
    private final UserRepository userRepository;

    public boolean hasAccessToGroup(Long groupId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String username = authentication.getName();

        // Admin has full access
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }

        // Check if lecturer is assigned to this group
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LECTURER"))) {
            Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(username);
            if (userOpt.isPresent()) {
                return lecturerAssignmentRepository.existsByGroupIdAndLecturerId(groupId, userOpt.get().getUserId());
            }
        }

        return false;
    }
}
