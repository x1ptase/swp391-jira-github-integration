import { useEffect, useState, useMemo, useRef, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import "./GroupsTab.css";

const PAGE_SIZE = 8;

// ── Helpers ───────────────────────────────────────────────────────────────────

function groupInitials(name = "") {
  const m = name.match(/\d+/);
  return m ? `G${m[0]}` : name.slice(0, 2).toUpperCase();
}

function groupAvatarColor(name = "") {
  const colors = [
    { bg: "#dbeafe", text: "#1d4ed8" },
    { bg: "#e0e7ff", text: "#4338ca" },
    { bg: "#d1fae5", text: "#065f46" },
    { bg: "#fce7f3", text: "#be185d" },
    { bg: "#fef3c7", text: "#92400e" },
    { bg: "#ede9fe", text: "#5b21b6" },
  ];
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
  return colors[Math.abs(hash) % colors.length];
}

function HealthBadge({ health }) {
  const map = {
    HEALTHY: { cls: "gt-health-healthy", dot: "#22c55e", label: "HEALTHY" },
    WARNING: { cls: "gt-health-warning", dot: "#a855f7", label: "WARNING" },
    CRITICAL: { cls: "gt-health-critical", dot: "#ef4444", label: "CRITICAL" },
  };
  const h = map[health?.toUpperCase()] || { cls: "gt-health-default", dot: "#94a3b8", label: health || "—" };
  return (
    <span className={`gt-health-badge ${h.cls}`}>
      <span className="gt-health-dot" style={{ background: h.dot }} />
      {h.label}
    </span>
  );
}

function StatusBadge({ status }) {
  const map = {
    OPEN: "gt-status-open",
    CLOSED: "gt-status-closed",
    ACTIVE: "gt-status-active",
  };
  const cls = map[status?.toUpperCase()] || "gt-status-default";
  return <span className={`gt-status-badge ${cls}`}>{status || "—"}</span>;
}

// ── Members Modal (view + add + remove + set leader) ──────────────────────────

function MembersModal({ group, onClose }) {
  const token = localStorage.getItem("token");
  const groupId = group.groupId;

  const [members, setMembers] = useState([]);
  const [membersLoading, setMembersLoading] = useState(true);
  const [keyword, setKeyword] = useState("");
  const [eligible, setEligible] = useState([]);
  const [eligiblePage, setEligiblePage] = useState(0);
  const [eligibleTotal, setEligibleTotal] = useState(1);
  const [eligibleLoading, setEligibleLoading] = useState(false);

  const fetchMembers = useCallback(async () => {
    setMembersLoading(true);
    try {
      const res = await fetch(`/api/groups/${groupId}/members`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await res.json();
      setMembers(data.data || []);
    } catch (e) {
      console.error("Failed to fetch members", e);
    } finally {
      setMembersLoading(false);
    }
  }, [groupId, token]);

  const fetchEligible = useCallback(async (kw = keyword, p = 0) => {
    setEligibleLoading(true);
    try {
      const params = new URLSearchParams({ page: p, size: 10 });
      if (kw.trim()) params.set("keyword", kw.trim());
      const res = await fetch(`/api/groups/${groupId}/members/search?${params}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await res.json();
      const payload = data.data;
      setEligible(payload?.content || payload || []);
      setEligibleTotal(payload?.totalPages ?? 1);
      setEligiblePage(p);
    } catch (e) {
      console.error("Failed to search eligible", e);
    } finally {
      setEligibleLoading(false);
    }
  }, [groupId, token]);

  useEffect(() => {
    fetchMembers();
    fetchEligible("", 0);
  }, [fetchMembers, fetchEligible]);

  const handleAdd = async (userId) => {
    try {
      const res = await fetch(`/api/groups/${groupId}/members`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify({ userId }),
      });
      if (res.ok) {
        fetchMembers();
        fetchEligible(keyword, eligiblePage);
      }
    } catch (e) {
      console.error("Failed to add member", e);
    }
  };

  const handleRemove = async (userId) => {
    if (!window.confirm("Remove this member from the group?")) return;
    try {
      const res = await fetch(`/api/groups/${groupId}/members/${userId}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.ok) fetchMembers();
    } catch (e) {
      console.error("Failed to remove member", e);
    }
  };

  const handleSetLeader = async (userId) => {
    try {
      const res = await fetch(`/api/groups/${groupId}/leader`, {
        method: "PUT",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify({ userId }),
      });
      if (res.ok) fetchMembers();
    } catch (e) {
      console.error("Failed to set leader", e);
    }
  };

  return (
    <div className="gt-modal-overlay" onClick={onClose}>
      <div className="gt-modal gt-modal-xl" onClick={(e) => e.stopPropagation()}>
        <div className="gt-modal-header">
          <div>
            <h3 className="gt-modal-title">Members — {group.groupName}</h3>
            <p className="gt-modal-sub">{members.length} member(s) in this group</p>
          </div>
          <button className="gt-modal-close" onClick={onClose}>✕</button>
        </div>

        <div className="gt-modal-body">
          {/* Current members */}
          <div className="gt-modal-section-label">Current Members</div>
          {membersLoading ? (
            <div className="gt-modal-loading"><div className="gt-spinner" /> Loading...</div>
          ) : (
            <table className="gt-modal-table">
              <thead>
                <tr>
                  <th>#</th><th>Full Name</th><th>Student Code</th>
                  <th>Role</th><th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {members.length === 0 ? (
                  <tr><td colSpan={5} className="gt-modal-empty-row">No members yet</td></tr>
                ) : members.map((m, i) => (
                  <tr key={m.userId || i}>
                    <td>{i + 1}</td>
                    <td className="gt-modal-name">{m.fullName || m.username}</td>
                    <td>{m.studentCode || m.code || "—"}</td>
                    <td>
                      <span className={`gt-role-badge ${(m.memberRole || m.role) === "LEADER" ? "gt-role-leader" : "gt-role-member"}`}>
                        {m.memberRole || m.role || "MEMBER"}
                      </span>
                    </td>
                    <td>
                      <div className="gt-modal-row-actions">
                        {(m.memberRole || m.role) !== "LEADER" && (
                          <button className="gt-modal-action gt-modal-action-leader"
                            onClick={() => handleSetLeader(m.userId)}>
                            Set Leader
                          </button>
                        )}
                        <button className="gt-modal-action gt-modal-action-remove"
                          onClick={() => handleRemove(m.userId)}>
                          Remove
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {/* Add student section */}
          <div className="gt-modal-section-label" style={{ marginTop: "1.25rem" }}>Add Student</div>
          <div className="gt-modal-search-wrap">
            <input
              className="gt-modal-search-input"
              placeholder="Search by name or username..."
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value);
                fetchEligible(e.target.value, 0);
              }}
            />
          </div>
          {eligibleLoading ? (
            <div className="gt-modal-loading"><div className="gt-spinner" /> Searching...</div>
          ) : (
            <table className="gt-modal-table" style={{ marginTop: "0.5rem" }}>
              <thead>
                <tr><th>#</th><th>Full Name</th><th>Student Code</th><th>Email</th><th>Action</th></tr>
              </thead>
              <tbody>
                {eligible.length === 0 ? (
                  <tr><td colSpan={5} className="gt-modal-empty-row">No eligible students found</td></tr>
                ) : eligible.map((s, i) => (
                  <tr key={s.userId || i}>
                    <td>{i + 1}</td>
                    <td className="gt-modal-name">{s.fullName || s.username}</td>
                    <td>{s.studentCode || "—"}</td>
                    <td className="gt-modal-email">{s.email || "—"}</td>
                    <td>
                      <button className="gt-modal-action gt-modal-action-add"
                        onClick={() => handleAdd(s.userId)}>
                        Add
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {eligibleTotal > 1 && (
            <div className="gt-modal-pagination">
              {Array.from({ length: eligibleTotal }, (_, i) => i).map((p) => (
                <button
                  key={p}
                  className={`gt-modal-page-btn ${p === eligiblePage ? "active" : ""}`}
                  onClick={() => fetchEligible(keyword, p)}
                >
                  {p + 1}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ── Topic Modal ───────────────────────────────────────────────────────────────

function TopicModal({ group, onClose, onAssigned }) {
  const token = localStorage.getItem("token");
  const groupId = group.groupId;

  const [topics, setTopics] = useState([]);
  const [loadingTopics, setLoadingTopics] = useState(true);
  const [topicSearch, setTopicSearch] = useState("");
  const [assigningId, setAssigningId] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchTopics = async () => {
      try {
        const res = await fetch("/api/topics/list?size=1000", { headers: { Authorization: `Bearer ${token}` } });
        const data = await res.json();
        const payload = data.data;
        // Could be { content: [...] } or plain array
        const list = payload?.content ? payload.content : (Array.isArray(payload) ? payload : []);
        setTopics(list);
      } catch (e) {
        console.error("Failed to fetch topics", e);
      } finally {
        setLoadingTopics(false);
      }
    };
    fetchTopics();
  }, [token]);

  const filteredTopics = topics.filter(t =>
    t.topicCode?.toLowerCase().includes(topicSearch.toLowerCase()) ||
    t.topicName?.toLowerCase().includes(topicSearch.toLowerCase())
  );

  const handleAssign = async (topicId) => {
    setAssigningId(topicId);
    setError("");
    try {
      const res = await fetch(`/api/student_group/${groupId}/topic`, {
        method: "PUT",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify({ topicId }),
      });
      const data = await res.json();
      if (res.ok) {
        onAssigned(groupId, data.data);
        onClose();
      } else {
        setError(data.message || "Failed to assign topic.");
      }
    } catch (e) {
      setError("Network error. Please try again.");
    } finally {
      setAssigningId(null);
    }
  };

  return (
    <div className="gt-modal-overlay" onClick={onClose}>
      <div className="gt-modal gt-modal-xl" onClick={(e) => e.stopPropagation()}>
        <div className="gt-modal-header">
          <div>
            <h3 className="gt-modal-title">Assign Topic — {group.groupName}</h3>
            <p className="gt-modal-sub">
              {group.topicName
                ? <>Current topic: <strong>{group.topicName}</strong></>
                : "No topic assigned yet."}
            </p>
          </div>
          <button className="gt-modal-close" onClick={onClose}>✕</button>
        </div>
        <div className="gt-modal-body">
          {error && <div className="gt-field-error" style={{ marginBottom: "1rem" }}>{error}</div>}

          <div className="gt-modal-search-wrap" style={{ marginBottom: "1rem" }}>
            <input
              className="gt-modal-search-input"
              placeholder="Search topics by code or name..."
              value={topicSearch}
              onChange={(e) => setTopicSearch(e.target.value)}
              autoFocus
            />
          </div>

          {loadingTopics ? (
            <div className="gt-modal-loading"><div className="gt-spinner" /> Loading topics...</div>
          ) : (
            <table className="gt-modal-table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th className="gt-th-left">Name</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {filteredTopics.length === 0 ? (
                  <tr><td colSpan={3} className="gt-modal-empty-row">No topics found</td></tr>
                ) : (
                  filteredTopics.map((t) => (
                    <tr key={t.topicId}>
                      <td style={{ fontFamily: "monospace", fontSize: "0.85rem", fontWeight: 600 }}>{t.topicCode}</td>
                      <td style={{ textAlign: "left" }}>{t.topicName}</td>
                      <td>
                        <div className="gt-modal-row-actions">
                          <button
                            className="gt-modal-action gt-modal-action-add"
                            onClick={() => handleAssign(t.topicId)}
                            disabled={assigningId === t.topicId}
                          >
                            {assigningId === t.topicId ? "..." : "Assign"}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}


// ── Status Dropdown (portal-like, fixed to button) ────────────────────────────

const GROUP_STATUSES = ["OPEN", "CLOSED"];

function StatusMenu({ group, onClose, onStatusChanged }) {
  const token = localStorage.getItem("token");
  const ref = useRef();

  useEffect(() => {
    const handler = (e) => {
      if (ref.current && !ref.current.contains(e.target)) onClose();
    };
    // slight delay so the click that opened this menu doesn't immediately close it
    const t = setTimeout(() => document.addEventListener("mousedown", handler), 10);
    return () => { clearTimeout(t); document.removeEventListener("mousedown", handler); };
  }, [onClose]);

  const changeStatus = async (status) => {
    onClose();
    try {
      const res = await fetch(`/api/student_group/${group.groupId}/status`, {
        method: "PUT",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify({ status }),
      });
      if (res.ok) onStatusChanged(group.groupId, status);
    } catch (e) {
      console.error("Failed to change status", e);
    }
  };

  return (
    <div className="gt-context-menu" ref={ref}>
      <div className="gt-context-header">Change Status</div>
      {GROUP_STATUSES.map((s) => (
        <button
          key={s}
          className={`gt-context-item ${group.status === s ? "active" : ""}`}
          onClick={() => changeStatus(s)}
        >
          {s}
          {group.status === s && <span className="gt-context-check">✓</span>}
        </button>
      ))}
    </div>
  );
}

// ── Main GroupsTab ────────────────────────────────────────────────────────────

export default function GroupsTab({ classId, classInfo }) {
  const navigate = useNavigate();
  const token = localStorage.getItem("token");

  const [groups, setGroups] = useState([]);
  const [healthMap, setHealthMap] = useState({});
  const [loading, setLoading] = useState(true);

  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [page, setPage] = useState(1);

  const [membersModal, setMembersModal] = useState(null);  // group object
  const [topicModal, setTopicModal] = useState(null);  // group object
  const [menuOpen, setMenuOpen] = useState(null);  // groupId number

  const [showCreateGroup, setShowCreateGroup] = useState(false);
  const [newGroupName, setNewGroupName] = useState("");
  const [createError, setCreateError] = useState("");
  const [creating, setCreating] = useState(false);

  const classCode = classInfo?.classCode || "Class";

  // ── Fetch ────────────────────────────────────────────────────────────────────
  const fetchGroups = useCallback(async () => {
    setLoading(true);
    try {
      const [gRes, hRes] = await Promise.all([
        fetch(`/api/student_group?class_id=${classId}`, { headers: { Authorization: `Bearer ${token}` } }),
        fetch(`/api/classes/${classId}/monitoring/groups`, { headers: { Authorization: `Bearer ${token}` } }),
      ]);
      const gData = await gRes.json();
      const hData = await hRes.json();

      setGroups(gData.data || []);

      const hm = {};
      (hData.data || []).forEach((h) => { hm[h.groupId] = h.healthStatus; });
      setHealthMap(hm);
    } catch (err) {
      console.error("Failed to fetch groups", err);
    } finally {
      setLoading(false);
    }
  }, [classId, token]);

  useEffect(() => { fetchGroups(); }, [fetchGroups]);

  // ── Filter ───────────────────────────────────────────────────────────────────
  const filtered = useMemo(() => {
    return groups.filter((g) => {
      const q = searchQuery.toLowerCase();
      const matchSearch = !q ||
        g.groupName?.toLowerCase().includes(q) ||
        g.topicName?.toLowerCase().includes(q);
      const matchStatus = statusFilter === "all" || g.status?.toUpperCase() === statusFilter;
      return matchSearch && matchStatus;
    });
  }, [groups, searchQuery, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const paginated = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  // ── Callbacks ────────────────────────────────────────────────────────────────
  const handleTopicAssigned = (groupId, updatedGroup) => {
    setGroups((prev) => prev.map((g) =>
      g.groupId === groupId
        ? { ...g, topicId: updatedGroup?.topicId, topicName: updatedGroup?.topicName }
        : g
    ));
  };

  const handleStatusChanged = (groupId, newStatus) => {
    setGroups((prev) => prev.map((g) =>
      g.groupId === groupId ? { ...g, status: newStatus } : g
    ));
  };

  const handleCreateGroup = async () => {
    if (!newGroupName.trim()) { setCreateError("Group name is required."); return; }
    setCreating(true);
    setCreateError("");
    try {
      const res = await fetch("/api/student_group/add", {
        method: "POST",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify({ classId: Number(classId), groupName: newGroupName }),
      });
      const data = await res.json();
      if (res.ok) {
        setShowCreateGroup(false);
        setNewGroupName("");
        fetchGroups(); // Refresh list to get new group (with its IDs, health, etc.)
      } else {
        setCreateError(data.message || "Failed to create group.");
      }
    } catch (e) {
      setCreateError("Network error. Please try again.");
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="gt-container">
      {/* Header */}
      <div className="gt-page-header">
        <div>
          <h2 className="gt-page-title">{classCode} - Groups</h2>
          <p className="gt-page-sub">Manage student groups in this class.</p>
        </div>

        {!showCreateGroup ? (
          <button className="gt-new-group-btn" onClick={() => setShowCreateGroup(true)}>
            + New Group
          </button>
        ) : (
          <div>
            <div className="gt-create-bar">
              <input
                className="gt-field-input"
                style={{ width: "220px", padding: "0.5rem 0.75rem" }}
                placeholder="Group name..."
                value={newGroupName}
                onChange={(e) => { setNewGroupName(e.target.value); setCreateError(""); }}
                onKeyDown={(e) => e.key === "Enter" && handleCreateGroup()}
                autoFocus
              />
              <button
                className="gt-btn-primary"
                style={{ padding: "0.5rem 1rem" }}
                onClick={handleCreateGroup}
                disabled={creating}
              >
                {creating ? "..." : "Save"}
              </button>
              <button
                className="gt-btn-cancel"
                style={{ padding: "0.5rem 0.8rem" }}
                onClick={() => { setShowCreateGroup(false); setNewGroupName(""); setCreateError(""); }}
                disabled={creating}
              >
                Cancel
              </button>
            </div>
            {createError && <p className="gt-field-error" style={{ marginTop: "0.25rem", textAlign: "right" }}>{createError}</p>}
          </div>
        )}
      </div>

      {/* Filter Bar */}
      <div className="gt-filter-bar">
        <div className="gt-search-wrap">
          <svg className="gt-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
          </svg>
          <input
            className="gt-search-input"
            placeholder="Search group name or topic..."
            value={searchQuery}
            onChange={(e) => { setSearchQuery(e.target.value); setPage(1); }}
          />
        </div>
        <select
          className="gt-status-select"
          value={statusFilter}
          onChange={(e) => { setStatusFilter(e.target.value); setPage(1); }}
        >
          <option value="all">All Status</option>
          <option value="OPEN">OPEN</option>
          <option value="CLOSED">CLOSED</option>
        </select>
      </div>

      {/* Table */}
      <div className="gt-table-card">
        {loading ? (
          <div className="gt-loading"><div className="gt-spinner" /> Loading groups...</div>
        ) : (
          <>
            <table className="gt-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th className="gt-th-left">GROUP</th>
                  <th className="gt-th-left">TOPIC</th>
                  <th>MEMBERS</th>
                  <th>STATUS</th>
                  <th>HEALTH</th>
                  <th>ACTION</th>
                </tr>
              </thead>
              <tbody>
                {paginated.map((g, idx) => {
                  const initials = groupInitials(g.groupName);
                  const color = groupAvatarColor(g.groupName);
                  const health = healthMap[g.groupId] || g.healthStatus;
                  const num = String((page - 1) * PAGE_SIZE + idx + 1).padStart(2, "0");
                  const isMenuOpen = menuOpen === g.groupId;

                  return (
                    <tr key={g.groupId ?? idx}>
                      <td className="gt-td-num">{num}</td>
                      <td className="gt-td-group">
                        <div className="gt-group-avatar" style={{ background: color.bg, color: color.text }}>
                          {initials}
                        </div>
                        <span className="gt-group-name">{g.groupName}</span>
                      </td>
                      <td className="gt-td-topic">
                        {g.topicName
                          ? <span className="gt-topic-text">{g.topicName}</span>
                          : <span className="gt-topic-empty">Not assigned</span>}
                      </td>
                      <td className="gt-td-members">
                        {g.memberCount != null ? `${g.memberCount} members` : "—"}
                      </td>
                      <td><StatusBadge status={g.status} /></td>
                      <td><HealthBadge health={health} /></td>
                      <td>
                        <div className="gt-actions">
                          <button
                            className="gt-action-view"
                            onClick={() => navigate(`/lecturer/groups/${g.groupId}/detail`)}
                          >
                            View
                          </button>
                          <button className="gt-action-link" onClick={() => setMembersModal(g)}>Members</button>
                          <button className="gt-action-link" onClick={() => setTopicModal(g)}>Topic</button>
                          <div className="gt-menu-wrap">
                            <button
                              className={`gt-action-dots ${isMenuOpen ? "active" : ""}`}
                              onClick={(e) => {
                                e.stopPropagation();
                                setMenuOpen(isMenuOpen ? null : g.groupId);
                              }}
                            >
                              ⋮
                            </button>
                            {isMenuOpen && (
                              <StatusMenu
                                group={g}
                                onClose={() => setMenuOpen(null)}
                                onStatusChanged={handleStatusChanged}
                              />
                            )}
                          </div>
                        </div>
                      </td>
                    </tr>
                  );
                })}
                {paginated.length === 0 && (
                  <tr><td colSpan={7} className="gt-empty">No groups found.</td></tr>
                )}
              </tbody>
            </table>

            <div className="gt-table-footer">
              <span className="gt-footer-count">Showing {paginated.length} of {filtered.length} groups</span>
              <div className="gt-pagination">
                <button className="gt-page-btn" disabled={page <= 1} onClick={() => setPage((p) => p - 1)}>‹</button>
                {Array.from({ length: totalPages }, (_, i) => i + 1).map((p) => (
                  <button
                    key={p}
                    className={`gt-page-btn ${p === page ? "active" : ""}`}
                    onClick={() => setPage(p)}
                  >
                    {p}
                  </button>
                ))}
                <button className="gt-page-btn" disabled={page >= totalPages} onClick={() => setPage((p) => p + 1)}>›</button>
              </div>
            </div>
          </>
        )}
      </div>

      {/* Modals */}
      {membersModal && (
        <MembersModal group={membersModal} onClose={() => setMembersModal(null)} />
      )}
      {topicModal && (
        <TopicModal
          group={topicModal}
          onClose={() => setTopicModal(null)}
          onAssigned={handleTopicAssigned}
        />
      )}
    </div>
  );
}
