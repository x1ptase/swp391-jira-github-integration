package com.swp391.backend.repository.monitoring;

import java.time.LocalDateTime;

/**
 * Projection "flat" tổng hợp dữ liệu Watchlist cho từng sinh viên trong một lớp học.
 *
 * <p>Được trả về bởi {@link MonitoringAggregationRepository#getStudentWatchlistByClassId}.
 *
 * <h4>Lưu ý về commit source:</h4>
 * <p>Commits được match theo {@code author_user_id} (đã được map với userId nội bộ
 * trong quá trình sync GitHub). Sinh viên chưa map GitHub email vào tài khoản hệ thống
 * sẽ vẫn xuất hiện nhưng {@code commitCount = 0}.
 *
 * <h4>Lưu ý về GroupMember:</h4>
 * <p>Một sinh viên có thể chưa thuộc nhóm nào — khi đó {@code groupId},
 * {@code groupName}, {@code memberRole} sẽ là {@code null}.
 */
public interface StudentWatchlistProjection {

    // ── Thông tin sinh viên ─────────────────────────────────────────────────

    /** ID người dùng (user_id từ bảng Users). */
    Long getUserId();

    /** Họ và tên đầy đủ. */
    String getFullName();

    /** Mã số sinh viên (student_code). */
    String getStudentCode();

    /** Email sinh viên. */
    String getEmail();

    // ── Thông tin nhóm ──────────────────────────────────────────────────────

    /**
     * ID nhóm sinh viên đang tham gia.
     * {@code null} nếu sinh viên chưa được phân vào nhóm nào.
     */
    Long getGroupId();

    /**
     * Tên nhóm sinh viên đang tham gia.
     * {@code null} nếu sinh viên chưa được phân vào nhóm nào.
     */
    String getGroupName();

    /**
     * Vai trò trong nhóm: {@code "LEADER"} hoặc {@code "MEMBER"}.
     * {@code null} nếu sinh viên chưa được phân vào nhóm nào.
     */
    String getMemberRole();

    // ── Thống kê commit ─────────────────────────────────────────────────────

    /**
     * Số commit của sinh viên trong khoảng [{@code fromDate}, {@code toDate}].
     * <p>Giá trị này luôn &ge; 0 (không bao giờ null), kể cả với sinh viên chưa commit.
     */
    Long getCommitCount();

    /**
     * Thời điểm commit gần nhất của sinh viên trong khoảng thời gian.
     * {@code null} nếu sinh viên chưa có commit nào.
     */
    LocalDateTime getLastActiveAt();
}
