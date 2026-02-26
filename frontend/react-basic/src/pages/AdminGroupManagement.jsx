import { useEffect, useState } from "react";
import "./AdminGroupManagement.css";

const API_URL = "/api/student_group";
const LECTURER_API = "/api/admin/users?roleCode=LECTURER&page=0&size=999";
const ASSIGN_API = "/api/admin/groups";
const MEMBER_API = "/api/groups";

function AdminGroupManagement() {
  const [groups, setGroups] = useState([]);
  const [lecturers, setLecturers] = useState([]);
  const [members, setMembers] = useState([]);
  const [eligibleStudents, setEligibleStudents] = useState([]);
  const [studentKeyword, setStudentKeyword] = useState("");
  const [studentPage, setStudentPage] = useState(0);
  const [studentTotalPages, setStudentTotalPages] = useState(0);
  const [form, setForm] = useState({
    groupId: null,
    classCode: "",
    groupName: "",
    courseCode: "",
    semester: "",
  });

  const [filter, setFilter] = useState({
    courseCode: "",
    semester: "",
  });

  const [error, setError] = useState("");
  const [showLecturerModal, setShowLecturerModal] = useState(false);
  const [showMemberModal, setShowMemberModal] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState(null);

  const token = localStorage.getItem("token");

  useEffect(() => {
    fetchGroups();
    fetchLecturers();
  }, []);

  const fetchGroups = async () => {
    let query = [];
    if (filter.courseCode) query.push(`course_code=${filter.courseCode}`);
    if (filter.semester) query.push(`semester=${filter.semester}`);

    const url = query.length ? `${API_URL}?${query.join("&")}` : API_URL;

    const res = await fetch(url, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
    const data = await res.json();
    setGroups(data.data || []);
  };

  const fetchLecturers = async () => {
    const res = await fetch(LECTURER_API, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
    const data = await res.json();
    if (data.data && data.data.content) {
      setLecturers(data.data.content);
    } else if (Array.isArray(data.data)) {
      setLecturers(data.data);
    } else {
      setLecturers([]);
    }
  };

  const fetchMembers = async (groupId) => {
    const res = await fetch(`${MEMBER_API}/${groupId}/members`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
    const data = await res.json();
    setMembers(data.data || []);
  };

  const handleOpenLecturerModal = (group) => {
    setSelectedGroup(group);
    setShowLecturerModal(true);
  };

  const handleCloseLecturerModal = () => {
    setShowLecturerModal(false);
    setSelectedGroup(null);
  };

  const handleOpenMemberModal = async (group) => {
    setSelectedGroup(group);
    await fetchMembers(group.groupId);
    await fetchEligibleStudents(group.groupId);
    setShowMemberModal(true);
  };

  const handleCloseMemberModal = () => {
    setShowMemberModal(false);
    setSelectedGroup(null);
    setMembers([]);
    setStudentKeyword("");
  };

  const handleAssignLecturer = async (lecturerId) => {
    if (!selectedGroup) return;

    const res = await fetch(`${ASSIGN_API}/${selectedGroup.groupId}/lecturer`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ lecturerId: lecturerId }),
    });

    if (res.ok) {
      alert("Lecturer assigned successfully!");
      handleCloseLecturerModal();
      fetchGroups();
    } else {
      const err = await res.json();
      alert("Failed to assign lecturer: " + (err.message || "Unknown error"));
    }
  };

  const handleAddMember = async (userId) => {
    if (!selectedGroup) return;

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
      setStudentKeyword("");
      await fetchEligibleStudents(selectedGroup.groupId);
    } else {
      const err = await res.json();
      alert("Failed to add member: " + (err.message || "Unknown error"));
    }
  };

  const fetchEligibleStudents = async (groupId, keyword = "", p = 0) => {
    try {
      const res = await fetch(
        `/api/groups/${groupId}/members/search?keyword=${keyword}&page=${p}&size=5`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!res.ok) {
        throw new Error("Failed to search students");
      }

      const data = await res.json();
      setEligibleStudents(data.data.content);
      setStudentTotalPages(data.data.totalPages);
      setStudentPage(p);
    } catch (err) {
      console.error("Search students error:", err);
    }
  };

  const handleRemoveMember = async (userId) => {
    if (!selectedGroup) return;
    if (!window.confirm("Remove this member?")) return;

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
  };

  const handleSetLeader = async (userId) => {
    if (!selectedGroup) return;
    if (!window.confirm("Set this member as leader?")) return;

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
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    const method = form.groupId ? "PUT" : "POST";
    const url = form.groupId
      ? `${API_URL}/update/${form.groupId}`
      : `${API_URL}/add`;

    const res = await fetch(url, {
      method,
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        classCode: form.classCode,
        groupName: form.groupName,
        courseCode: form.courseCode,
        semester: form.semester,
      }),
    });

    if (!res.ok) {
      const err = await res.json();
      if (res.status === 409) {
        setError("Class Code already exists");
      } else {
        setError(err.message || "Error occurred");
      }
      return;
    }

    alert(form.groupId ? "Group updated successfully!" : "Group created successfully!");
    resetForm();
    fetchGroups();
  };

  const handleEdit = (g) => {
    setForm({
      groupId: g.groupId,
      classCode: g.classCode,
      groupName: g.groupName,
      courseCode: g.courseCode,
      semester: g.semester,
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Delete this group?")) return;

    await fetch(`${API_URL}/delete/${id}`, {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    fetchGroups();
  };

  const resetForm = () => {
    setForm({
      groupId: null,
      classCode: "",
      groupName: "",
      courseCode: "",
      semester: "",
    });
    setError("");
  };

  return (
    <div className="group-container">
      <h3>👥 Group Management</h3>

      {/* FILTER */}
      <div className="group-filter">
        <input
          placeholder="Course Code"
          value={filter.courseCode}
          onChange={(e) =>
            setFilter({ ...filter, courseCode: e.target.value })
          }
        />
        <input
          placeholder="Semester"
          value={filter.semester}
          onChange={(e) => setFilter({ ...filter, semester: e.target.value })}
        />
        <button onClick={fetchGroups}>Filter</button>
        <button
          className="cancel"
          onClick={() => {
            setFilter({ courseCode: "", semester: "" });
            fetchGroups();
          }}
        >
          Clear
        </button>
      </div>

      {/* FORM */}
      <form className="group-form" onSubmit={handleSubmit}>
        <input
          placeholder="Class Code"
          value={form.classCode}
          disabled={!!form.groupId}
          onChange={(e) => setForm({ ...form, classCode: e.target.value })}
          required
        />

        <input
          placeholder="Group Name"
          value={form.groupName}
          onChange={(e) => setForm({ ...form, groupName: e.target.value })}
          required
        />

        <input
          placeholder="Course Code"
          value={form.courseCode}
          onChange={(e) => setForm({ ...form, courseCode: e.target.value })}
          required
        />

        <input
          placeholder="Semester"
          value={form.semester}
          onChange={(e) => setForm({ ...form, semester: e.target.value })}
          required
        />

        {error && <p className="error">{error}</p>}

        <div className="form-actions">
          <button type="submit">
            {form.groupId ? "Update Group" : "Create Group"}
          </button>
          {form.groupId && (
            <button type="button" className="cancel" onClick={resetForm}>
              Cancel
            </button>
          )}
        </div>
      </form>

      {/* TABLE */}
      <table className="group-table">
        <thead>
          <tr>
            <th>#</th>
            <th>Class Code</th>
            <th>Group Name</th>
            <th>Course</th>
            <th>Semester</th>
            <th>Lecturer</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {groups.length > 0 ? (
            groups.map((g, index) => (
              <tr key={g.groupId}>
                <td>{index + 1}</td>
                <td><strong>{g.classCode}</strong></td>
                <td>{g.groupName}</td>
                <td>{g.courseCode}</td>
                <td>{g.semester}</td>
                <td>
                  {g.lecturerName ? (
                    <span className="lecturer-badge">{g.lecturerName}</span>
                  ) : (
                    <span className="no-lecturer">Not assigned</span>
                  )}
                </td>
                <td>
                  <button onClick={() => handleEdit(g)}>Edit</button>
                  <button className="members-btn" onClick={() => handleOpenMemberModal(g)}>
                    Members
                  </button>
                  <button className="choose-btn" onClick={() => handleOpenLecturerModal(g)}>
                    Lecturer
                  </button>
                  <button className="danger" onClick={() => handleDelete(g.groupId)}>
                    Delete
                  </button>
                </td>
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan="7" style={{ textAlign: "center", padding: "32px", color: "var(--text-tertiary)" }}>
                No groups found
              </td>
            </tr>
          )}
        </tbody>
      </table>

      {/* MODAL - LECTURER SELECTION */}
      {showLecturerModal && (
        <div className="modal-overlay" onClick={handleCloseLecturerModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Assign Lecturer - {selectedGroup?.groupName}</h3>
              <button className="close-btn" onClick={handleCloseLecturerModal}>
                ×
              </button>
            </div>

            <div className="modal-body">
              <table className="lecturer-table">
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
                  {lecturers.length > 0 ? (
                    lecturers.map((lect, index) => (
                      <tr key={lect.userId}>
                        <td>{index + 1}</td>
                        <td>{lect.fullName}</td>
                        <td>{lect.username}</td>
                        <td>{lect.email}</td>
                        <td>
                          <button
                            className="assign-btn"
                            onClick={() => handleAssignLecturer(lect.userId)}
                            disabled={selectedGroup?.lecturerId === lect.userId}
                          >
                            {selectedGroup?.lecturerId === lect.userId
                              ? "Assigned"
                              : "Assign"}
                          </button>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan="5" style={{ textAlign: "center", padding: "32px" }}>
                        No lecturers found
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
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
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {members.length > 0 ? (
                      members.map((member, index) => (
                        <tr key={member.userId}>
                          <td>{index + 1}</td>
                          <td>{member.fullName}</td>
                          <td>{member.username}</td>
                          <td>{member.email}</td>
                          <td>
                            {member.memberRole === "LEADER" ? (
                              <span className="leader-badge">👑 Leader</span>
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
                      ))
                    ) : (
                      <tr>
                        <td colSpan="6" style={{ textAlign: "center", padding: "24px" }}>
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

                <input
                  type="text"
                  placeholder="🔍 Search by name or username..."
                  value={studentKeyword}
                  onChange={(e) => {
                    const value = e.target.value;
                    setStudentKeyword(value);
                    fetchEligibleStudents(selectedGroup.groupId, value.trim(), 0);
                  }}
                  className="search-input"
                />

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
                    {eligibleStudents.length > 0 ? (
                      eligibleStudents.map((student, index) => (
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
                      ))
                    ) : (
                      <tr>
                        <td colSpan="5" style={{ textAlign: "center", padding: "24px" }}>
                          No eligible students
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>

                {studentTotalPages > 1 && (
                  <div className="pagination">
                    {Array.from({ length: studentTotalPages }, (_, i) => (
                      <button
                        key={i}
                        className={i === studentPage ? "active" : ""}
                        onClick={() =>
                          fetchEligibleStudents(selectedGroup.groupId, studentKeyword, i)
                        }
                      >
                        {i + 1}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default AdminGroupManagement;
