import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "./LecturerClassList.css";

const CLASS_API = "/api/lecturer/classes";
const GROUP_API = "/api/student_group";
const MEMBER_API = "/api/groups";

export default function LecturerClassList() {
  const [classes, setClasses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  // Groups modal
  const [showGroupsModal, setShowGroupsModal] = useState(false);
  const [selectedClass, setSelectedClass] = useState(null);
  const [groups, setGroups] = useState([]);
  const [groupsLoading, setGroupsLoading] = useState(false);
  const [showCreateGroup, setShowCreateGroup] = useState(false);
  const [newGroupName, setNewGroupName] = useState("");
  const [createError, setCreateError] = useState("");
  const [editGroupId, setEditGroupId] = useState(null);
  const [editGroupName, setEditGroupName] = useState("");
  const [editError, setEditError] = useState("");

  // Members modal
  const [showMemberModal, setShowMemberModal] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState(null);
  const [members, setMembers] = useState([]);
  const [eligibleStudents, setEligibleStudents] = useState([]);
  const [studentKeyword, setStudentKeyword] = useState("");
  const [studentPage, setStudentPage] = useState(0);
  const [studentTotalPages, setStudentTotalPages] = useState(0);

  const token = localStorage.getItem("token");
  const auth = () => ({ Authorization: `Bearer ${token}` });
  const authJson = () => ({ ...auth(), "Content-Type": "application/json" });

  useEffect(() => { fetchClasses(); }, []);

  const fetchClasses = async () => {
    setLoading(true); setError("");
    try {
      const res = await fetch(CLASS_API, { headers: auth() });
      if (!res.ok) throw new Error("Failed to fetch classes");
      const data = await res.json();
      setClasses(data.data || []);
    } catch (err) {
      setError(err.message || "Error occurred");
    } finally {
      setLoading(false);
    }
  };

  //  Groups 
  const fetchGroupsForClass = async (classId) => {
    setGroupsLoading(true);
    try {
      const res = await fetch(`${GROUP_API}?class_id=${classId}`, { headers: auth() });
      const data = await res.json();
      // filter chỉ lấy group thuộc class này
      const all = data.data || [];
      setGroups(all.filter(g => g.classId === classId || g.academicClass?.classId === classId));
    } catch { setGroups([]); }
    finally { setGroupsLoading(false); }
  };

  const openGroupsModal = async (cls) => {
    setSelectedClass(cls);
    setShowCreateGroup(false);
    setNewGroupName("");
    setCreateError("");
    await fetchGroupsForClass(cls.classId);
    setShowGroupsModal(true);
  };

  const handleCreateGroup = async () => {
    if (!newGroupName.trim()) { setCreateError("Group name is required"); return; }
    setCreateError("");
    const res = await fetch(`${GROUP_API}/add`, {
      method: "POST",
      headers: authJson(),
      body: JSON.stringify({ classId: selectedClass.classId, groupName: newGroupName.trim() }),
    });
    if (res.ok) {
      setNewGroupName("");
      setShowCreateGroup(false);
      await fetchGroupsForClass(selectedClass.classId);
    } else {
      const err = await res.json();
      setCreateError(err.message || "Failed to create group");
    }
  };

  const handleDeleteGroup = async (groupId) => {
    if (!window.confirm("Delete this group?")) return;
    await fetch(`${GROUP_API}/delete/${groupId}`, { method: "DELETE", headers: auth() });
    await fetchGroupsForClass(selectedClass.classId);
  };

  const handleUpdateGroup = async (groupId) => {
    if (!editGroupName.trim()) { setEditError("Group name is required"); return; }
    setEditError("");
    const res = await fetch(`${GROUP_API}/update/${groupId}`, {
      method: "PUT",
      headers: authJson(),
      body: JSON.stringify({ groupName: editGroupName.trim() }),
    });
    if (res.ok) {
      setEditGroupId(null);
      setEditGroupName("");
      await fetchGroupsForClass(selectedClass.classId);
    } else {
      const err = await res.json();
      setEditError(err.message || "Failed to update group");
    }
  };

  //  Members ─
  const openMemberModal = async (group) => {
    setSelectedGroup(group);
    setStudentKeyword("");
    await fetchMembers(group.groupId);
    await fetchEligibleStudents(group.groupId, "", 0);
    setShowMemberModal(true);
  };

  const fetchMembers = async (groupId) => {
    const res = await fetch(`${MEMBER_API}/${groupId}/members`, { headers: auth() });
    const data = await res.json();
    setMembers(data.data || []);
  };

  const fetchEligibleStudents = async (groupId, keyword = "", p = 0) => {
    try {
      const res = await fetch(
        `/api/groups/${groupId}/members/search?keyword=${encodeURIComponent(keyword)}&page=${p}&size=5`,
        { headers: auth() }
      );
      const data = await res.json();
      setEligibleStudents(data.data?.content || []);
      setStudentTotalPages(data.data?.totalPages || 0);
      setStudentPage(p);
    } catch { setEligibleStudents([]); }
  };

  const handleAddMember = async (userId) => {
    const res = await fetch(`${MEMBER_API}/${selectedGroup.groupId}/members`, {
      method: "POST", headers: authJson(),
      body: JSON.stringify({ userId }),
    });
    if (res.ok) {
      await fetchMembers(selectedGroup.groupId);
      await fetchEligibleStudents(selectedGroup.groupId, studentKeyword, studentPage);
    } else {
      const err = await res.json();
      alert("Failed: " + (err.message || "Unknown error"));
    }
  };

  const handleRemoveMember = async (userId) => {
    if (!window.confirm("Remove this member?")) return;
    const res = await fetch(`${MEMBER_API}/${selectedGroup.groupId}/members/${userId}`, {
      method: "DELETE", headers: auth(),
    });
    if (res.ok) {
      await fetchMembers(selectedGroup.groupId);
      await fetchEligibleStudents(selectedGroup.groupId, studentKeyword, studentPage);
    }
  };

  const handleSetLeader = async (userId) => {
    if (!window.confirm("Set this member as leader?")) return;
    const res = await fetch(`${MEMBER_API}/${selectedGroup.groupId}/leader`, {
      method: "PUT", headers: authJson(),
      body: JSON.stringify({ userId }),
    });
    if (res.ok) await fetchMembers(selectedGroup.groupId);
    else { const err = await res.json(); alert("Failed: " + (err.message || "Unknown")); }
  };

  //  Render 
  if (loading) return (
    <div className="lcl-root"><div className="lcl-loading"><span className="lcl-spinner" />Loading...</div></div>
  );

  if (error) return (
    <div className="lcl-root"><div className="lcl-error">{error}</div></div>
  );

  return (
    <div className="lcl-root">
      <div className="lcl-page-header">
        <div>
          <h1 className="lcl-page-title">My Assigned Classes</h1>
          <p className="lcl-page-desc">You are assigned to {classes.length} class(es)</p>
        </div>
        <button className="lcl-refresh-btn" onClick={fetchClasses}>
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8" /><path d="M21 3v5h-5" />
          </svg>
          Refresh
        </button>
      </div>

      {classes.length === 0 ? (
        <div className="lcl-empty">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.2">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" /><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
          </svg>
          <p>No classes assigned yet</p>
          <span>Contact your admin to assign you to classes</span>
        </div>
      ) : (
        <div className="lcl-grid">
          {classes.map(cls => (
            <div key={cls.classId} className="lcl-card">
              <div className="lcl-card-header">
                <div className="lcl-card-icon">{cls.classCode?.charAt(0) || "C"}</div>
                <div className="lcl-card-title-wrap">
                  <h3 className="lcl-card-title">{cls.classCode}</h3>
                  <span className="lcl-course-badge">{cls.courseCode}</span>
                </div>
              </div>
              <div className="lcl-card-body">
                <div className="lcl-card-row">
                  <span className="lcl-card-label">Course</span>
                  <span className="lcl-card-val">{cls.courseName || cls.courseCode}</span>
                </div>
                <div className="lcl-card-row">
                  <span className="lcl-card-label">Semester</span>
                  <span className="lcl-card-val">{cls.semesterCode || "—"}</span>
                </div>
              </div>
              <div className="lcl-card-actions">
                <button className="lcl-btn-groups" onClick={() => navigate(`/lecturer/classes/${cls.classId}`)}>
                  Manage Class
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/*  Groups Modal  */}
      {showGroupsModal && (
        <div className="lcl-modal-overlay" onClick={() => setShowGroupsModal(false)}>
          <div className="lcl-modal lcl-modal-lg" onClick={e => e.stopPropagation()}>
            <div className="lcl-modal-header">
              <div>
                <div className="lcl-modal-title">Groups — {selectedClass?.classCode}</div>
                <div className="lcl-modal-subtitle">{selectedClass?.semesterCode}</div>
              </div>
              <button className="lcl-modal-close" onClick={() => setShowGroupsModal(false)}>×</button>
            </div>
            <div className="lcl-modal-body">
              {/* Create group */}
              <div className="lcl-create-group-bar">
                {!showCreateGroup ? (
                  <button className="lcl-btn-create" onClick={() => setShowCreateGroup(true)}>
                    + New Group
                  </button>
                ) : (
                  <div className="lcl-create-group-form">
                    <input
                      className="lcl-input"
                      placeholder="Group name..."
                      value={newGroupName}
                      onChange={e => setNewGroupName(e.target.value)}
                      onKeyDown={e => e.key === "Enter" && handleCreateGroup()}
                      autoFocus
                    />
                    <button className="lcl-btn-primary" onClick={handleCreateGroup}>Create</button>
                    <button className="lcl-btn-ghost" onClick={() => { setShowCreateGroup(false); setNewGroupName(""); setCreateError(""); }}>Cancel</button>
                    {createError && <span className="lcl-create-error">{createError}</span>}
                  </div>
                )}
              </div>

              {groupsLoading ? (
                <div className="lcl-loading"><span className="lcl-spinner" />Loading groups...</div>
              ) : groups.length === 0 ? (
                <div className="lcl-empty-sm">No groups yet. Create one above.</div>
              ) : (
                <table className="lcl-table">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Group Name</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {groups.map((g, i) => (
                      <tr key={g.groupId}>
                        <td className="lcl-td-num">{i + 1}</td>
                        <td>
                          {editGroupId === g.groupId ? (
                            <div className="lcl-inline-edit">
                              <input
                                className="lcl-input"
                                value={editGroupName}
                                onChange={e => setEditGroupName(e.target.value)}
                                onKeyDown={e => e.key === "Enter" && handleUpdateGroup(g.groupId)}
                                autoFocus
                              />
                              <button className="lcl-btn-primary lcl-btn-xs" onClick={() => handleUpdateGroup(g.groupId)}>Save</button>
                              <button className="lcl-btn-ghost lcl-btn-xs" onClick={() => { setEditGroupId(null); setEditError(""); }}>Cancel</button>
                              {editError && <span className="lcl-create-error">{editError}</span>}
                            </div>
                          ) : (
                            <strong>{g.groupName}</strong>
                          )}
                        </td>
                        <td>
                          <div className="lcl-row-actions">
                            <button className="lcl-btn-action lcl-btn-members" onClick={() => { setShowGroupsModal(false); openMemberModal(g); }}>
                              Members
                            </button>
                            <button className="lcl-btn-action lcl-btn-edit2" onClick={() => { setEditGroupId(g.groupId); setEditGroupName(g.groupName); setEditError(""); }}>
                              Edit
                            </button>
                            <button className="lcl-btn-action lcl-btn-stats2" onClick={() => navigate(`/lecturer/groups/${g.groupId}/stats`)}>
                              Stats
                            </button>
                            <button className="lcl-btn-action lcl-btn-danger" onClick={() => handleDeleteGroup(g.groupId)}>
                              Delete
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </div>
      )}

      {/*  Members Modal  */}
      {showMemberModal && (
        <div className="lcl-modal-overlay" onClick={() => setShowMemberModal(false)}>
          <div className="lcl-modal lcl-modal-xl" onClick={e => e.stopPropagation()}>
            <div className="lcl-modal-header">
              <div>
                <div className="lcl-modal-title">Members — {selectedGroup?.groupName}</div>
                <div className="lcl-modal-subtitle">{members.length} member(s)</div>
              </div>
              <button className="lcl-modal-close" onClick={() => setShowMemberModal(false)}>×</button>
            </div>
            <div className="lcl-modal-body">
              {/* Current members */}
              <div className="lcl-section-title">Current Members</div>
              <table className="lcl-table">
                <thead>
                  <tr><th>#</th><th>Full Name</th><th>Username</th><th>Email</th><th>Role</th><th>Actions</th></tr>
                </thead>
                <tbody>
                  {members.length === 0
                    ? <tr><td colSpan="6" className="lcl-empty-row">No members yet</td></tr>
                    : members.map((m, i) => (
                      <tr key={m.userId}>
                        <td className="lcl-td-num">{i + 1}</td>
                        <td>{m.fullName}</td>
                        <td><span className="lcl-username">{m.username}</span></td>
                        <td>{m.email}</td>
                        <td>
                          <span className={`lcl-role-badge ${m.memberRole === "LEADER" ? "lcl-leader" : "lcl-member"}`}>
                            {m.memberRole}
                          </span>
                        </td>
                        <td>
                          <div className="lcl-row-actions">
                            {m.memberRole !== "LEADER" && (
                              <button className="lcl-btn-action lcl-btn-leader" onClick={() => handleSetLeader(m.userId)}>Set Leader</button>
                            )}
                            <button className="lcl-btn-action lcl-btn-danger" onClick={() => handleRemoveMember(m.userId)}>Remove</button>
                          </div>
                        </td>
                      </tr>
                    ))
                  }
                </tbody>
              </table>

              {/* Add student */}
              <div className="lcl-section-title" style={{ marginTop: 20 }}>Add Student</div>
              <input
                className="lcl-input lcl-search-input"
                placeholder="Search by name or username..."
                value={studentKeyword}
                onChange={e => { setStudentKeyword(e.target.value); fetchEligibleStudents(selectedGroup.groupId, e.target.value.trim(), 0); }}
              />
              <table className="lcl-table">
                <thead>
                  <tr><th>#</th><th>Full Name</th><th>Username</th><th>Email</th><th>Action</th></tr>
                </thead>
                <tbody>
                  {eligibleStudents.length === 0
                    ? <tr><td colSpan="5" className="lcl-empty-row">No eligible students</td></tr>
                    : eligibleStudents.map((s, i) => (
                      <tr key={s.userId}>
                        <td className="lcl-td-num">{i + 1}</td>
                        <td>{s.fullName}</td>
                        <td><span className="lcl-username">{s.username}</span></td>
                        <td>{s.email}</td>
                        <td><button className="lcl-btn-action lcl-btn-add" onClick={() => handleAddMember(s.userId)}>Add</button></td>
                      </tr>
                    ))
                  }
                </tbody>
              </table>
              {studentTotalPages > 1 && (
                <div className="lcl-pagination">
                  {Array.from({ length: studentTotalPages }, (_, i) => (
                    <button key={i} className={`lcl-page-num ${i === studentPage ? "active" : ""}`}
                      onClick={() => fetchEligibleStudents(selectedGroup.groupId, studentKeyword, i)}>
                      {i + 1}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}