package com.swp391.backend.repository;

import com.swp391.backend.entity.GroupMember;
import com.swp391.backend.entity.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

    boolean existsByGroup_GroupIdAndUser_UserId(Long groupId, Long userId);

    boolean existsByGroup_GroupIdAndUser_UserIdAndMemberRole_Code(Long groupId, Long userId, String code);

    boolean existsByUser_UserIdAndGroup_AcademicClass_ClassId(Long userId, Long classId);

    List<GroupMember> findByGroup_GroupId(Long groupId);

    Optional<GroupMember> findByGroup_GroupIdAndMemberRole_Code(Long groupId, String code);

    Optional<GroupMember> findByUser_UserId(Long userId);

    @Query("""
        SELECT COUNT(sca)
        FROM StudentClassAssignment sca
        WHERE sca.academicClass.classId = :classId
          AND NOT EXISTS (
              SELECT 1
              FROM GroupMember gm
              WHERE gm.user.userId = sca.student.userId
                AND gm.group.academicClass.classId = :classId
          )
    """)
    long countStudentsWithoutGroupByClassId(@Param("classId") Long classId);
}