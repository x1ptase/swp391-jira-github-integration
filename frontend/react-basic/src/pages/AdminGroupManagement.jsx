import { useEffect, useState } from "react";
import "./AdminGroupManagement.css";

const API_URL = "/api/student_group";
const LECTURER_API = "/api/admin/users?roleCode=LECTURER&page=0&size=999";
const STUDENT_API = "/api/admin/users?roleCode=STUDENT&page=0&size=999";
const ASSIGN_API = "/api/admin/groups";
const MEMBER_API = "/api/groups";

function AdminGroupManagement() {
  const [groups, setGroups] = useState([]);
  const [lecturers, setLecturers] = useState([]);
  const [students, setStudents] = useState([]);
  const [members, setMembers] = useState([]);
  const [form, setForm] = useState({
    groupId: null,
    groupCode: "",
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
    fetchStudents();
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
    // Nếu API trả về Page object
    if (data.data && data.data.content) {
      setLecturers(data.data.content);
    } else if (Array.isArray(data.data)) {
      setLecturers(data.data);
    } else {
      setLecturers([]);
    }

    //Debug để kiểm tra structure
    console.log("Lecturers response:", data);
  };
  // Fetch danh sách students
  const fetchStudents = async () => {
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
  };

  //  Fetch members của group
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
    setShowMemberModal(true);
  };

  const handleCloseMemberModal = () => {
    setShowMemberModal(false);
    setSelectedGroup(null);
    setMembers([]);
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
      fetchGroups(); // Refresh để cập nhật lecturer
    } else {
      const err = await res.json();
      alert("Failed to assign lecturer: " + (err.message || "Unknown error"));
    }
  };

  // add member 
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
    } else {
      const err = await res.json();
      alert("Failed to add member: " + (err.message || "Unknown error"));
    }
  };

  //remove member
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

  //set leader
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
        groupCode: form.groupCode,      
        groupName: form.groupName,      
        courseCode: form.courseCode, 
        semester: form.semester,
      }),
    });

    if (!res.ok) {
      const err = await res.json();
      if (res.status === 409) {
        setError("Group Code already exists");
      } else {
        setError(err.message || "Error occurred");
      }
      return;
    }

    resetForm();
    fetchGroups();
  };

  const handleEdit = (g) => {
    setForm({
      groupId: g.groupId,
      groupCode: g.groupCode,
      groupName: g.groupName,
      courseCode: g.courseCode,
      semester: g.semester,
    });
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
      groupCode: "",
      groupName: "",
      courseCode: "",
      semester: "",
    });
    setError("");
  };

  return (
    <div className="group-container">
      <h3>Group Management</h3>

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
          placeholder="Group Code"
          value={form.groupCode}
          disabled={!!form.groupId}
          onChange={(e) => setForm({ ...form, groupCode: e.target.value })}
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

        <div className="form-actions">
          <button type="submit">
            {form.groupId ? "Update" : "Create"}
          </button>
          {form.groupId && (
            <button type="button" className="cancel" onClick={resetForm}>
              Cancel
            </button>
          )}
        </div>

        {error && <p className="error">{error}</p>}
      </form>

      {/* TABLE */}
      <table className="group-table">
        <thead>
          <tr>
            <th>#</th>
            <th>Group Code</th>
            <th>Group Name</th>
            <th>Course Code</th>
            <th>Semester</th>
            <th>Assigned Lecturer</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {groups.map((g, index) => (
            <tr key={g.groupId}>
              <td>{index + 1}</td>
              <td>{g.groupCode}</td>
              <td>{g.groupName}</td>
              <td>{g.courseCode}</td>
              <td>{g.semester}</td>
              <td>
                {g.lecturerName ? (
                  <span className="lecturer-badge">
                    {g.lecturerName}
                  </span>
                ) : (
                  <span className="no-lecturer">Not assigned</span>
                )}
              </td>
              <td>
                <button onClick={() => handleEdit(g)}>Edit</button>
                {/*nút Members */}
                <button
                  className="members-btn"
                  onClick={() => handleOpenMemberModal(g)}
                >
                  Members
                </button>
                {/*nút Choose Lecturer */}
                <button
                  className="choose-btn"
                  onClick={() => handleOpenLecturerModal(g)}
                >
                  Choose
                </button>
                <button
                  className="danger"
                  onClick={() => handleDelete(g.groupId)}
                >
                  Delete
                </button>
              </td>
            </tr>
          ))}
          {groups.length === 0 && (
            <tr>
              <td colSpan="7" style={{ textAlign: "center" }}>
                No data
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
              <h3>Select Lecturer for {selectedGroup?.groupName}</h3>
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
                  {lecturers.map((lect, index) => (
                    <tr key={lect.userId}>
                      <td>{index + 1}</td>
                      <td>{lect.fullName}</td>
                      <td>{lect.username}</td>
                      <td>{lect.email}</td>
                      <td>
                        <button
                          className="assign-btn"
                          onClick={() => handleAssignLecturer(lect.userId)}
                          disabled={
                            selectedGroup?.lecturerId === lect.userId
                          }
                        >
                          {selectedGroup?.lecturerId === lect.userId
                            ? "Assigned"
                            : "Assign"}
                        </button>
                      </td>
                    </tr>
                  ))}
                  {lecturers.length === 0 && (
                    <tr>
                      <td colSpan="5" style={{ textAlign: "center" }}>
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
                          {member.memberRole==="LEADER" ? (
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

export default AdminGroupManagement;