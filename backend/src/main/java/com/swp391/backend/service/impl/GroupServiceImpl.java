package com.swp391.backend.service.impl;

import com.swp391.backend.entity.User;
import com.swp391.backend.repository.GroupMemberRepository;
import com.swp391.backend.repository.UserRepository;
import com.swp391.backend.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Override
    public boolean isUserAuthorized(Long userId, Long groupId, List<String> allowedRoles) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        // 1. Kiểm tra system role (ADMIN luôn có quyền nếu ADMIN trong allowedRoles)
        if (user.getRole() != null && allowedRoles.contains(user.getRole().getRoleCode())) {
            return true;
        }

        // 2. Kiểm tra member role trong group
        return allowedRoles.stream().anyMatch(roleCode -> groupMemberRepository
                .existsByGroup_GroupIdAndUser_UserIdAndMemberRole_Code(groupId, userId, roleCode));
    }
}
