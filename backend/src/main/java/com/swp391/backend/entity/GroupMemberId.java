package com.swp391.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class GroupMemberId implements Serializable {

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "user_id")
    private Long userId;

    public GroupMemberId() {}

    public GroupMemberId(Long groupId, Long userId) {
        this.groupId = groupId;
        this.userId = userId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof GroupMemberId)) return false;

        GroupMemberId that = (GroupMemberId) o;

        if (groupId == null) {
            if (that.groupId != null) return false;
        } else if (!groupId.equals(that.groupId)) return false;

        if (userId == null) {
            return that.userId == null;
        } else return userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (groupId == null ? 0 : groupId.hashCode());
        result = 31 * result + (userId == null ? 0 : userId.hashCode());
        return result;
    }
}