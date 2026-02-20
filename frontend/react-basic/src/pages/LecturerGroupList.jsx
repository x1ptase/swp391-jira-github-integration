import { useEffect, useState } from "react";
import "./LecturerGroupList.css";

const API_URL = "/api/lecturer/groups";
const STUDENT_API = "/api/admin/users?roleCode=STUDENT&page=0&size=999";
const MEMBER_API = "/api/groups";

function LecturerGroupList() {
  const [groups, setGroups] = useState([]);
  const [students, setStudents] = useState([]);
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showMemberModal, setShowMemberModal] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState(null);

  const token = localStorage.getItem("token");

  useEffect(() => {
    fetchMyGroups();
    fetchStudents();
  }, []);

  const fetchMyGroups = async () => {
    setLoading(true);
    setError("");
    
    try {
      const res = await fetch(API_URL, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!res.ok) {
        throw new Error("Failed to fetch groups");
      }

      const data = await res.json();
      setGroups(data.data || []);
    } catch (err) {
      setError(err.message || "Error occurred");
    } finally {
      setLoading(false);
    }
  };

  const fetchStudents = async () => {
    try {
      const res = await fetch(STUDENT_API, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      const data = await res.json();
      if (data.data && data.data.content) {
        setStudents(data.data.content);
      } else if (Array.isArray(data.data)) {
        setStudents(data.data);
      } else {
        setStudents([]);
      }
    } catch (err) {
      console.error("Error fetching students:", err);
    }
  };

  const fetchMembers = async (groupId) => {
    try {
      const res = await fetch(`${MEMBER_API}/${groupId}/members`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || "Failed to fetch members");
      }

      const data = await res.json();
      setMembers(data.data || []);
    } catch (err) {
      alert("Error: " + err.message);
      throw err;
    }
  };

  const handleOpenMemberModal = async (group) => {
    setSelectedGroup(group);
    try {
      await fetchMembers(group.groupId);
      setShowMemberModal(true);
    } catch (err) {
      console.error("Cannot open member modal:", err);
    }
  };

  const handleCloseMemberModal = () => {
    setShowMemberModal(false);
    setSelectedGroup(null);
    setMembers([]);
  };

  const handleAddMember = async (userId) => {
    if (!selectedGroup) return;

    try {
      const res = await fetch(`${MEMBER_API}/${selectedGroup.groupId}/members`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ userId: userId }),
      });

      if (res.ok) {
        alert("Member added successfully!");
        await fetchMembers(selectedGroup.groupId);
      } else {
        const err = await res.json();
        alert("Failed to add member: " + (err.message || "Unknown error"));
      }
    } catch (err) {
      alert("Error: " + err.message);
    }
  };

  const handleRemoveMember = async (userId) => {
    if (!selectedGroup) return;
    if (!window.confirm("Remove this member?")) return;

    try {
      const res = await fetch(`${MEMBER_API}/${selectedGroup.groupId}/members/${userId}`, {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (res.ok) {
        alert("Member removed successfully!");
        await fetchMembers(selectedGroup.groupId);
      } else {
        const err = await res.json();
        alert("Failed to remove member: " + (err.message || "Unknown error"));
      }
    } catch (err) {
      alert("Error: " + err.message);
    }
  };

  const handleSetLeader = async (userId) => {
    if (!selectedGroup) return;
    if (!window.confirm("Set this member as leader?")) return;

    try {
      const res = await fetch(`${MEMBER_API}/${selectedGroup.groupId}/leader`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ userId: userId }),
      });

      if (res.ok) {
        alert("Leader set successfully!");
        await fetchMembers(selectedGroup.groupId);
      } else {
        const err = await res.json();
        alert("Failed to set leader: " + (err.message || "Unknown error"));
      }
    } catch (err) {
      alert("Error: " + err.message);
    }
  };

  if (loading) {
    return (
      <div className="lecturer-container">
        <h3>My Assigned Groups</h3>
        <p>Loading...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="lecturer-container">
        <h3>My Assigned Groups</h3>
        <p className="error">{error}</p>
      </div>
    );
  }

  return (
    <div className="lecturer-container">
      <h3>My Assigned Groups</h3>
      <p className="subtitle">You are assigned to {groups.length} group(s)</p>

      {groups.length === 0 ? (
        <div className="empty-state">
          <span className="empty-icon">📋</span>
          <p>No groups assigned yet</p>
          <p className="empty-hint">Contact your admin to assign you to groups</p>
        </div>
      ) : (
        <div className="groups-grid">
          {groups.map((group) => (
            <div key={group.groupId} className="group-card">
              <div className="group-header">
                <h4>{group.groupName}</h4>
                <span className="group-code">{group.groupCode}</span>
              </div>
              <div className="group-details">
                <div className="detail-item">
                  <span className="detail-label">Course:</span>
                  <span className="detail-value">{group.courseCode}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Semester:</span>
                  <span className="detail-value">{group.semester}</span>
                </div>
              </div>
              <button 
                className="view-btn" 
                onClick={() => handleOpenMemberModal(group)}
              >
                Manage Members
              </button>
            </div>
          ))}
        </div>
      )}

      {/* MODAL - MEMBER MANAGEMENT */}
      {showMemberModal && (
        <div className="modal-overlay" onClick={handleCloseMemberModal}>
          <div className="modal-content modal-large" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Manage Members - {selectedGroup?.groupName}</h3>
              <button className="close-btn" onClick={handleCloseMemberModal}>
                ×
              </button>
            </div>

            <div className="modal-body">
              {/* Current Members */}
              <div className="members-section">
                <h4>Current Members ({members.length})</h4>
                <table className="member-table">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Full Name</th>
                      <th>Username</th>
                      <th>Email</th>
                      <th>Role</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {members.map((member, index) => (
                      <tr key={member.userId}>
                        <td>{index + 1}</td>
                        <td>{member.fullName}</td>
                        <td>{member.username}</td>
                        <td>{member.email}</td>
                        <td>
                          {member.memberRole === "LEADER" ? (
                            <span className="leader-badge">Leader</span>
                          ) : (
                            <span className="member-badge">Member</span>
                          )}
                        </td>
                        <td>
                          {member.memberRole !== "LEADER" && (
                            <button
                              className="leader-btn"
                              onClick={() => handleSetLeader(member.userId)}
                            >
                              Set Leader
                            </button>
                          )}
                          <button
                            className="remove-btn"
                            onClick={() => handleRemoveMember(member.userId)}
                          >
                            Remove
                          </button>
                        </td>
                      </tr>
                    ))}
                    {members.length === 0 && (
                      <tr>
                        <td colSpan="6" style={{ textAlign: "center" }}>
                          No members yet
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>

              {/* Add Member */}
              <div className="add-member-section">
                <h4>Add Student</h4>
                <table className="student-table">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Full Name</th>
                      <th>Username</th>
                      <th>Email</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {students
                      .filter(s => !members.some(m => m.userId === s.userId))
                      .map((student, index) => (
                        <tr key={student.userId}>
                          <td>{index + 1}</td>
                          <td>{student.fullName}</td>
                          <td>{student.username}</td>
                          <td>{student.email}</td>
                          <td>
                            <button
                              className="add-btn"
                              onClick={() => handleAddMember(student.userId)}
                            >
                              Add
                            </button>
                          </td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default LecturerGroupList;