package com.swp391.backend.service;

import com.swp391.backend.entity.SyncLog;
import com.swp391.backend.entity.SyncStatus;

public interface SyncLogService {

    /**
     * Bắt đầu một phiên sync mới.
     * Ném 409 CONFLICT nếu đã có RUNNING record cho (groupId, source).
     *
     * @return SyncLog vừa tạo (status = RUNNING)
     */
    SyncLog begin(Long groupId, String source);

    /**
     * Kết thúc một phiên sync với trạng thái bất kỳ (SUCCESS / FAILED).
     */
    SyncLog end(Long syncId, SyncStatus status,
            Integer inserted, Integer updated, String message);

    /**
     * Shorthand: kết thúc với FAILED, counts = 0/0.
     */
    SyncLog fail(Long syncId, String message);

    /**
     * Shorthand: kết thúc với SUCCESS.
     */
    SyncLog success(Long syncId, String message, Integer inserted, Integer updated);
}
