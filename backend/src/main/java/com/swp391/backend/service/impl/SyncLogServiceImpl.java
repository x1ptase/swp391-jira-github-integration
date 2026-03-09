package com.swp391.backend.service.impl;

import com.swp391.backend.entity.SyncLog;
import com.swp391.backend.entity.SyncStatus;
import com.swp391.backend.repository.SyncLogRepository;
import com.swp391.backend.service.SyncLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * Quản lý vòng đời đồng bộ (Sync Lifecycle) dựa trên bảng SyncLog.
 *
 * <p>
 * Mọi method đều chạy trong transaction RIÊNG BIỆT (REQUIRES_NEW) để
 * đảm bảo trạng thái RUNNING / SUCCESS / FAILED được commit ngay lập tức
 * vào DB, độc lập với transaction nghiệp vụ bên ngoài.
 * Kể cả khi nghiệp vụ thất bại, SyncLog vẫn được cập nhật.
 * </p>
 *
 * <p>
 * <b>Chiến lược bảo vệ đồng thời (Concurrency Guard):</b>
 * </p>
 * <ol>
 * <li>Lớp 1 – Application: Query kiểm tra RUNNING trước khi insert.</li>
 * <li>Lớp 2 – Database: Unique Filtered Index
 * {@code UX_SyncLog_OneRunningPerSource}
 * trên {@code (group_id, source) WHERE status = 'RUNNING'} đảm bảo tính
 * an toàn tuyệt đối ngay cả khi nhiều request đến đồng thời.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncLogServiceImpl implements SyncLogService {

    private final SyncLogRepository syncLogRepository;

    // -------------------------------------------------------------------------
    // begin
    // -------------------------------------------------------------------------

    /**
     * Bắt đầu một phiên sync mới.
     *
     * <ul>
     * <li>Nếu đã có bản ghi RUNNING → ném {@code 409 CONFLICT}.</li>
     * <li>Nếu không → tạo bản ghi mới với status = RUNNING và trả về.</li>
     * </ul>
     *
     * @param groupId ID nhóm sinh viên
     * @param source  "JIRA" hoặc "GITHUB"
     * @return {@link SyncLog} vừa được lưu với status = RUNNING
     * @throws ResponseStatusException 409 nếu đang có sync chạy dở
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncLog begin(Long groupId, String source) {
        // Lớp 1: kiểm tra ở application level
        syncLogRepository.findByGroupIdAndSourceAndStatus(groupId, source, SyncStatus.RUNNING)
                .ifPresent(existing -> {
                    log.warn("[SyncLog] begin() aborted – already RUNNING: syncId={}, group={}, source={}",
                            existing.getId(), groupId, source);
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Sync is already running for group " + groupId + " / " + source);
                });

        SyncLog syncLog = SyncLog.builder()
                .groupId(groupId)
                .source(source)
                .status(SyncStatus.RUNNING)
                // insertedCount và updatedCount mặc định = 0 qua @Builder.Default trong entity
                .build();

        try {
            // Lớp 2: DB Unique Index sẽ bắt race condition còn sót lại
            SyncLog saved = syncLogRepository.saveAndFlush(syncLog);
            log.info("[SyncLog] begin() → syncId={}, group={}, source={}", saved.getId(), groupId, source);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            // Race condition: hai luồng vượt qua lớp 1 cùng lúc → DB unique index chặn lại
            log.warn("[SyncLog] begin() DB unique index conflict – group={}, source={}", groupId, source);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Sync is already running for group " + groupId + " / " + source, ex);
        }
    }

    // -------------------------------------------------------------------------
    // end
    // -------------------------------------------------------------------------

    /**
     * Kết thúc một phiên sync.
     *
     * @param syncId   ID của bản ghi SyncLog cần cập nhật
     * @param status   {@link SyncStatus#SUCCESS} hoặc {@link SyncStatus#FAILED}
     * @param inserted số bản ghi mới được chèn (null sẽ được coi là 0)
     * @param updated  số bản ghi được cập nhật (null sẽ được coi là 0)
     * @param message  nội dung ghi chú; bắt buộc khi FAILED, tùy chọn khi SUCCESS
     * @return {@link SyncLog} sau khi cập nhật
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncLog end(Long syncId, SyncStatus status,
            Integer inserted, Integer updated, String message) {
        if (status == SyncStatus.RUNNING) {
            throw new IllegalArgumentException("end() không được dùng với status RUNNING");
        }

        SyncLog syncLog = syncLogRepository.findById(syncId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "SyncLog not found: id=" + syncId));

        syncLog.setStatus(status);
        syncLog.setEndedAt(LocalDateTime.now());
        syncLog.setInsertedCount(inserted != null ? inserted : 0);
        syncLog.setUpdatedCount(updated != null ? updated : 0);
        syncLog.setDetailMessage(message);

        SyncLog saved = syncLogRepository.save(syncLog);
        log.info("[SyncLog] end() → syncId={}, status={}, inserted={}, updated={}",
                syncId, status, saved.getInsertedCount(), saved.getUpdatedCount());
        return saved;
    }

    // -------------------------------------------------------------------------
    // Shorthand helpers
    // -------------------------------------------------------------------------

    /**
     * Kết thúc phiên sync với trạng thái FAILED.
     * Các count được giữ nguyên giá trị tại thời điểm gọi nếu cần truyền vào,
     * hoặc mặc định là 0 nếu chưa có gì được xử lý.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncLog fail(Long syncId, String message) {
        log.error("[SyncLog] fail() → syncId={}, reason={}", syncId, message);
        return end(syncId, SyncStatus.FAILED, 0, 0, message);
    }

    /**
     * Kết thúc phiên sync với trạng thái SUCCESS.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncLog success(Long syncId, String message, Integer inserted, Integer updated) {
        return end(syncId, SyncStatus.SUCCESS, inserted, updated, message);
    }
}
