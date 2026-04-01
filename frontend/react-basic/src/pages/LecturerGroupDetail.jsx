import { useEffect, useState, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import "./LecturerGroupDetail.css";
import "./RequirementDashboard.css";
import CommitStats from "./CommitStats";
import StoryDashboard from "./StoryDashboard";

const STATUS_COLOR = {
  DONE: "#22c55e",
  "IN PROGRESS": "#3b82f6",
  "IN_PROGRESS": "#3b82f6",
  TODO: "#6b7280",
  "TO DO": "#6b7280",
  ACTIVE: "#3b82f6",
};

const PRIORITY_COLOR = {
  HIGHEST: "#ef4444",
  HIGH: "#f97316",
  MEDIUM: "#f59e0b",
  LOW: "#3b82f6",
  LOWEST: "#6b7280",
};

function EpicBadge({ label }) {
  return (
    <span className="rd-epic-key">{label || "—"}</span>
  );
}

function Badge({ label, colorMap, fallback = "#94a3b8" }) {
  const color = colorMap?.[label?.toUpperCase()] || fallback;
  return (
    <span className="rd-badge" style={{ "--badge-color": color }}>
      {label || "—"}
    </span>
  );
}

function ProgressBar({ done, total }) {
  const pct = total > 0 ? Math.round((done / total) * 100) : 0;
  const color = pct === 100 ? "#22c55e" : pct >= 50 ? "#3b82f6" : "#f59e0b";
  return (
    <div className="rd-progress-wrap">
      <div className="rd-progress-bar">
        <div className="rd-progress-fill" style={{ width: `${pct}%`, "--fill-color": color }} />
      </div>
      <span className="rd-progress-label">
        {done}/{total} <span className="rd-progress-pct">({pct}%)</span>
      </span>
    </div>
  );
}

const formatDateVN = (value) => {
  if (!value) return "No record";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return "No record";
  return d.toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
};

export default function LecturerGroupDetail() {
  const { groupId } = useParams();
  const navigate = useNavigate();
  const token = localStorage.getItem("token");

  // States cho dữ liệu chung
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  // States cho Jira Requirements
  const [requirements, setRequirements] = useState([]);
  const [reqLoading, setReqLoading] = useState(false);
  const [reqError, setReqError] = useState("");
  const [selectedEpic, setSelectedEpic] = useState(null);

  // Filter states
  const [keyword, setKeyword] = useState("");
  const [statusId, setStatusId] = useState("");
  const [priorityId, setPriorityId] = useState("");

  // 2. Fetch danh sách Epic/Requirements với filter params
  const fetchRequirements = useCallback(async (kw = keyword, sid = statusId, pid = priorityId) => {
    setReqLoading(true);
    setReqError("");
    try {
      const params = new URLSearchParams();
      params.set("page", "0");
      params.set("size", "50");
      if (sid) params.set("statusId", sid);
      if (pid) params.set("priorityId", pid);
      if (kw.trim()) params.set("keyword", kw.trim());

      const res = await fetch(`/api/groups/${groupId}/requirements?${params}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      const result = await res.json();
      const pageData = result.data;
      if (pageData && Array.isArray(pageData.content)) {
        setRequirements(pageData.content);
      } else if (Array.isArray(pageData)) {
        setRequirements(pageData);
      } else {
        setRequirements([]);
      }
    } catch (err) {
      console.error("Fetch requirements error:", err);
      setReqError("Failed to load requirements.");
      setRequirements([]);
    } finally {
      setReqLoading(false);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [groupId, token]);

  const handleReqSearch = () => fetchRequirements(keyword, statusId, priorityId);
  const handleReqKeyDown = (e) => { if (e.key === "Enter") handleReqSearch(); };

  // 1. Fetch thông tin tổng quan của Group (Stats, Members...)
  const fetchDetail = useCallback(async () => {
    setLoading(true);
    try {
      const toDate = new Date().toISOString();
      const fromDate = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString();
      const res = await fetch(
        `/api/groups/${groupId}/monitoring/detail?fromDate=${fromDate}&toDate=${toDate}`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      const result = await res.json();
      setData(result.data);
    } catch (err) {
      console.error("Fetch detail error:", err);
    } finally {
      setLoading(false);
    }
  }, [groupId, token]);

  useEffect(() => {
    fetchDetail();
    fetchRequirements("", "", "");
  }, [fetchDetail, fetchRequirements]);

  if (loading) return <div className="lgd-loading">Loading group details...</div>;
  if (!data) return <div className="lgd-error">Group not found or error occurred.</div>;

  const { summary, members } = data;

  return (
    <div className="lgd-root">
      {/* Header & Breadcrumb */}
      <header className="lgd-header">
        <div className="lgd-header-left">
          <button className="lgd-back-circle" onClick={() => navigate(-1)}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
          </button>
          <div className="lgd-breadcrumb">
            <span>{data.classCode}</span> &gt; <span>{data.groupName}</span>
            <div className="lgd-page-title-row">
              <h1 className="lgd-page-title">Group Details</h1>
              <span className="lgd-topic-tag">{data.topicName || "No Topic"}</span>
              <span className={`lgd-status-pill ${data.health.toLowerCase()}`}>{data.health} STATUS</span>
            </div>
          </div>
        </div>
      </header>

      <main className="lgd-container">
        {/* Warning/Summary Banner */}
        <div className="lgd-warning-box">
          <h2>Group Summary</h2>
          <p>
            {data.reasons?.length > 0 
              ? data.reasons.map((r, i) => <span key={i}> {r}.</span>)
              : "Group is performing normally based on current metrics."}
          </p>
        </div>

        {/* Stats Grid */}
        <div className="lgd-stats-grid">
          <div className="lgd-stat-card">
            <div className="lgd-stat-content">
              <span className="lgd-stat-label">ACTIVE MEMBERS</span>
              <span className="lgd-stat-value">{summary.activeMembers} <small>/{summary.totalMembers}</small></span>
            </div>
            <div className="lgd-stat-icon">👥</div>
          </div>
          <div className="lgd-stat-card">
            <div className="lgd-stat-content">
              <span className="lgd-stat-label">LAST ACTIVITY</span>
              <span className="lgd-stat-value" style={{ fontSize: "1.2rem", marginTop: "4px" }}>
                {summary.lastActivityAt 
                  ? new Date(summary.lastActivityAt).toLocaleString("vi-VN", {
                      day: "2-digit", month: "2-digit", year: "numeric",
                      hour: "2-digit", minute: "2-digit"
                    })
                  : "No activity"}
              </span>
            </div>
            <div className="lgd-stat-icon">📅</div>
          </div>
        </div>

        {/* Member Table Section */}
        <div className="lgd-table-section">
          <div className="lgd-table-header">
            <h3>Member Contribution (Last 7 Days)</h3>
          </div>
          <table className="lgd-table">
            <thead>
              <tr>
                <th>#</th>
                <th>STUDENT</th>
                <th>ROLE</th>
                <th>COMMITS</th>
                <th>LAST ACTIVE</th>
                <th>STATUS</th>
              </tr>
            </thead>
            <tbody>
              {members.map((m, idx) => (
                <tr key={m.userId}>
                  <td>{String(idx + 1).padStart(2, '0')}</td>
                  <td>
                    <div className="lgd-student-info">
                      <div className="fw-bold">{m.fullName}</div>
                      <div className="text-muted small">{m.studentId}</div>
                    </div>
                  </td>
                  <td>
                    {m.role === "LEADER" ? (
                      <span className="lgd-role-badge leader">LEADER</span>
                    ) : (
                      <span className="lgd-role-badge member">{m.role || "MEMBER"}</span>
                    )}
                  </td>
                  <td className="fw-bold">{m.commitCount}</td>
                  <td>{formatDateVN(m.lastActiveAt)}</td>
                  <td>
                    <span className={`lgd-badge ${m.contributionStatus.toLowerCase().replace('_', '-')}`}>
                      {m.contributionStatus}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* --- JIRA REQUIREMENTS SECTION --- */}
        <div className="lgd-external-section">
          {!selectedEpic ? (
            <div className="lgd-table-section">
              <div className="lgd-table-header">
                <h3 style={{ margin: 0, fontSize: '15px', fontWeight: 600, color: '#1e293b', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#8b5cf6" strokeWidth="2">
                    <path d="M9 11l3 3L22 4" />
                    <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
                  </svg>
                  Project Requirements (Jira Epics)
                  {requirements.length > 0 && (
                    <span className="rd-count-chip">{requirements.length} epics</span>
                  )}
                </h3>
              </div>

              {/* Filter bar - giống RequirementDashboard */}
              <div className="rd-filter-bar">
                <div className="rd-filter-row">
                  <div className="rd-search-wrap">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
                    </svg>
                    <input
                      className="rd-search-input"
                      placeholder="Search by key or summary..."
                      value={keyword}
                      onChange={(e) => setKeyword(e.target.value)}
                      onKeyDown={handleReqKeyDown}
                    />
                    {keyword && (
                      <button className="rd-clear-btn" onClick={() => { setKeyword(""); fetchRequirements("", statusId, priorityId); }}>×</button>
                    )}
                  </div>

                  <select className="rd-select" value={statusId} onChange={(e) => setStatusId(e.target.value)}>
                    <option value="">All Statuses</option>
                    <option value="1">ACTIVE</option>
                    <option value="2">DONE</option>
                  </select>

                  <select className="rd-select" value={priorityId} onChange={(e) => setPriorityId(e.target.value)}>
                    <option value="">All Priorities</option>
                    <option value="3">HIGH</option>
                    <option value="2">MEDIUM</option>
                    <option value="1">LOW</option>
                  </select>

                  <button className="rd-fetch-btn" onClick={handleReqSearch} disabled={reqLoading}>
                    {reqLoading ? <span className="rd-spinner" /> : (
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                        <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
                      </svg>
                    )}
                    {reqLoading ? "Loading..." : "Search"}
                  </button>
                </div>
              </div>

              {reqError && <div className="rd-error">{reqError}</div>}

              {reqLoading ? (
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '32px', color: '#64748b', fontSize: '14px' }}>
                  <span className="rd-spinner" />
                  Loading requirements...
                </div>
              ) : requirements.length > 0 ? (
                <div className="rd-table-wrap" style={{ border: 'none', borderRadius: 0 }}>
                  <table className="rd-table">
                    <thead>
                      <tr>
                        <th>Epic Key</th>
                        <th>Summary</th>
                        <th>Status</th>
                        <th>Priority</th>
                        <th>Stories Progress</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {requirements.map((req) => (
                        <tr key={req.requirementId} className="rd-row">
                          <td><EpicBadge label={req.epicKey || req.key} /></td>
                          <td className="rd-summary-cell">
                            <span className="rd-summary" title={req.summary}>{req.summary || "—"}</span>
                          </td>
                          <td>
                            {(req.statusRaw || req.statusCode)
                              ? <Badge label={req.statusRaw || req.statusCode} colorMap={STATUS_COLOR} />
                              : <span className="rd-null">—</span>}
                          </td>
                          <td>
                            {(req.priorityRaw || req.priorityCode)
                              ? <Badge label={req.priorityRaw || req.priorityCode} colorMap={PRIORITY_COLOR} />
                              : <span className="rd-null">—</span>}
                          </td>
                          <td className="rd-progress-cell">
                            {req.progressTotal > 0
                              ? <ProgressBar done={req.progressDone} total={req.progressTotal} />
                              : <span className="rd-null">No stories</span>}
                          </td>
                          <td>
                            <button
                              className="rd-fetch-btn"
                              style={{ padding: '6px 14px', height: 'auto', fontSize: '12px', marginLeft: 0 }}
                              onClick={() => setSelectedEpic(req)}
                            >
                              View Stories
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="rd-empty">
                  <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                    <path d="M9 11l3 3L22 4" />
                    <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
                  </svg>
                  <p>No requirements found for this group.</p>
                  <span>Sync Jira issues first to populate requirements.</span>
                </div>
              )}
            </div>
          ) : (
            <StoryDashboard
              groupId={groupId}
              requirementId={selectedEpic.requirementId}
              epicKey={selectedEpic.epicKey || selectedEpic.key}
              epicSummary={selectedEpic.summary}
              onClose={() => setSelectedEpic(null)}
            />
          )}
        </div>

        {/* GitHub Detailed Stats Section */}
        <div className="lgd-external-section">
          <CommitStats groupId={groupId} />
        </div>
      </main>
    </div>
  );
}