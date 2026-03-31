package com.swp391.backend.service.monitoring;

import com.swp391.backend.dto.monitoring.shared.LecturerClassMonitoringResponse;
import com.swp391.backend.entity.GroupMember;
import com.swp391.backend.entity.LecturerAssignment;
import com.swp391.backend.entity.StudentGroup;
import com.swp391.backend.entity.User;
import com.swp391.backend.entity.monitoring.HealthStatus;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.GitCommitRepository;
import com.swp391.backend.repository.GroupMemberRepository;
import com.swp391.backend.repository.LecturerAssignmentRepository;
import com.swp391.backend.repository.StudentGroupRepository;
import com.swp391.backend.repository.TaskRepository;
import com.swp391.backend.repository.UserRepository;
import com.swp391.backend.utils.GroupHealthCalculator;
import com.swp391.backend.utils.StudentStatusCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LecturerClassMonitoringService {

    private final LecturerAssignmentRepository lecturerAssignmentRepository;
    private final StudentGroupRepository       studentGroupRepository;
    private final GroupMemberRepository        groupMemberRepository;
    private final GitCommitRepository          gitCommitRepository;
    private final TaskRepository               taskRepository;
    private final UserRepository               userRepository;

    public List<LecturerClassMonitoringResponse> getMyClassesMonitoring(
            String semesterCode,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            String keyword) {

        // 1. Lấy current lecturer
        User currentUser = getCurrentUser();

        // 2. Lấy tất cả class lecturer được assign
        List<LecturerAssignment> assignments = lecturerAssignmentRepository
                .findByLecturer_UserId(currentUser.getUserId());

        // 3. Filter + build response
        return assignments.stream()
                .filter(a -> {
                    if (semesterCode != null && !semesterCode.isBlank()) {
                        return semesterCode.equalsIgnoreCase(
                                a.getAcademicClass().getSemester().getSemesterCode());
                    }
                    return true;
                })
                .filter(a -> {
                    if (keyword != null && !keyword.isBlank()) {
                        return a.getAcademicClass().getClassCode()
                                .toLowerCase()
                                .contains(keyword.toLowerCase());
                    }
                    return true;
                })
                .map(a -> buildResponse(a, fromDate, toDate))
                .collect(Collectors.toList());
    }

    private LecturerClassMonitoringResponse buildResponse(
            LecturerAssignment assignment,
            LocalDateTime fromDate,
            LocalDateTime toDate) {

        Long classId        = assignment.getAcademicClass().getClassId();
        String classCode    = assignment.getAcademicClass().getClassCode();
        String courseCode   = assignment.getAcademicClass().getCourse().getCourseCode();
        String semesterCode = assignment.getAcademicClass().getSemester().getSemesterCode();

        // Lấy tất cả group
        List<StudentGroup> allGroups = studentGroupRepository
                .findByAcademicClass_ClassId(classId);
        long totalGroups = allGroups.size();

        // Chỉ tính trên group OPEN
        List<StudentGroup> openGroups = allGroups.stream()
                .filter(g -> "OPEN".equalsIgnoreCase(g.getStatus()))
                .collect(Collectors.toList());

        // groupsAtRisk + criticalGroups
        long groupsAtRisk   = 0;
        long criticalGroups = 0;

        for (StudentGroup g : openGroups) {
            long commits      = gitCommitRepository
                    .countCommitsByGroupAndDateRange(g.getGroupId(), fromDate, toDate);
            long overdueTasks = taskRepository
                    .countOverdueTasksByGroupId(g.getGroupId());
            HealthStatus health = GroupHealthCalculator.calculate(commits, overdueTasks);

            if (health == HealthStatus.WARNING || health == HealthStatus.CRITICAL) {
                groupsAtRisk++;
            }
            if (health == HealthStatus.CRITICAL) {
                criticalGroups++;
            }
        }

        // studentsFlagged = distinct student LOW hoặc NO_CONTRIBUTION
        Set<Long> flaggedUserIds = new HashSet<>();
        for (StudentGroup g : allGroups) {
            List<GroupMember> members = groupMemberRepository
                    .findByGroup_GroupId(g.getGroupId());
            for (GroupMember member : members) {
                Long userId = member.getUser().getUserId();
                if (flaggedUserIds.contains(userId)) continue;
                long commitCount = gitCommitRepository
                        .countCommitsByGroupAndUserAndDateRange(
                                g.getGroupId(), userId, fromDate, toDate);
                if (StudentStatusCalculator.isFlagged(commitCount)) {
                    flaggedUserIds.add(userId);
                }
            }
        }
        long studentsFlagged = flaggedUserIds.size();

        // classHealth theo rule spec
        double atRiskRatio = totalGroups > 0
                ? (double) groupsAtRisk / totalGroups : 0.0;

        String classHealth;
        if (criticalGroups >= 2 || atRiskRatio > 0.50) {
            classHealth = HealthStatus.CRITICAL.name();
        } else if (criticalGroups >= 1 || atRiskRatio >= 0.25) {
            classHealth = HealthStatus.WARNING.name();
        } else {
            classHealth = HealthStatus.HEALTHY.name();
        }

        return LecturerClassMonitoringResponse.builder()
                .classId(classId)
                .classCode(classCode)
                .courseCode(courseCode)
                .semesterCode(semesterCode)
                .totalGroups(totalGroups)
                .groupsAtRisk(groupsAtRisk)
                .studentsFlagged(studentsFlagged)
                .classHealth(classHealth)
                .lastUpdatedAt(LocalDateTime.now())
                .build();
    }

    // Lấy current user từ SecurityContext
    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        String username;
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else {
            username = principal.toString();
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found", 404));
    }
}