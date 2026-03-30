package com.swp391.backend.dto.monitoring.response;

import com.swp391.backend.entity.monitoring.HealthStatus;
import com.swp391.backend.entity.monitoring.PrimaryReason;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Response DTO trả về dữ liệu giám sát tổng hợp của một nhóm sinh viên.
 *
 * <p>Được tạo bởi {@link com.swp391.backend.service.monitoring.GroupMonitoringService}
 * và dùng trực tiếp trong API response của endpoint giám sát theo lớp.
 *
 * <h4>Quy tắc xếp loại (healthStatus):</h4>
 * <ul>
 *   <li>{@code CLOSED} – Nhóm đã đóng ({@code StudentGroup.status = 'CLOSED'}).</li>
 *   <li>{@code CRITICAL} – Một trong các điều kiện: không có commit, tỉ lệ active &lt; 25%,
 *       overdue &ge; 5, lastActivity &gt; 7 ngày, hoặc sync gần nhất bị FAILED.</li>
 *   <li>{@code WARNING} – Không CRITICAL nhưng: overdue trong [2,4], tỉ lệ active &lt; 50%,
 *       hoặc chưa có topic.</li>
 *   <li>{@code HEALTHY} – Không thuộc các trường hợp trên.</li>
 * </ul>
 */
@Getter
@Builder
public class GroupMonitoringDTO {

    // ── Thông tin nhóm ────────────────────────────────────────────────────────

    /** ID nhóm. */
    private final Long groupId;

    /** Tên nhóm. */
    private final String groupName;

    /**
     * Trạng thái vận hành từ DB: {@code "OPEN"} hoặc {@code "CLOSED"}.
     */
    private final String groupStatus;

    /** Tên đề tài đã chọn; {@code null} nếu nhóm chưa gán topic. */
    private final String topicName;

    // ── Thống kê thành viên ───────────────────────────────────────────────────

    /** Tổng số thành viên trong nhóm. */
    private final int totalMembers;

    /** Số thành viên có ít nhất 1 commit trong khoảng thời gian monitoring. */
    private final int activeMembers;

    /**
     * Chuỗi hiển thị dạng {@code "active/total active"}.
     * Ví dụ: {@code "3/5 active"}.
     */
    private final String membersText;

    // ── Thống kê task ─────────────────────────────────────────────────────────

    /**
     * Số task bị quá hạn: {@code due_date} đã qua hiện tại
     * và status không phải {@code DONE}.
     */
    private final int overdueTasks;

    // ── Thống kê commit ───────────────────────────────────────────────────────

    /** Tổng số commit của nhóm trong khoảng thời gian. */
    private final long totalCommits;

    /**
     * Thời điểm commit gần nhất; {@code null} nếu nhóm chưa có commit nào.
     */
    private final LocalDateTime lastActivityAt;

    // ── Sync status ───────────────────────────────────────────────────────────

    /**
     * Trạng thái sync GitHub gần nhất: {@code "RUNNING"}, {@code "SUCCESS"},
     * {@code "FAILED"}, hoặc {@code null} nếu chưa sync.
     */
    private final String githubSyncStatus;

    /**
     * Thời điểm bắt đầu sync GitHub gần nhất;
     * {@code null} nếu chưa có sync nào.
     */
    private final LocalDateTime githubSyncStartedAt;

    /**
     * Trạng thái sync Jira gần nhất: {@code "RUNNING"}, {@code "SUCCESS"},
     * {@code "FAILED"}, hoặc {@code null} nếu chưa sync.
     */
    private final String jiraSyncStatus;

    /**
     * Thời điểm bắt đầu sync Jira gần nhất;
     * {@code null} nếu chưa có sync nào.
     */
    private final LocalDateTime jiraSyncStartedAt;

    // ── Health classification ─────────────────────────────────────────────────

    /**
     * Xếp loại sức khoẻ tổng hợp của nhóm.
     * Một trong: {@code CLOSED}, {@code CRITICAL}, {@code WARNING}, {@code HEALTHY}.
     */
    private final HealthStatus healthStatus;

    /**
     * Lý do chính duy nhất (theo thứ tự ưu tiên giảm dần).
     * {@code STABLE} nếu nhóm khoẻ mạnh.
     */
    private final PrimaryReason primaryReason;
}
