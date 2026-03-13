import { useState, useCallback } from "react";
import "./RequirementDashboard.css";
import StoryDashboard from "./StoryDashboard";

const API_URL = "/api/groups";

const STATUS_COLOR = {
  DONE: "#22c55e",
  "IN PROGRESS": "#3b82f6",
  "IN_PROGRESS": "#3b82f6",
  TODO: "#6b7280",
  "TO DO": "#6b7280",
};

const PRIORITY_COLOR = {
  HIGHEST: "#ef4444",
  HIGH: "#f97316",
  MEDIUM: "#f59e0b",
  LOW: "#3b82f6",
  LOWEST: "#6b7280",
};

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

function Badge({ label, colorMap, fallback = "#94a3b8" }) {
  const color = colorMap?.[label?.toUpperCase()] || fallback;
  return (
    <span className="rd-badge" style={{ "--badge-color": color }}>
      {label || "—"}
    </span>
  );
}

function RequirementDashboard({ groupId }) {
  const [keyword, setKeyword] = useState("");
  const [statusId, setStatusId] = useState("");
  const [priorityId, setPriorityId] = useState("");
  const [requirements, setRequirements] = useState([]);
  const [pagination, setPagination] = useState(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [fetched, setFetched] = useState(false);

  // ── Drill-down state ──────────────────────────────────────────────────────
  const [selectedEpic, setSelectedEpic] = useState(null);

  const authHeader = () => ({
    Authorization: `Bearer ${localStorage.getItem("token")}`,
  });

  const fetchRequirements = useCallback(async (p = 0) => {
    setError("");
    setLoading(true);
    if (p === 0) { setRequirements([]); setFetched(false); }

    const params = new URLSearchParams();
    params.set("page", p);
    params.set("size", "20");
    if (statusId) params.set("statusId", statusId);
    if (priorityId) params.set("priorityId", priorityId);
    if (keyword.trim()) params.set("keyword", keyword.trim());

    try {
      const res = await fetch(`${API_URL}/${groupId}/requirements?${params}`, {
        headers: authHeader(),
      });
      const data = await res.json();
      if (!res.ok) {
        setError(data.message || "Failed to fetch requirements");
      } else {
        const pageData = data.data;
        setRequirements(pageData.content || []);
        setPagination({
          totalElements: pageData.totalElements,
          totalPages: pageData.totalPages,
          currentPage: pageData.number,
          isFirst: pageData.first,
          isLast: pageData.last,
        });
        setPage(p);
        setFetched(true);
      }
    } catch {
      setError("Network error");
    } finally {
      setLoading(false);
    }
  }, [groupId, statusId, priorityId, keyword]);

  const handleSearch = () => fetchRequirements(0);
  const handleKeyDown = (e) => { if (e.key === "Enter") handleSearch(); };

  const formatDate = (iso) => {
    if (!iso) return "—";
    return new Date(iso).toLocaleDateString("vi-VN", {
      day: "2-digit", month: "2-digit", year: "numeric",
    });
  };

  // ── Show StoryDashboard when epic selected ────────────────────────────────
  if (selectedEpic) {
    return (
      <div className="rd-root">
        <StoryDashboard
          groupId={groupId}
          requirementId={selectedEpic.requirementId}
          epicKey={selectedEpic.epicKey}
          epicSummary={selectedEpic.epicSummary}
          onClose={() => setSelectedEpic(null)}
        />
      </div>
    );
  }

  return (
    <div className="rd-root">
      {/* Header */}
      <div className="rd-header">
        <div className="rd-title">
          <span className="rd-title-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M9 11l3 3L22 4" />
              <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
            </svg>
          </span>
          <span>Requirements</span>
          {fetched && pagination && (
            <span className="rd-count-chip">{pagination.totalElements} epics</span>
          )}
        </div>
      </div>

      {/* Filter bar */}
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
              onKeyDown={handleKeyDown}
            />
            {keyword && (
              <button className="rd-clear-btn" onClick={() => setKeyword("")}>×</button>
            )}
          </div>

          <select className="rd-select" value={statusId} onChange={(e) => setStatusId(e.target.value)}>
            <option value="">All Statuses</option>
            <option value="1">TODO</option>
            <option value="2">IN PROGRESS</option>
            <option value="3">DONE</option>
          </select>

          <select className="rd-select" value={priorityId} onChange={(e) => setPriorityId(e.target.value)}>
            <option value="">All Priorities</option>
            <option value="3">HIGH</option>
            <option value="2">MEDIUM</option>
            <option value="1">LOW</option>
          </select>

          <button className="rd-fetch-btn" onClick={handleSearch} disabled={loading}>
            {loading ? <span className="rd-spinner" /> : (
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
              </svg>
            )}
            {loading ? "Loading..." : "Search"}
          </button>
        </div>
      </div>

      {error && <div className="rd-error">{error}</div>}

      {!loading && fetched && requirements.length === 0 && (
        <div className="rd-empty">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
            <path d="M9 11l3 3L22 4" />
            <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
          </svg>
          <p>No requirements found.</p>
          <span>Sync Jira issues first to populate requirements.</span>
        </div>
      )}

      {!loading && requirements.length > 0 && (
        <div className="rd-table-wrap">
          <table className="rd-table">
            <thead>
              <tr>
                <th>Epic Key</th>
                <th>Summary</th>
                <th>Status</th>
                <th>Priority</th>
                <th>Stories Progress</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {requirements.map((req) => (
                <tr
                  key={req.requirementId}
                  className="rd-row rd-row-clickable"
                  onClick={() => setSelectedEpic({
                    requirementId: req.requirementId,
                    epicKey: req.epicKey,
                    epicSummary: req.summary,
                  })}
                  title="Click to view stories"
                >
                  <td><span className="rd-epic-key">{req.epicKey || "—"}</span></td>
                  <td className="rd-summary-cell">
                    <span className="rd-summary" title={req.summary}>{req.summary || "—"}</span>
                  </td>
                  <td>
                    <Badge label={req.statusRaw || req.statusCode} colorMap={STATUS_COLOR} />
                  </td>
                  <td>
                    {(req.priorityRaw || req.priorityCode) ? (
                      <Badge label={req.priorityRaw || req.priorityCode} colorMap={PRIORITY_COLOR} />
                    ) : (
                      <span className="rd-null">—</span>
                    )}
                  </td>
                  <td className="rd-progress-cell">
                    {req.progressTotal > 0 ? (
                      <ProgressBar done={req.progressDone} total={req.progressTotal} />
                    ) : (
                      <span className="rd-null">No stories</span>
                    )}
                  </td>
                  <td className="rd-date">{formatDate(req.updated)}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {pagination && pagination.totalPages > 1 && (
            <div className="rd-pagination">
              <button className="rd-page-btn" onClick={() => fetchRequirements(page - 1)} disabled={pagination.isFirst || loading}>← Prev</button>
              <div className="rd-page-numbers">
                {Array.from({ length: pagination.totalPages }, (_, i) => (
                  <button key={i} className={`rd-page-num ${i === page ? "active" : ""}`} onClick={() => fetchRequirements(i)} disabled={loading}>
                    {i + 1}
                  </button>
                ))}
              </div>
              <button className="rd-page-btn" onClick={() => fetchRequirements(page + 1)} disabled={pagination.isLast || loading}>Next →</button>
              <span className="rd-page-info">
                Page {page + 1} of {pagination.totalPages} · {pagination.totalElements} total
              </span>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default RequirementDashboard;