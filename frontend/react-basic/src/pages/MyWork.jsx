import { useState, useEffect, useCallback } from "react";
import "./MyWork.css";

const API_URL = "/api/groups";

const STATUS_COLOR = {
  DONE: "#22c55e", IN_PROGRESS: "#3b82f6", "IN PROGRESS": "#3b82f6",
  TODO: "#6b7280", "TO DO": "#6b7280", IN_REVIEW: "#f59e0b", BLOCKED: "#ef4444",
};
const PRIORITY_COLOR = {
  HIGHEST: "#ef4444", HIGH: "#f97316", MEDIUM: "#f59e0b", LOW: "#3b82f6", LOWEST: "#6b7280",
};

function Badge({ label, colorMap, fallback = "#94a3b8" }) {
  if (!label) return <span className="mw-null">—</span>;
  const color = colorMap?.[label.toUpperCase()] || fallback;
  return <span className="mw-badge" style={{ "--badge-color": color }}>{label}</span>;
}

function formatDate(iso) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
}

// ── Detail Modal ──────────────────────────────────────────────────────────────
function SubtaskDetail({ groupId, taskId, onClose }) {
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const auth = () => ({ Authorization: `Bearer ${localStorage.getItem("token")}` });

  useEffect(() => {
    const fetchDetail = async () => {
      setLoading(true);
      try {
        const res = await fetch(`${API_URL}/${groupId}/my-work/subtasks/${taskId}`, { headers: auth() });
        const data = await res.json();
        if (!res.ok) { setError(data.message || "Failed to load detail"); return; }
        setDetail(data.data);
      } catch { setError("Network error"); }
      finally { setLoading(false); }
    };
    fetchDetail();
  }, [groupId, taskId]);

  return (
    <div className="mw-modal-overlay" onClick={onClose}>
      <div className="mw-modal" onClick={e => e.stopPropagation()}>
        <div className="mw-modal-header">
          <span className="mw-modal-key">{detail?.subtaskKey || "Subtask"}</span>
          <button className="mw-modal-close" onClick={onClose}>×</button>
        </div>

        {loading && <div className="mw-modal-loading"><span className="mw-spinner"/>Loading...</div>}
        {error && <div className="mw-modal-error">{error}</div>}

        {detail && !loading && (
          <div className="mw-modal-body">
            <h2 className="mw-modal-title">{detail.summary}</h2>

            {/* Badges row */}
            <div className="mw-modal-badges">
              <Badge label={detail.statusRaw || detail.statusCode} colorMap={STATUS_COLOR} />
              <Badge label={detail.priorityRaw} colorMap={PRIORITY_COLOR} />
              {detail.assigneeName && (
                <span className="mw-modal-assignee">
                  <span className="mw-avatar-sm">{detail.assigneeName.charAt(0)}</span>
                  {detail.assigneeName}
                </span>
              )}
            </div>

            {/* Description */}
            {detail.description && (
              <div className="mw-modal-section">
                <span className="mw-modal-label">Description</span>
                <p className="mw-modal-desc">{detail.description}</p>
              </div>
            )}

            {/* Context */}
            <div className="mw-modal-context">
              {detail.epicKey && (
                <div className="mw-ctx-row">
                  <span className="mw-ctx-label">Epic</span>
                  <div className="mw-ctx-val">
                    <span className="mw-ctx-key">{detail.epicKey}</span>
                    <span className="mw-ctx-summary">{detail.epicSummary}</span>
                  </div>
                </div>
              )}
              {detail.parentStoryKey && (
                <div className="mw-ctx-row">
                  <span className="mw-ctx-label">Story</span>
                  <div className="mw-ctx-val">
                    <span className="mw-ctx-key">{detail.parentStoryKey}</span>
                    <span className="mw-ctx-summary">{detail.parentStorySummary}</span>
                  </div>
                </div>
              )}
              {detail.updated && (
                <div className="mw-ctx-row">
                  <span className="mw-ctx-label">Updated</span>
                  <span className="mw-ctx-val">{formatDate(detail.updated)}</span>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// ── Main MyWork component ─────────────────────────────────────────────────────
export default function MyWork({ groupId }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);

  // Filters
  const [statusId, setStatusId] = useState("");
  const [priority, setPriority] = useState("");
  const [keyword, setKeyword] = useState("");

  // Detail modal
  const [selectedTaskId, setSelectedTaskId] = useState(null);

  const auth = () => ({ Authorization: `Bearer ${localStorage.getItem("token")}` });

  const fetchMyWork = useCallback(async (p = 0) => {
    setLoading(true); setError("");
    const params = new URLSearchParams();
    params.set("page", p); params.set("size", "20");
    if (statusId) params.set("statusId", statusId);
    if (priority) params.set("priority", priority);
    if (keyword.trim()) params.set("keyword", keyword.trim());

    try {
      const res = await fetch(`${API_URL}/${groupId}/my-work/subtasks?${params}`, { headers: auth() });
      const json = await res.json();
      if (!res.ok) { setError(json.message || "Failed to load tasks"); return; }
      setData(json.data);
      setPage(p);
    } catch { setError("Network error"); }
    finally { setLoading(false); }
  }, [groupId, statusId, priority, keyword]);

  useEffect(() => { if (groupId) fetchMyWork(0); }, [groupId]);

  const subtasks = data?.page?.content || [];
  const pagination = data?.page;

  return (
    <div className="mw-root">
      {/* Header */}
      <div className="mw-header">
        <div className="mw-title">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M9 11l3 3L22 4"/>
            <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
          </svg>
          My Tasks
          {data && (
            <span className="mw-count-chip">
              {data.mappedToJira
                ? `${pagination?.totalElements ?? 0} tasks`
                : "Not mapped"}
            </span>
          )}
        </div>
      </div>

      {/* Unmapped state */}
      {data && !data.mappedToJira && (
        <div className="mw-unmapped">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.3">
            <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/>
            <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
            <line x1="2" y1="2" x2="22" y2="22"/>
          </svg>
          <p>Your account is not linked to Jira.</p>
          <span>Contact your leader or admin to link your Jira account.</span>
        </div>
      )}

      {/* Filter bar — only show if mapped */}
      {(!data || data.mappedToJira) && (
        <div className="mw-filter-bar">
          <div className="mw-search-wrap">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
            </svg>
            <input
              className="mw-search-input"
              placeholder="Search tasks..."
              value={keyword}
              onChange={e => setKeyword(e.target.value)}
              onKeyDown={e => e.key === "Enter" && fetchMyWork(0)}
            />
            {keyword && <button className="mw-clear-btn" onClick={() => setKeyword("")}>×</button>}
          </div>

          <select className="mw-select" value={statusId} onChange={e => setStatusId(e.target.value)}>
            <option value="">All Statuses</option>
            <option value="1">TODO</option>
            <option value="2">IN PROGRESS</option>
            <option value="3">DONE</option>
          </select>

          <select className="mw-select" value={priority} onChange={e => setPriority(e.target.value)}>
            <option value="">All Priorities</option>
            <option value="HIGH">HIGH</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="LOW">LOW</option>
          </select>

          <button className="mw-search-btn" onClick={() => fetchMyWork(0)} disabled={loading}>
            {loading ? <span className="mw-spinner"/> : (
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
              </svg>
            )}
            {loading ? "Loading..." : "Search"}
          </button>
        </div>
      )}

      {error && <div className="mw-error">{error}</div>}

      {loading && (
        <div className="mw-loading"><span className="mw-spinner"/> Loading your tasks...</div>
      )}

      {!loading && data?.mappedToJira && subtasks.length === 0 && (
        <div className="mw-empty">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.3">
            <path d="M9 11l3 3L22 4"/>
            <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
          </svg>
          <p>No tasks assigned to you.</p>
        </div>
      )}

      {/* Table */}
      {!loading && subtasks.length > 0 && (
        <div className="mw-table-wrap">
          <table className="mw-table">
            <thead>
              <tr>
                <th>Key</th>
                <th>Summary</th>
                <th>Status</th>
                <th>Priority</th>
                <th>Story</th>
                <th>Epic</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {subtasks.map(s => (
                <tr
                  key={s.taskId}
                  className="mw-row"
                  onClick={() => setSelectedTaskId(s.taskId)}
                  title="Click to view detail"
                >
                  <td><span className="mw-key">{s.subtaskKey || "—"}</span></td>
                  <td className="mw-summary-cell">
                    <span className="mw-summary" title={s.summary}>{s.summary || "—"}</span>
                  </td>
                  <td><Badge label={s.statusRaw || s.statusCode} colorMap={STATUS_COLOR} /></td>
                  <td><Badge label={s.priorityRaw} colorMap={PRIORITY_COLOR} /></td>
                  <td>
                    {s.parentStoryKey
                      ? <span className="mw-parent-key" title={s.parentStorySummary}>{s.parentStoryKey}</span>
                      : <span className="mw-null">—</span>}
                  </td>
                  <td>
                    {s.epicKey
                      ? <span className="mw-parent-key" title={s.epicSummary}>{s.epicKey}</span>
                      : <span className="mw-null">—</span>}
                  </td>
                  <td className="mw-date">{formatDate(s.updated)}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Pagination */}
          {pagination && pagination.totalPages > 1 && (
            <div className="mw-pagination">
              <button className="mw-page-btn" onClick={() => fetchMyWork(page - 1)} disabled={pagination.first || loading}>← Prev</button>
              <div className="mw-page-numbers">
                {Array.from({ length: pagination.totalPages }, (_, i) => (
                  <button key={i} className={`mw-page-num ${i === page ? "active" : ""}`} onClick={() => fetchMyWork(i)} disabled={loading}>
                    {i + 1}
                  </button>
                ))}
              </div>
              <button className="mw-page-btn" onClick={() => fetchMyWork(page + 1)} disabled={pagination.last || loading}>Next →</button>
              <span className="mw-page-info">Page {page + 1} of {pagination.totalPages} · {pagination.totalElements} total</span>
            </div>
          )}
        </div>
      )}

      {/* Detail modal */}
      {selectedTaskId && (
        <SubtaskDetail
          groupId={groupId}
          taskId={selectedTaskId}
          onClose={() => setSelectedTaskId(null)}
        />
      )}
    </div>
  );
}