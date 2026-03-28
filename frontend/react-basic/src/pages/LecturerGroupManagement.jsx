import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import CommitStats from "./CommitStats";
import RequirementDashboard from "./RequirementDashboard";
import "./LecturerGroupManagement.css";
import LogoutButton from "./LogoutButton";
import logo from "../assets/logo.png";

const CLASS_API = "/api/lecturer/classes";
const CLASS_STUDENTS_API = "/api/lecturer/classes";
const GROUP_API = "/api/student_group";
const MEMBER_API = "/api/groups";
const TOPIC_API = "/api/topics";

export default function LecturerGroupManagement() {
  const { classId } = useParams();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("overview");
  const [classInfo, setClassInfo] = useState(null);

  // Overview tab
  const [groupSummaries, setGroupSummaries] = useState([]);
  const [expandedGroupId, setExpandedGroupId] = useState(null);
  const [overviewLoading, setOverviewLoading] = useState(false);
  const [selectedGroupForStats, setSelectedGroupForStats] = useState(null);

  // Students tab (inside Overview)
  const [classStudents, setClassStudents] = useState([]);
  const [studentSearch, setStudentSearch] = useState("");
  const [hasGroupFilter, setHasGroupFilter] = useState("");
  const [studentsLoading, setStudentsLoading] = useState(false);
  const [studentPage, setStudentPage] = useState(0);
  const [studentTotalPages, setStudentTotalPages] = useState(0);

  // Groups tab
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
  const [memberKeyword, setMemberKeyword] = useState("");
  const [memberPage, setMemberPage] = useState(0);
  const [memberTotalPages, setMemberTotalPages] = useState(0);

  // Topic modal
  const [showTopicModal, setShowTopicModal] = useState(false);
  const [topicGroup, setTopicGroup] = useState(null);
  const [topics, setTopics] = useState([]);
  const [topicSearch, setTopicSearch] = useState("");
  const [assigningTopic, setAssigningTopic] = useState(null);
  const [topicError, setTopicError] = useState("");
  const [topicLoading, setTopicLoading] = useState(false);

  const token = localStorage.getItem("token");
  const auth = () => ({ Authorization: `Bearer ${token}` });
  const authJson = () => ({ ...auth(), "Content-Type": "application/json" });

  useEffect(() => {
    fetchClassInfo();
    fetchGroupSummaries();
    fetchGroups();
    fetchClassStudents(0);
  }, [classId]);

  const fetchClassInfo = async () => {
    const res = await fetch(CLASS_API, { headers: auth() });
    const data = await res.json();
    const classes = data.data || [];
    setClassInfo(classes.find(c => String(c.classId) === String(classId)) || null);
  };

  //  Overview tab
  const fetchGroupSummaries = async () => {
    setOverviewLoading(true);
    const res = await fetch(`/api/lecturer/classes/${classId}/groups`, { headers: auth() });
    const data = await res.json();
    setGroupSummaries(data.data || []);
    setOverviewLoading(false);
  };

  const fetchClassStudents = async (p = 0, search = studentSearch, hg = hasGroupFilter) => {
    setStudentsLoading(true);
    const params = new URLSearchParams({ page: p, size: 10 });
    if (search.trim()) params.set("search", search.trim());
    if (hg !== "") params.set("hasGroup", hg);
    const res = await fetch(`${CLASS_STUDENTS_API}/${classId}/students?${params}`, { headers: auth() });
    const data = await res.json();
    setClassStudents(data.data?.content || []);
    setStudentTotalPages(data.data?.totalPages || 0);
    setStudentPage(p);
    setStudentsLoading(false);
  };

  //  Groups ─
  const fetchGroups = async () => {
    setGroupsLoading(true);
    const res = await fetch(`${GROUP_API}?class_id=${classId}`, { headers: auth() });
    const data = await res.json();
    setGroups(data.data || []);
    setGroupsLoading(false);
  };

  const handleCreateGroup = async () => {
    if (!newGroupName.trim()) { setCreateError("Group name is required"); return; }
    setCreateError("");
    const res = await fetch(`${GROUP_API}/add`, {
      method: "POST", headers: authJson(),
      body: JSON.stringify({ classId: Number(classId), groupName: newGroupName.trim() }),
    });
    if (res.ok) { setNewGroupName(""); setShowCreateGroup(false); fetchGroups(); fetchGroupSummaries(); }
    else { const err = await res.json(); setCreateError(err.message || "Failed"); }
  };

  const handleUpdateGroup = async (groupId) => {
    if (!editGroupName.trim()) { setEditError("Group name is required"); return; }
    setEditError("");
    const res = await fetch(`${GROUP_API}/update/${groupId}`, {
      method: "PUT", headers: authJson(),
      body: JSON.stringify({ groupName: editGroupName.trim() }),
    });
    if (res.ok) { setEditGroupId(null); fetchGroups(); fetchGroupSummaries(); }
    else { const err = await res.json(); setEditError(err.message || "Failed"); }
  };

  const handleDeleteGroup = async (groupId) => {
    if (!window.confirm("Delete this group?")) return;
    await fetch(`${GROUP_API}/delete/${groupId}`, { method: "DELETE", headers: auth() });
    fetchGroups(); fetchGroupSummaries();
  };

  const handleChangeStatus = async (groupId, status) => {
    const res = await fetch(`${GROUP_API}/${groupId}/status`, {
      method: "PUT", headers: authJson(),
      body: JSON.stringify({ status }),
    });
    if (res.ok) { fetchGroups(); fetchGroupSummaries(); }
    else { const err = await res.json(); alert(err.message || "Failed"); }
  };

  //  Members 
  const openMemberModal = async (group) => {
    setSelectedGroup(group); setMemberKeyword("");
    await fetchMembers(group.groupId);
    await fetchEligibleStudents(group.groupId, "", 0);
    setShowMemberModal(true);
  };

  const fetchMembers = async (groupId) => {
    const res = await fetch(`${MEMBER_API}/${groupId}/members`, { headers: auth() });
    const data = await res.json();
    setMembers(data.data || []);
  };

  const fetchEligibleStudents = async (groupId, kw = "", p = 0) => {
    const res = await fetch(`/api/groups/${groupId}/members/search?keyword=${encodeURIComponent(kw)}&page=${p}&size=5`, { headers: auth() });
    const data = await res.json();
    setEligibleStudents(data.data?.content || []);
    setMemberTotalPages(data.data?.totalPages || 0);
    setMemberPage(p);
  };

  const handleAddMember = async (userId) => {
    const res = await fetch(`${MEMBER_API}/${selectedGroup.groupId}/members`, {
      method: "POST", headers: authJson(), body: JSON.stringify({ userId }),
    });
    if (res.ok) { await fetchMembers(selectedGroup.groupId); await fetchEligibleStudents(selectedGroup.groupId, memberKeyword, memberPage); }
    else { const err = await res.json(); alert("Failed: " + (err.message || "Unknown")); }
  };

  const handleRemoveMember = async (userId) => {
    if (!window.confirm("Remove this member?")) return;
    const res = await fetch(`${MEMBER_API}/${selectedGroup.groupId}/members/${userId}`, { method: "DELETE", headers: auth() });
    if (res.ok) { await fetchMembers(selectedGroup.groupId); await fetchEligibleStudents(selectedGroup.groupId, memberKeyword, memberPage); }
  };

  const handleSetLeader = async (userId) => {
    if (!window.confirm("Set as leader?")) return;
    const res = await fetch(`${MEMBER_API}/${selectedGroup.groupId}/leader`, {
      method: "PUT", headers: authJson(), body: JSON.stringify({ userId }),
    });
    if (res.ok) await fetchMembers(selectedGroup.groupId);
    else { const err = await res.json(); alert("Failed: " + (err.message || "Unknown")); }
  };

  //  Topic 
  const openTopicModal = async (group) => {
    setTopicGroup(group); setTopicSearch(""); setTopicError(""); setTopics([]);
    setShowTopicModal(true);
    setTopicLoading(true);
    // Lấy semesterId từ classInfo (class hiện tại)
    const semId = classInfo?.semesterId || "";
    const params = new URLSearchParams({ page: 0, size: 999 });
    if (semId) params.set("semester_id", semId);
    const res = await fetch(`/api/topics/list?${params}`, { headers: auth() });
    const data = await res.json();
    setTopics(data.data?.content || data.data || []);
    setTopicLoading(false);
  };

  const handleAssignTopic = async (topicId) => {
    setAssigningTopic(topicId); setTopicError("");
    const res = await fetch(`${GROUP_API}/${topicGroup.groupId}/topic`, {
      method: "PUT", headers: authJson(), body: JSON.stringify({ topicId }),
    });
    if (res.ok) { setShowTopicModal(false); fetchGroups(); fetchGroupSummaries(); }
    else { const err = await res.json(); setTopicError(err.message || "Failed"); }
    setAssigningTopic(null);
  };

  const filteredTopics = topics.filter(t =>
    t.topicName?.toLowerCase().includes(topicSearch.toLowerCase()) ||
    t.topicCode?.toLowerCase().includes(topicSearch.toLowerCase())
  );

  const getStatusStyle = (status) => {
    if (status === "OPEN") return { bg: "#f0fdf4", color: "#16a34a", border: "#bbf7d0" };
    if (status === "CLOSED") return { bg: "#fef2f2", color: "#dc2626", border: "#fecaca" };
    return { bg: "#f8fafc", color: "#94a3b8", border: "#e2e8f0" };
  };

  // --- Render helpers for better readability ---
  const renderLoadingSpinner = () => (
    <div className="lgm-loading">
      <span className="lgm-spinner" />
      <span>Loading...</span>
    </div>
  );

  const renderEmptyState = (message) => (
    <div className="lgm-empty-sm">{message}</div>
  );

  return (
    <div className="lgm-root">
      {/* Topbar */}
      <header className="lgm-topbar" role="banner">
        <div className="lgm-topbar-left">
          <button className="lgm-back-btn" onClick={() => navigate(-1)} aria-label="Back">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" aria-hidden>
              <path d="M19 12H5M12 5l-7 7 7 7" />
            </svg>
            Back
          </button>

          <div className="lgm-class-info" aria-live="polite">
            <span className="lgm-class-code">{classInfo?.classCode || `Class #${classId}`}</span>
            <span className="lgm-class-meta">{classInfo?.semesterCode} · {classInfo?.courseCode}</span>
          </div>
        </div>
        <img src={logo} alt="Logo" className="lcl-navbar-logo" />
        <div className="lgm-topbar-right">
          <div className="lgm-top-actions">
            <button className="lgm-btn-ghost" onClick={() => { fetchGroupSummaries(); fetchGroups(); fetchClassStudents(0); }}>
              Refresh
            </button>
            <LogoutButton />
          </div>
        </div>
      </header>

      {/* Tabs */}
      <nav className="lgm-tabs" role="tablist" aria-label="Group management tabs">
        {[
          { id: "overview", label: "Overview", icon: "📋" },
          { id: "groups", label: "Groups", icon: "👥" },
          { id: "students", label: "Students", icon: "🎓" },
        ].map(t => (
          <button
            key={t.id}
            role="tab"
            aria-selected={activeTab === t.id}
            className={`lgm-tab ${activeTab === t.id ? "lgm-tab-active" : ""}`}
            onClick={() => setActiveTab(t.id)}
          >
            <span className="lgm-tab-icon" aria-hidden>{t.icon}</span>
            <span>{t.label}</span>
          </button>
        ))}
      </nav>

      <main className="lgm-content" role="main">
        {/* OVERVIEW */}
        {activeTab === "overview" && (
          <section className="lgm-home" aria-labelledby="overview-title">
            <h2 id="overview-title" className="lgm-section-title">Groups Overview</h2>

            {overviewLoading ? (
              renderLoadingSpinner()
            ) : groupSummaries.length === 0 ? (
              renderEmptyState("No groups yet.")
            ) : (
              <div className="lgm-summary-list">
                {groupSummaries.map((g, i) => (
                  <article key={g.groupId} className="lgm-summary-card" aria-expanded={expandedGroupId === g.groupId}>
                    <header
                      className="lgm-summary-header"
                      onClick={() => setExpandedGroupId(expandedGroupId === g.groupId ? null : g.groupId)}
                      tabIndex={0}
                      role="button"
                      aria-expanded={expandedGroupId === g.groupId}
                      onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") setExpandedGroupId(expandedGroupId === g.groupId ? null : g.groupId); }}
                    >
                      <div className="lgm-summary-left">
                        <span className="lgm-summary-num">{i + 1}</span>
                        <div>
                          <div className="lgm-summary-name">{g.groupName}</div>
                          <div className="lgm-summary-topic">
                            {g.topicName ? <span className="lgm-topic-badge">{g.topicName}</span> : <span className="lgm-no-topic">No topic assigned</span>}
                          </div>
                        </div>
                      </div>

                      <div className="lgm-summary-right">
                        <span className="lgm-member-count">{g.members?.length || 0} members</span>
                        <div className="lgm-summary-actions">
                          <button
                            className="lgm-stats-btn"
                            onClick={(e) => { e.stopPropagation(); setSelectedGroupForStats(selectedGroupForStats === g.groupId ? null : g.groupId); }}
                            aria-pressed={selectedGroupForStats === g.groupId}
                          >
                            {selectedGroupForStats === g.groupId ? "Hide Stats" : "Stats"}
                          </button>
                          <span className="lgm-expand-icon" aria-hidden>{expandedGroupId === g.groupId ? "▲" : "▼"}</span>
                        </div>
                      </div>
                    </header>

                    {expandedGroupId === g.groupId && (
                      <div className="lgm-summary-members">
                        <table className="lgm-table" role="table" aria-label={`Members of ${g.groupName}`}>
                          <thead>
                            <tr><th>#</th><th>Student Code</th><th>Full Name</th><th>Role</th></tr>
                          </thead>
                          <tbody>
                            {(g.members || []).length === 0 ? (
                              <tr><td colSpan="4" className="lgm-empty-row">No members</td></tr>
                            ) : (
                              (g.members || []).map((m, mi) => (
                                <tr key={m.userId}>
                                  <td className="lgm-td-num">{mi + 1}</td>
                                  <td><span className="lgm-student-code">{m.studentCode || "—"}</span></td>
                                  <td>{m.fullName}</td>
                                  <td>{m.memberRole}</td>
                                </tr>
                              ))
                            )}
                          </tbody>
                        </table>
                      </div>
                    )}

                    {selectedGroupForStats === g.groupId && (
                      <div className="lgm-stats-section">
                        <CommitStats groupId={g.groupId} />
                        <RequirementDashboard groupId={g.groupId} />
                      </div>
                    )}
                  </article>
                ))}
              </div>
            )}
          </section>
        )}

        {/* GROUPS */}
        {activeTab === "groups" && (
          <section className="lgm-groups" aria-labelledby="groups-title">
            <h2 id="groups-title" className="lgm-section-title">Groups</h2>

            <div className="lgm-create-bar">
              {!showCreateGroup ? (
                <button className="lgm-btn-primary" onClick={() => setShowCreateGroup(true)}>+ New Group</button>
              ) : (
                <div className="lgm-create-form">
                  <input className="lgm-input" placeholder="Group name..." value={newGroupName}
                    onChange={e => setNewGroupName(e.target.value)}
                    onKeyDown={e => e.key === "Enter" && handleCreateGroup()} autoFocus />
                  <button className="lgm-btn-primary" onClick={handleCreateGroup}>Create</button>
                  <button className="lgm-btn-ghost" onClick={() => { setShowCreateGroup(false); setNewGroupName(""); setCreateError(""); }}>Cancel</button>
                  {createError && <span className="lgm-error-text" role="alert">{createError}</span>}
                </div>
              )}
            </div>

            {groupsLoading ? (
              renderLoadingSpinner()
            ) : groups.length === 0 ? (
              renderEmptyState("No groups yet.")
            ) : (
              <div className="lgm-card lgm-table-card">
                <table className="lgm-table" role="table" aria-label="Groups table">
                  <thead>
                    <tr><th>#</th><th>Group Name</th><th>Topic</th><th>Status</th><th>Actions</th></tr>
                  </thead>
                  <tbody>
                    {groups.map((g, i) => {
                      const st = getStatusStyle(g.status);
                      return (
                        <tr key={g.groupId}>
                          <td className="lgm-td-num">{i + 1}</td>
                          <td>
                            {editGroupId === g.groupId ? (
                              <div className="lgm-inline-edit">
                                <input className="lgm-input lgm-input-sm" value={editGroupName}
                                  onChange={e => setEditGroupName(e.target.value)}
                                  onKeyDown={e => e.key === "Enter" && handleUpdateGroup(g.groupId)} autoFocus />
                                <button className="lgm-btn-xs lgm-btn-primary" onClick={() => handleUpdateGroup(g.groupId)}>Save</button>
                                <button className="lgm-btn-xs lgm-btn-ghost" onClick={() => { setEditGroupId(null); setEditError(""); }}>Cancel</button>
                                {editError && <span className="lgm-error-text">{editError}</span>}
                              </div>
                            ) : <strong>{g.groupName}</strong>}
                          </td>
                          <td>{g.topicName ? <span className="lgm-topic-badge">{g.topicName}</span> : <span className="lgm-null">—</span>}</td>
                          <td><span className="lgm-status-badge" style={{ background: st.bg, color: st.color, border: `1px solid ${st.border}` }}>{g.status || "OPEN"}</span></td>
                          <td>
                            <div className="lgm-row-actions">
                              <button className="lgm-btn-action lgm-btn-members" onClick={() => openMemberModal(g)} disabled={g.status === "CLOSED"}>Members</button>
                              <button className="lgm-btn-action lgm-btn-topic" onClick={() => openTopicModal(g)} disabled={g.status === "CLOSED"}>Topic</button>
                              <button className="lgm-btn-action lgm-btn-edit2" onClick={() => { setEditGroupId(g.groupId); setEditGroupName(g.groupName); setEditError(""); }} disabled={g.status === "CLOSED"}>Edit</button>
                              {g.status !== "CLOSED"
                                ? <button className="lgm-btn-action lgm-btn-close" onClick={() => handleChangeStatus(g.groupId, "CLOSED")}>Close</button>
                                : <button className="lgm-btn-action lgm-btn-open" onClick={() => handleChangeStatus(g.groupId, "OPEN")}>Reopen</button>
                              }
                              <button className="lgm-btn-action lgm-btn-danger" onClick={() => handleDeleteGroup(g.groupId)}>Delete</button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        )}

        {/* STUDENTS */}
        {activeTab === "students" && (
          <section className="lgm-students" aria-labelledby="students-title">
            <h2 id="students-title" className="lgm-section-title">Students by Group</h2>

            <div className="lgm-filter-bar">
              <div className="lgm-search-wrap">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
                  <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
                </svg>
                <input
                  className="lgm-search-input"
                  placeholder="Search by group name or student name..."
                  value={studentSearch}
                  onChange={e => setStudentSearch(e.target.value)}
                  onKeyDown={e => e.key === "Enter" && fetchClassStudents(0)}
                />
              </div>

              <select
                className="lgm-select"
                value={hasGroupFilter}
                onChange={e => {
                  setHasGroupFilter(e.target.value);
                  fetchClassStudents(0, studentSearch, e.target.value);
                }}
              >
                <option value="">All Groups</option>
                <option value="true">Has Members</option>
                <option value="false">Empty Groups</option>
              </select>

              <button className="lgm-btn-primary lgm-btn-sm" onClick={() => fetchClassStudents(0)}>
                Search
              </button>
            </div>

            {studentsLoading ? (
              <div className="lgm-loading"><span className="lgm-spinner" /> Loading...</div>
            ) : groupSummaries.length === 0 ? (
              <div className="lgm-empty-sm">No groups available.</div>
            ) : (
              <div className="lgm-group-list">
                {groupSummaries
                  .filter(group => {
                    // Lọc theo từ khóa và bộ lọc "Có thành viên / Không có thành viên"
                    const matchesSearch =
                      studentSearch.trim() === "" ||
                      group.groupName.toLowerCase().includes(studentSearch.toLowerCase()) ||
                      (group.members || []).some(m => m.fullName.toLowerCase().includes(studentSearch.toLowerCase()));

                    if (hasGroupFilter === "true") return matchesSearch && (group.members?.length > 0);
                    if (hasGroupFilter === "false") return matchesSearch && (group.members?.length === 0);
                    return matchesSearch;
                  })
                  .map((group, idx) => (
                    <div key={group.groupId} className="lgm-group-card">
                      <div className="lgm-group-card-header">
                        <div className="lgm-group-name">
                          <span className="lgm-group-index">{idx + 1}</span>
                          {group.groupName}
                        </div>
                        <div className="lgm-group-stats">
                          <span className="lgm-member-count">{group.members?.length || 0} members</span>
                          {group.topicName && (
                            <span className="lgm-topic-badge-sm">{group.topicName}</span>
                          )}
                        </div>
                      </div>
                      <div className="lgm-group-members">
                        {(group.members || []).length === 0 ? (
                          <div className="lgm-empty-members">No members in this group</div>
                        ) : (
                          <table className="lgm-table lgm-table-compact">
                            <thead>
                              <tr>
                                <th>#</th>
                                <th>Student Code</th>
                                <th>Full Name</th>
                                <th>Role</th>
                              </tr>
                            </thead>
                            <tbody>
                              {(group.members || []).map((member, mi) => (
                                <tr key={member.userId}>
                                  <td className="lgm-td-num">{mi + 1}</td>
                                  <td>
                                    <span className="lgm-student-code">{member.studentCode || "—"}</span>
                                  </td>
                                  <td>{member.fullName}</td>
                                  <td>
                                    <span className={`lgm-role-badge ${member.memberRole === "LEADER" ? "lgm-leader" : "lgm-member"}`}>
                                      {member.memberRole}
                                    </span>
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        )}
                      </div>
                    </div>
                  ))}
              </div>
            )}
          </section>
        )}
      </main>

      {/*  Members Modal  */}
      {showMemberModal && (
        <div className="lgm-modal-overlay" onClick={() => setShowMemberModal(false)}>
          <div className="lgm-modal lgm-modal-xl" onClick={e => e.stopPropagation()}>
            <div className="lgm-modal-header">
              <div>
                <div className="lgm-modal-title">Members — {selectedGroup?.groupName}</div>
                <div className="lgm-modal-subtitle">{members.length} member(s)</div>
              </div>
              <button className="lgm-modal-close" onClick={() => setShowMemberModal(false)}>×</button>
            </div>
            <div className="lgm-modal-body">
              <div className="lgm-section-label">Current Members</div>
              <table className="lgm-table">
                <thead><tr><th>#</th><th>Full Name</th><th>Username</th><th>Email</th><th>Role</th><th>Actions</th></tr></thead>
                <tbody>
                  {members.length === 0 ? (
                    <tr><td colSpan="6" className="lgm-empty-row">No members yet</td></tr>
                  ) : (
                    members.map((m, i) => (
                      <tr key={m.userId}>
                        <td className="lgm-td-num">{i + 1}</td>
                        <td>{m.fullName}</td>
                        <td><span className="lgm-username">{m.username}</span></td>
                        <td>{m.email}</td>
                        <td><span className={`lgm-role-badge ${m.memberRole === "LEADER" ? "lgm-leader" : "lgm-member"}`}>{m.memberRole}</span></td>
                        <td>
                          <div className="lgm-row-actions">
                            {m.memberRole !== "LEADER" && <button className="lgm-btn-action lgm-btn-leader" onClick={() => handleSetLeader(m.userId)}>Set Leader</button>}
                            <button className="lgm-btn-action lgm-btn-danger" onClick={() => handleRemoveMember(m.userId)}>Remove</button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>

              <div className="lgm-section-label" style={{ marginTop: 18 }}>Add Student</div>
              <input className="lgm-input lgm-search-input" placeholder="Search by name or username..."
                value={memberKeyword}
                onChange={e => { setMemberKeyword(e.target.value); fetchEligibleStudents(selectedGroup.groupId, e.target.value.trim(), 0); }} />
              <table className="lgm-table" style={{ marginTop: 8 }}>
                <thead><tr><th>#</th><th>Full Name</th><th>Username</th><th>Email</th><th>Action</th></tr></thead>
                <tbody>
                  {eligibleStudents.length === 0 ? (
                    <tr><td colSpan="5" className="lgm-empty-row">No eligible students</td></tr>
                  ) : (
                    eligibleStudents.map((s, i) => (
                      <tr key={s.userId}>
                        <td className="lgm-td-num">{i + 1}</td>
                        <td>{s.fullName}</td>
                        <td><span className="lgm-username">{s.username}</span></td>
                        <td>{s.email}</td>
                        <td><button className="lgm-btn-action lgm-btn-add" onClick={() => handleAddMember(s.userId)}>Add</button></td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
              {memberTotalPages > 1 && (
                <div className="lgm-pagination" style={{ marginTop: 8 }}>
                  {Array.from({ length: memberTotalPages }, (_, i) => (
                    <button key={i} className={`lgm-page-num ${i === memberPage ? "active" : ""}`}
                      onClick={() => fetchEligibleStudents(selectedGroup.groupId, memberKeyword, i)}>{i + 1}</button>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/*  Topic Modal  */}
      {showTopicModal && (
        <div className="lgm-modal-overlay" onClick={() => setShowTopicModal(false)}>
          <div className="lgm-modal lgm-modal-lg" onClick={e => e.stopPropagation()}>
            <div className="lgm-modal-header">
              <div>
                <div className="lgm-modal-title">Assign Topic — {topicGroup?.groupName}</div>
                {topicGroup?.topicName && <div className="lgm-modal-subtitle">Current: {topicGroup.topicName}</div>}
              </div>
              <button className="lgm-modal-close" onClick={() => setShowTopicModal(false)}>×</button>
            </div>
            <div className="lgm-modal-body">
              {topicError && <div className="lgm-error-banner">{topicError}</div>}
              <div className="lgm-search-wrap" style={{ marginBottom: 12 }}>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
                </svg>
                <input className="lgm-search-input" placeholder="Search topics..." value={topicSearch}
                  onChange={e => setTopicSearch(e.target.value)} autoFocus />
              </div>
              <table className="lgm-table">
                <thead><tr><th>Code</th><th>Name</th><th>Action</th></tr></thead>
                <tbody>
                  {topicLoading ? (
                    <tr><td colSpan="3" className="lgm-empty-row"><span className="lgm-spinner" /> Loading topics...</td></tr>
                  ) : filteredTopics.length === 0 ? (
                    <tr><td colSpan="3" className="lgm-empty-row">
                      {classInfo?.semesterId
                        ? `No topics found for this semester (${classInfo.semesterCode || classInfo.semesterId})`
                        : "No topics found"}
                    </td></tr>
                  ) : (
                    filteredTopics.map(t => (
                      <tr key={t.topicId}>
                        <td><span className="lgm-topic-code">{t.topicCode}</span></td>
                        <td>{t.topicName}</td>
                        <td>
                          <button className="lgm-btn-action lgm-btn-topic"
                            onClick={() => handleAssignTopic(t.topicId)}
                            disabled={assigningTopic === t.topicId}>
                            {assigningTopic === t.topicId ? <span className="lgm-spinner-sm" /> : "Assign"}
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}