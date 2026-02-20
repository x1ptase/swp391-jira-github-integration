package com.swp391.backend.repository;

import com.swp391.backend.entity.GroupMember;
import com.swp391.backend.entity.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

    boolean existsByGroup_GroupIdAndUser_UserId(Long groupId, Long userId);

    boolean existsByGroup_GroupIdAndUser_UserIdAndMemberRole_Code(Long groupId, Long userId, String code);

    boolean existsByUser_UserIdAndCourseCodeAndSemester(Long userId, String courseCode, String semester);

    List<GroupMember> findByGroup_GroupId(Long groupId);

    Optional<GroupMember> findByGroup_GroupIdAndMemberRole_Code(Long groupId, String code);
}