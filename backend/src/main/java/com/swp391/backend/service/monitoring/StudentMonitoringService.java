package com.swp391.backend.service.monitoring;

import com.swp391.backend.dto.monitoring.request.MonitoringFilterRequest;
import com.swp391.backend.dto.monitoring.response.StudentWatchlistDTO;
import com.swp391.backend.dto.monitoring.shared.MonitoringDateRange;
import com.swp391.backend.entity.monitoring.ContributionStatus;
import com.swp391.backend.repository.monitoring.MonitoringAggregationRepository;
import com.swp391.backend.repository.monitoring.StudentWatchlistProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentMonitoringService {

    private final MonitoringAggregationRepository aggregationRepository;
    private final MonitoringDateRangeService dateRangeService;

    // Ngưỡng commit để phân loại ACTIVE (MVP rule: >= 2 commits)
    private static final int ACTIVE_COMMIT_THRESHOLD = 2;

    // ────────────────────────────────────────────────────────────────────────
    // Public API
    // ────────────────────────────────────────────────────────────────────────
    public List<StudentWatchlistDTO> getWatchlistByClass(Long classId, MonitoringFilterRequest filter) {
        MonitoringDateRange range = dateRangeService.resolve(filter);
        return getWatchlistByClassAndRange(classId, range, filter);
    }
    public List<StudentWatchlistDTO> getWatchlistByClass(Long classId, MonitoringDateRange dateRange,
                                                          MonitoringFilterRequest filter) {
        return getWatchlistByClassAndRange(classId, dateRange, filter);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Core logic
    // ────────────────────────────────────────────────────────────────────────

    private List<StudentWatchlistDTO> getWatchlistByClassAndRange(
            Long classId, MonitoringDateRange range, MonitoringFilterRequest filter) {

        // ── 1. Single-shot query ───────────────────────────────────────────────
        List<StudentWatchlistProjection> raws = aggregationRepository
                .getStudentWatchlistByClassId(classId, range.getFrom(), range.getTo());

        // ── 2. Map → DTO (tính ContributionStatus) ────────────────────────────
        Stream<StudentWatchlistDTO> stream = raws.stream().map(this::toDto);

        // ── 3. Filter: theo groupId ────────────────────────────────────────────
        if (filter != null && filter.getGroupId() != null) {
            Long targetGroupId = filter.getGroupId();
            stream = stream.filter(dto -> targetGroupId.equals(dto.getGroupId()));
        }

        // ── 4. Filter: theo contributionStatus ────────────────────────────────
        if (filter != null && filter.getStatus() != null && !filter.getStatus().isBlank()) {
            ContributionStatus targetStatus = parseContributionStatus(filter.getStatus());
            stream = stream.filter(dto -> dto.getContributionStatus() == targetStatus);
        }

        // ── 5. Filter: theo keyword (fullName hoặc studentCode) ───────────────
        if (filter != null && filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            String kw = filter.getKeyword().trim().toLowerCase();
            stream = stream.filter(dto ->
                    (dto.getFullName() != null && dto.getFullName().toLowerCase().contains(kw))
                    || (dto.getStudentCode() != null && dto.getStudentCode().toLowerCase().contains(kw)));
        }

        // ── 6. Sort: NO_CONTRIBUTION → LOW → ACTIVE ───────────────────────────
        stream = stream.sorted(this::compareByContribution);

        // ── 7. Phân trang ─────────────────────────────────────────────────────
        if (filter != null) {
            int page = Math.max(0, filter.getPage());
            int size = filter.getSize() > 0 ? filter.getSize() : 20;
            stream = stream.skip((long) page * size).limit(size);
        }

        return stream.collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────────────
    // Mapping
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Map một {@link StudentWatchlistProjection} sang {@link StudentWatchlistDTO}.
     */
    private StudentWatchlistDTO toDto(StudentWatchlistProjection raw) {
        long commitCount = raw.getCommitCount() != null ? raw.getCommitCount() : 0L;
        ContributionStatus status = classifyContribution(commitCount);

        return StudentWatchlistDTO.builder()
                .userId(raw.getUserId())
                .fullName(raw.getFullName())
                .studentCode(raw.getStudentCode())
                .email(raw.getEmail())
                .groupId(raw.getGroupId())
                .groupName(raw.getGroupName())
                .memberRole(raw.getMemberRole())
                .commitCount(commitCount)
                .lastActiveAt(raw.getLastActiveAt())
                .contributionStatus(status)
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Contribution classification
    // ────────────────────────────────────────────────────────────────────────
    private ContributionStatus classifyContribution(long commitCount) {
        if (commitCount >= ACTIVE_COMMIT_THRESHOLD) return ContributionStatus.ACTIVE;
        if (commitCount == 1) return ContributionStatus.LOW;
        return ContributionStatus.NO_CONTRIBUTION;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Sorting
    // ────────────────────────────────────────────────────────────────────────
    private int compareByContribution(StudentWatchlistDTO a, StudentWatchlistDTO b) {
        return Integer.compare(
                contributionOrder(a.getContributionStatus()),
                contributionOrder(b.getContributionStatus()));
    }

    private int contributionOrder(ContributionStatus status) {
        if (status == null) return 99;
        return switch (status) {
            case NO_CONTRIBUTION -> 0;
            case LOW             -> 1;
            case ACTIVE          -> 2;
        };
    }

    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────
    private ContributionStatus parseContributionStatus(String status) {
        try {
            return ContributionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Giá trị status không hợp lệ: '" + status
                    + "'. Chấp nhận: ACTIVE, LOW, NO_CONTRIBUTION");
        }
    }
}
