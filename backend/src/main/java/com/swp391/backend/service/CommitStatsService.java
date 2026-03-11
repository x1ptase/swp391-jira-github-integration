package com.swp391.backend.service;

import com.swp391.backend.dto.response.CommitStatsDTO;

import java.util.List;

public interface CommitStatsService {

    /**
     * Lấy thống kê commit theo groupId.
     *
     * <p>
     * <b>Phân quyền:</b>
     * <ul>
     * <li>ADMIN → xem bất kỳ group nào</li>
     * <li>LECTURER → chỉ xem group mình phụ trách</li>
     * <li>LEADER / MEMBER → chỉ xem group mình tham gia</li>
     * <li>Khác → ném 403 Forbidden</li>
     * </ul>
     *
     * @param groupId ID nhóm sinh viên
     * @return danh sách thống kê commit theo tác giả; rỗng nếu chưa có dữ liệu
     */
    List<CommitStatsDTO> getCommitStats(Long groupId);
}
