package com.swp391.backend.security;

import com.swp391.backend.entity.User;
import com.swp391.backend.repository.GroupMemberRepository;
import com.swp391.backend.repository.LecturerAssignmentRepository;
import com.swp391.backend.repository.StudentGroupRepository;
import com.swp391.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("securityService")
@RequiredArgsConstructor
public class SecurityService {

    private final LecturerAssignmentRepository lecturerAssignmentRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final StudentGroupRepository studentGroupRepository;

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String username = authentication.getName();
        return userRepository.findByUsernameIgnoreCase(username)
                .map(User::getUserId)
                .orElse(null);
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public boolean hasAccessToGroup(Long groupId) {
        if (isAdmin())
            return true;

        Long userId = getCurrentUserId();
        System.out.println(">>> hasAccessToGroup groupId=" + groupId + " userId=" + userId);
        if (userId == null)
            return false;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"))) {
            boolean exists = groupMemberRepository.existsByGroup_GroupIdAndUser_UserId(groupId, userId);
            System.out.println(">>> STUDENT check exists=" + exists);
            return exists;
        }
        // Lecturer assigned to group
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_LECTURER"))) {
            return studentGroupRepository.findById(groupId)
                    .map(group -> lecturerAssignmentRepository.existsByClassIdAndLecturerId(group.getAcademicClass().getClassId(), userId))
                    .orElse(false);
        }

        // Student in group
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"))) {
            return groupMemberRepository.existsByGroup_GroupIdAndUser_UserId(groupId, userId);
        }

        return false;
    }

    public boolean isGroupManager(Long groupId) {
        if (isAdmin())
            return true;

            
        Long userId = getCurrentUserId();
        if (userId == null)
            return false;
            

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Lecturer assigned to group
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_LECTURER"))) {
            return studentGroupRepository.findById(groupId)
                    .map(group -> lecturerAssignmentRepository.existsByClassIdAndLecturerId(group.getAcademicClass().getClassId(), userId))
                    .orElse(false);
        }

        // Student is Leader of the group
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"))) {
            return groupMemberRepository.existsByGroup_GroupIdAndUser_UserIdAndMemberRole_Code(groupId, userId,
                    "LEADER");
        }

        return false;
    }

    /**
     * Chỉ cho phép sinh viên thuộc nhóm truy cập.
     * Dùng cho các endpoint "cá nhân" như xem commit của chính mình (/me).
     * Lecturer và Admin (nếu không phải thành viên) sẽ bị chặn.
     */
    public boolean isStudentInGroup(Long groupId) {
        Long userId = getCurrentUserId();
        if (userId == null || groupId == null)
            return false;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        if (!isStudent)
            return false;

        return groupMemberRepository.existsByGroup_GroupIdAndUser_UserId(groupId, userId);
    }

    public boolean isLecturerAssigned(Long groupId) {
        Long userId = getCurrentUserId();
        if (userId == null || groupId == null) return false;

        return studentGroupRepository.findById(groupId)
                .map(group -> lecturerAssignmentRepository.existsByClassIdAndLecturerId(group.getAcademicClass().getClassId(), userId))
                .orElse(false);
    }

    @Deprecated
    public boolean isGroupLeader(Long groupId) {
        return isGroupManager(groupId);
    }
}
