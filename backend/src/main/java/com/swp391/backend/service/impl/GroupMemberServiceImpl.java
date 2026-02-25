package com.swp391.backend.service.impl;

import com.swp391.backend.dto.response.GroupMemberResponse;
import com.swp391.backend.dto.response.UserResponse;
import com.swp391.backend.entity.*;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.*;
import com.swp391.backend.service.GroupMemberService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GroupMemberServiceImpl implements GroupMemberService {

    private final GroupMemberRepository groupMemberRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final UserRepository userRepository;
    private final MemberRoleRepository memberRoleRepository;
    private final LecturerAssignmentRepository lecturerAssignmentRepository;

    public GroupMemberServiceImpl(
            GroupMemberRepository groupMemberRepository,
            StudentGroupRepository studentGroupRepository,
            UserRepository userRepository,
            MemberRoleRepository memberRoleRepository,
            LecturerAssignmentRepository lecturerAssignmentRepository
    ) {
        this.groupMemberRepository = groupMemberRepository;
        this.studentGroupRepository = studentGroupRepository;
        this.userRepository = userRepository;
        this.memberRoleRepository = memberRoleRepository;
        this.lecturerAssignmentRepository = lecturerAssignmentRepository;
    }

    @Override
    public void addMember(Long groupId, Long studentId) {
        User actor = currentUser();
        requireCanManageGroup(actor, groupId);

        StudentGroup group = requireGroup(groupId);
        User student = requireUser(studentId);
        requireStudentRole(student);

        // already in this group?
        if (groupMemberRepository.existsByGroup_GroupIdAndUser_UserId(groupId, studentId)) {
            throw new BusinessException("Student already in this group.", 409);
        }

        // BR-01: 1 group per (course_code + semester)
        if (groupMemberRepository.existsByUser_UserIdAndCourseCodeAndSemester(
                studentId, group.getCourseCode(), group.getSemester()
        )) {
            throw new BusinessException("BR-01 violated: This student already belongs to a group in "
                    + group.getCourseCode() + " / " + group.getSemester(), 409);
        }

        MemberRole memberRole = requireMemberRole("MEMBER");

        GroupMember gm = new GroupMember();
        gm.setId(new GroupMemberId(groupId, studentId));
        gm.setGroup(group);
        gm.setUser(student);
        gm.setMemberRole(memberRole);
        gm.setCourseCode(group.getCourseCode());
        gm.setSemester(group.getSemester());

        groupMemberRepository.save(gm);
    }

    @Override
    public void removeMember(Long groupId, Long studentId) {
        User actor = currentUser();
        requireCanManageGroup(actor, groupId);

        GroupMemberId id = new GroupMemberId(groupId, studentId);
        Optional<GroupMember> gmOpt = groupMemberRepository.findById(id);
        if (!gmOpt.isPresent()) {
            throw new BusinessException("Member not found in group.", 404);
        }
        groupMemberRepository.delete(gmOpt.get());
    }

    @Transactional
    @Override
    public void setLeader(Long groupId, Long studentId) {
        User actor = currentUser();
        requireCanManageGroup(actor, groupId);

        // group exists
        requireGroup(groupId);

        GroupMemberId id = new GroupMemberId(groupId, studentId);
        Optional<GroupMember> targetOpt = groupMemberRepository.findById(id);
        if (!targetOpt.isPresent()) {
            throw new BusinessException("Student is not a member of this group.", 400);
        }

        MemberRole leaderRole = requireMemberRole("LEADER");
        MemberRole memberRole = requireMemberRole("MEMBER");

        // current leader?
        Optional<GroupMember> currentLeaderOpt =
                groupMemberRepository.findByGroup_GroupIdAndMemberRole_Code(groupId, "LEADER");

        if (currentLeaderOpt.isPresent()) {
            GroupMember currentLeader = currentLeaderOpt.get();
            if (!currentLeader.getUser().getUserId().equals(studentId)) {
                currentLeader.setMemberRole(memberRole);
                groupMemberRepository.save(currentLeader);
                groupMemberRepository.flush();
            }
        }

        GroupMember target = targetOpt.get();
        target.setMemberRole(leaderRole);
        groupMemberRepository.save(target);
    }

    @Override
    public List<GroupMemberResponse> listMembers(Long groupId) {
        User actor = currentUser();
        requireCanManageGroup(actor, groupId);

        List<GroupMember> members = groupMemberRepository.findByGroup_GroupId(groupId);
        List<GroupMemberResponse> result = new ArrayList<GroupMemberResponse>();

        for (int i = 0; i < members.size(); i++) {
            GroupMember gm = members.get(i);
            GroupMemberResponse r = new GroupMemberResponse();
            r.setUserId(gm.getUser().getUserId());
            r.setUsername(gm.getUser().getUsername());
            r.setFullName(gm.getUser().getFullName());
            r.setEmail(gm.getUser().getEmail());
            r.setMemberRole(gm.getMemberRole() == null ? null : gm.getMemberRole().getCode());
            result.add(r);
        }

        return result;
    }

    @Override
    public Page<UserResponse> searchEligibleStudents(Long groupId, String keyword, Pageable pageable) {
        if (keyword == null) {
            keyword = "";
        }
        keyword = keyword.trim();

        User actor = currentUser();
        requireCanManageGroup(actor, groupId);

        StudentGroup group = requireGroup(groupId);

        Page<User> page = userRepository.searchEligibleStudentsForGroup(
                keyword,
                groupId,
                group.getCourseCode(),
                group.getSemester(),
                pageable
        );

        List<User> users = page.getContent();
        List<UserResponse> dtoList = new ArrayList<UserResponse>();
        for (int i = 0; i < users.size(); i++) {
            dtoList.add(toUserResponse(users.get(i)));
        }

        return new PageImpl<UserResponse>(dtoList, pageable, page.getTotalElements());
    }

    private UserResponse toUserResponse(User u) {
        UserResponse r = new UserResponse();
        r.setUserId(u.getUserId());
        r.setUsername(u.getUsername());
        r.setFullName(u.getFullName());
        r.setEmail(u.getEmail());
        r.setGithubUsername(u.getGithubUsername());
        r.setJiraAccountId(u.getJiraAccountId());
        if (u.getRole() != null) r.setRoleCode(u.getRole().getRoleCode());
        r.setCreatedAt(u.getCreatedAt());
        return r;
    }

    // ===== helpers =====

    private StudentGroup requireGroup(Long groupId) {
        Optional<StudentGroup> gOpt = studentGroupRepository.findById(groupId);
        if (!gOpt.isPresent()) throw new BusinessException("Group not found: " + groupId, 404);
        return gOpt.get();
    }

    private User requireUser(Long userId) {
        Optional<User> uOpt = userRepository.findById(userId);
        if (!uOpt.isPresent()) throw new BusinessException("User not found: " + userId, 404);
        return uOpt.get();
    }

    private MemberRole requireMemberRole(String code) {
        Optional<MemberRole> mrOpt = memberRoleRepository.findByCode(code);
        if (!mrOpt.isPresent()) throw new BusinessException("Missing MemberRole in DB: " + code, 500);
        return mrOpt.get();
    }

    private void requireStudentRole(User u) {
        if (u.getRole() == null || u.getRole().getRoleCode() == null) {
            throw new BusinessException("User role not found.", 400);
        }
        if (!"STUDENT".equalsIgnoreCase(u.getRole().getRoleCode())) {
            throw new BusinessException("Only STUDENT can be added to group.", 400);
        }
    }

    private void requireCanManageGroup(User actor, Long groupId) {
        if (actor == null) throw new BusinessException("Unauthorized", 401);

        String roleCode = actor.getRole() == null ? null : actor.getRole().getRoleCode();
        if (roleCode == null) throw new BusinessException("Unauthorized", 401);

        if ("ADMIN".equalsIgnoreCase(roleCode)) {
            return; // admin ok
        }

        if ("LECTURER".equalsIgnoreCase(roleCode)) {
            boolean assigned = lecturerAssignmentRepository.existsByGroupIdAndLecturerId(groupId, actor.getUserId());
            if (!assigned) {
                throw new BusinessException("Lecturer is not assigned to this group.", 403);
            }
            return;
        }

        throw new BusinessException("Access denied.", 403);
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        String username = auth.getName();

        Optional<User> uOpt = userRepository.findByUsername(username);
        if (!uOpt.isPresent()) return null;
        return uOpt.get();
    }
}