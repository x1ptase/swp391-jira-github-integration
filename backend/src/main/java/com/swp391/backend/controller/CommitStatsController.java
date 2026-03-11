package com.swp391.backend.controller;

import com.swp391.backend.common.ApiResponse;
import com.swp391.backend.dto.response.CommitStatsDTO;
import com.swp391.backend.service.CommitStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoint thống kê commit GitHub theo nhóm.
 *
 * <h3>Phân quyền (áp dụng tại mỗi endpoint)</h3>
 * <p>
 * Dùng {@code @PreAuthorize("@securityService.hasAccessToGroup(#groupId)")}
 * để Spring Security tự kiểm tra trước khi vào method:
 * </p>
 * <ul>
 * <li><b>ADMIN</b> → luôn được phép</li>
 * <li><b>LECTURER</b> → chỉ group mình phụ trách
 * ({@code LecturerAssignment})</li>
 * <li><b>STUDENT / LEADER / MEMBER</b> → chỉ group mình tham gia
 * ({@code GroupMember})</li>
 * <li>Còn lại → 403 Forbidden (bắt bởi {@code GlobalExceptionHandler})</li>
 * </ul>
 */
@Tag(name = "Commit Statistics", description = "Thống kê commit GitHub theo nhóm")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class CommitStatsController {

    private final CommitStatsService commitStatsService;

    /**
     * Lấy thống kê commit của tất cả thành viên trong nhóm.
     *
     * <p>
     * Mỗi phần tử trong danh sách kết quả đại diện cho một tác giả duy nhất.
     * Nếu nhóm chưa có dữ liệu commit (chưa sync), trả về danh sách rỗng {@code []}
     * với HTTP 200 (không phải 404) để frontend hiển thị empty state.
     * </p>
     *
     * @param groupId ID nhóm sinh viên
     * @return danh sách thống kê commit, sắp xếp giảm dần theo số commit
     */
    @Operation(summary = "Lấy thống kê commit theo nhóm", description = """
            Trả về thống kê commit của từng tác giả trong nhóm.
            Grouping priority: authorUserId (nếu đã map) → email → name.
            Trả về [] nếu nhóm chưa có dữ liệu commit.
            """)
    @GetMapping("/commits/{groupId}")
    @PreAuthorize("@securityService.hasAccessToGroup(#groupId)")
    public ResponseEntity<ApiResponse<List<CommitStatsDTO>>> getCommitStats(
            @Parameter(description = "ID nhóm sinh viên") @PathVariable Long groupId) {

        List<CommitStatsDTO> stats = commitStatsService.getCommitStats(groupId);

        if (stats.isEmpty()) {
            return ResponseEntity.ok(
                    ApiResponse.<List<CommitStatsDTO>>builder()
                            .status(200)
                            .message("No commits found for this group yet. Please sync GitHub data first.")
                            .data(List.of())
                            .build());
        }

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
