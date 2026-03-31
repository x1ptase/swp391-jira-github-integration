package com.swp391.backend.repository.monitoring;

import java.time.LocalDateTime;

/**
 * Projection "flat" tổng hợp toàn bộ dữ liệu giám sát cho một StudentGroup.
 *
 * <p>Được trả về bởi
 * {@link MonitoringAggregationRepository#getGroupRawByClassId(Long, LocalDateTime, LocalDateTime)}.
 *
 * <p><b>Lưu ý về Sync Status:</b> Mỗi group có thể có tối đa 2 bản ghi sync
 * (GITHUB và JIRA). Query dùng conditional aggregation (MAX CASE WHEN) để
 * "xoay" thành một hàng duy nhất cho mỗi group.
 */
public interface GroupRawProjection {

    // ── Group & Topic ─────────────────────────────────────────────────────────

    /** ID của nhóm. */
    Long getGroupId();

    /** Tên nhóm. */
    String getGroupName();

    /**
     * Trạng thái nhóm: {@code "OPEN"} hoặc {@code "CLOSED"}.
     */
    String getGroupStatus();

    /** Tên đề tài; {@code null} nếu nhóm chưa chọn đề tài. */
    String getTopicName();

    // ── Member Stats ──────────────────────────────────────────────────────────

    /** Tổng số thành viên trong nhóm (bao gồm cả Leader). */
    Long getTotalMembers();

    /**
     * Số thành viên "active" trong khoảng [{@code fromDate}, {@code toDate}]:
     * có ít nhất 1 commit được ghi nhận trong khoảng thời gian đó.
     * Tính theo định danh thực tế (user_id → login → email → name).
     */
    Long getActiveMembers();

    // ── Task Stats ────────────────────────────────────────────────────────────

    /**
     * Số task bị quá hạn: {@code due_date} đã qua hiện tại
     * VÀ status không phải {@code DONE}.
     */
    Long getOverdueTasks();

    // ── Commit Stats ──────────────────────────────────────────────────────────

    /**
     * Tổng số commit của nhóm trong khoảng [{@code fromDate}, {@code toDate}].
     */
    Long getTotalCommits();

    /**
     * Thời điểm commit gần nhất trong khoảng thời gian;
     * {@code null} nếu nhóm chưa có commit nào.
     */
    LocalDateTime getLastCommitAt();

    // ── Sync Status – GitHub ──────────────────────────────────────────────────

    /**
     * Trạng thái của lần sync GITHUB gần nhất:
     * {@code "RUNNING"}, {@code "SUCCESS"}, hoặc {@code "FAILED"}.
     * {@code null} nếu chưa có sync nào.
     */
    String getGithubSyncStatus();

    /**
     * Thời điểm bắt đầu của lần sync GITHUB gần nhất;
     * {@code null} nếu chưa có sync nào.
     */
    LocalDateTime getGithubSyncStartedAt();

    // ── Sync Status – Jira ────────────────────────────────────────────────────

    /**
     * Trạng thái của lần sync JIRA gần nhất:
     * {@code "RUNNING"}, {@code "SUCCESS"}, hoặc {@code "FAILED"}.
     * {@code null} nếu chưa có sync nào.
     */
    String getJiraSyncStatus();

    /**
     * Thời điểm bắt đầu của lần sync JIRA gần nhất;
     * {@code null} nếu chưa có sync nào.
     */
    LocalDateTime getJiraSyncStartedAt();
}
