package com.swp391.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "MemberRole")
public class MemberRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_role_id")
    private Long memberRoleId;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    public MemberRole() {}

    public Long getMemberRoleId() {
        return memberRoleId;
    }

    public void setMemberRoleId(Long memberRoleId) {
        this.memberRoleId = memberRoleId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}