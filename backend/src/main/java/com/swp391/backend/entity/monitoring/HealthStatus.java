package com.swp391.backend.entity.monitoring;

/**
 * Trạng thái sức khoẻ tổng hợp của một nhóm hoặc lớp học.
 * <p>
 * Đây là giá trị <b>tính toán động</b> – không được lưu vào database.
 * {@code StudentGroup.status} và {@code AcademicClass.status} vẫn là
 * trạng thái vận hành (OPEN/CLOSED) và không bị ảnh hưởng.
 */
public enum HealthStatus {

    /**
     * Nhóm/lớp đang hoạt động bình thường, đủ commit, không có vấn đề đáng lo ngại.
     */
    HEALTHY,

    /**
     * Nhóm/lớp có dấu hiệu cần chú ý nhưng chưa nghiêm trọng.
     * Ví dụ: commit thấp, phân bổ không đều, một vài task trễ hạn.
     */
    WARNING,

    /**
     * Nhóm/lớp ở mức rủi ro cao.
     * Ví dụ: không có commit, quá nhiều task trễ, không có thành viên hoạt động.
     */
    CRITICAL
}
