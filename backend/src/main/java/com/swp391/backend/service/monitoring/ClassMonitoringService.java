package com.swp391.backend.service.monitoring;

import com.swp391.backend.dto.monitoring.shared.ClassMonitoringSummaryResponse;
import com.swp391.backend.entity.AcademicClass;
import com.swp391.backend.entity.GroupMember;
import com.swp391.backend.entity.StudentGroup;
import com.swp391.backend.entity.monitoring.HealthStatus;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.AcademicClassRepository;
import com.swp391.backend.repository.GitCommitRepository;
import com.swp391.backend.repository.GroupMemberRepository;
import com.swp391.backend.repository.StudentGroupRepository;
import com.swp391.backend.repository.TaskRepository;
import com.swp391.backend.utils.GroupHealthCalculator;
import com.swp391.backend.utils.StudentStatusCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClassMonitoringService {

    private final AcademicClassRepository classRepository;
    private final StudentGroupRepository  studentGroupRepository;
    private final GroupMemberRepository   groupMemberRepository;
    private final GitCommitRepository     gitCommitRepository;
    private final TaskRepository          taskRepository;

    public ClassMonitoringSummaryResponse getSummary(Long classId,
                                                     LocalDateTime fromDate,
                                                     LocalDateTime toDate) {
        // 1. Validate class tồn tại
        AcademicClass academicClass = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException(
                        "Class not found: " + classId, 404));

        // 2. Lấy tất cả group thuộc class
        List<StudentGroup> allGroups = studentGroupRepository
                .findByAcademicClass_ClassId(classId);

        // 3. totalGroups = đếm tất cả group
        long totalGroups = allGroups.size();

        // 4. atRisk = group OPEN có health WARNING hoặc CRITICAL
        long atRisk = allGroups.stream()
                .filter(g -> "OPEN".equalsIgnoreCase(g.getStatus()))
                .filter(g -> {
                    long commits      = gitCommitRepository
                            .countCommitsByGroupAndDateRange(
                                    g.getGroupId(), fromDate, toDate);
                    long overdueTasks = taskRepository
                            .countOverdueTasksByGroupId(g.getGroupId());
                    HealthStatus health = GroupHealthCalculator
                            .calculate(commits, overdueTasks);
                    return health == HealthStatus.WARNING
                            || health == HealthStatus.CRITICAL;
                })
                .count();

        // 5. studentsFlagged = distinct student LOW hoặc NO_CONTRIBUTION
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

        return ClassMonitoringSummaryResponse.builder()
                .classId(academicClass.getClassId())
                .classCode(academicClass.getClassCode())
                .totalGroups(totalGroups)
                .atRisk(atRisk)
                .studentsFlagged(studentsFlagged)
                .lastUpdatedAt(LocalDateTime.now())
                .build();
    }
}