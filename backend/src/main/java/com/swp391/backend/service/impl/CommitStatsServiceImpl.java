package com.swp391.backend.service.impl;

import com.swp391.backend.dto.response.CommitStatsDTO;
import com.swp391.backend.dto.response.CommitStatsProjection;
import com.swp391.backend.repository.GitCommitRepository;
import com.swp391.backend.service.CommitStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Triển khai {@link CommitStatsService}.
 *
 * <p>
 * <b>Phân quyền</b> được thực hiện tại tầng Controller bằng
 * {@code @PreAuthorize("@securityService.hasAccessToGroup(#groupId)")},
 * bao gồm:
 * </p>
 * <ul>
 * <li><b>ADMIN</b> → luôn được phép</li>
 * <li><b>LECTURER</b> → chỉ group được phân công (bảng
 * {@code LecturerAssignment})</li>
 * <li><b>STUDENT (LEADER / MEMBER)</b> → chỉ group mình tham gia (bảng
 * {@code GroupMember})</li>
 * <li>Các trường hợp khác → Spring Security ném {@code AccessDeniedException} →
 * 403</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommitStatsServiceImpl implements CommitStatsService {

    private final GitCommitRepository gitCommitRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CommitStatsDTO> getCommitStats(Long groupId) {
        log.debug("[CommitStats] Fetching stats for group={}", groupId);

        List<CommitStatsProjection> projections = gitCommitRepository.getCommitStatsByGroup(groupId);

        if (projections.isEmpty()) {
            log.debug("[CommitStats] No commits found for group={}", groupId);
            return List.of(); // Empty State rõ ràng
        }

        List<CommitStatsDTO> result = projections.stream()
                .map(CommitStatsDTO::from)
                .toList();

        log.debug("[CommitStats] group={} → {} author(s)", groupId, result.size());
        return result;
    }
}
