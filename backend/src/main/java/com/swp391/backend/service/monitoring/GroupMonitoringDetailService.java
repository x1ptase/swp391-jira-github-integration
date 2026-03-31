package com.swp391.backend.service.monitoring;

import com.swp391.backend.dto.monitoring.shared.GroupMonitoringDetailResponse;
import com.swp391.backend.dto.monitoring.shared.GroupMonitoringDetailResponse.MemberDTO;
import com.swp391.backend.dto.monitoring.shared.GroupMonitoringDetailResponse.SummaryDTO;
import com.swp391.backend.entity.GroupMember;
import com.swp391.backend.entity.StudentGroup;
import com.swp391.backend.entity.monitoring.HealthStatus;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.GitCommitRepository;
import com.swp391.backend.repository.GroupMemberRepository;
import com.swp391.backend.repository.StudentGroupRepository;
import com.swp391.backend.repository.TaskRepository;
import com.swp391.backend.utils.GroupHealthCalculator;
import com.swp391.backend.utils.StudentStatusCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupMonitoringDetailService {

    private final StudentGroupRepository  studentGroupRepository;
    private final GroupMemberRepository   groupMemberRepository;
    private final GitCommitRepository     gitCommitRepository;
    private final TaskRepository          taskRepository;

    public GroupMonitoringDetailResponse getDetail(Long groupId,
                                                   LocalDateTime fromDate,
                                                   LocalDateTime toDate) {
        // 1. Validate group tồn tại
        StudentGroup group = studentGroupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(
                        "Group not found: " + groupId, 404));

        // 2. Lấy thông tin header
        String groupName         = group.getGroupName();
        Long classId             = group.getAcademicClass().getClassId();
        String classCode         = group.getAcademicClass().getClassCode();
        String topicName         = group.getTopic() != null
                ? group.getTopic().getTopicName() : null;
        String operationalStatus = group.getStatus();

        // 3. Lấy members
        List<GroupMember> members = groupMemberRepository
                .findByGroup_GroupId(groupId);

        // 4. Tính summary
        long totalCommits    = gitCommitRepository
                .countCommitsByGroupAndDateRange(groupId, fromDate, toDate);
        long overdueTasks    = taskRepository
                .countOverdueTasksByGroupId(groupId);
        long totalMembers    = members.size();
        LocalDateTime lastActivityAt = gitCommitRepository
                .findLatestCommitDateByGroup(groupId, fromDate, toDate);

        // 5. Build members + đếm activeMembers
        long activeMembers = 0;
        List<MemberDTO> memberDTOs = new ArrayList<>();

        for (GroupMember gm : members) {
            Long userId = gm.getUser().getUserId();

            long commitCount = gitCommitRepository
                    .countCommitsByGroupAndUserAndDateRange(
                            groupId, userId, fromDate, toDate);

            LocalDateTime lastActiveAt = gitCommitRepository
                    .findLatestCommitDateByGroupAndUser(
                            groupId, userId, fromDate, toDate);

            String contributionStatus = StudentStatusCalculator
                    .calculate(commitCount).name();

            if (commitCount >= 1) activeMembers++;

            memberDTOs.add(MemberDTO.builder()
                    .userId(userId)
                    .fullName(gm.getUser().getFullName())
                    .role(gm.getMemberRole().getCode())
                    .commitCount(commitCount)
                    .lastActiveAt(lastActiveAt)
                    .contributionStatus(contributionStatus)
                    .build());
        }

        // 6. Tính health + reasons
        HealthStatus health = GroupHealthCalculator
                .calculate(totalCommits, overdueTasks);
        List<String> reasons = GroupHealthCalculator
                .reasons(totalCommits, overdueTasks, lastActivityAt);

        return GroupMonitoringDetailResponse.builder()
                .groupId(groupId)
                .groupName(groupName)
                .classId(classId)
                .classCode(classCode)
                .topicName(topicName)
                .operationalStatus(operationalStatus)
                .health(health.name())
                .summary(SummaryDTO.builder()
                        .commits(totalCommits)
                        .activeMembers(activeMembers)
                        .totalMembers(totalMembers)
                        .overdueTasks(overdueTasks)
                        .lastActivityAt(lastActivityAt)
                        .build())
                .reasons(reasons)
                .members(memberDTOs)
                .build();
    }
}