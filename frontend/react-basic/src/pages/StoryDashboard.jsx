import { useState, useCallback } from "react";
import "./StoryDashboard.css";

const API_URL = "/api/groups";

const STATUS_COLOR = {
  "DONE": "#22c55e",
  "IN_PROGRESS": "#3b82f6",
  "TODO": "#6b7280",
  "IN PROGRESS": "#3b82f6",
  "TO DO": "#6b7280",
  "IN REVIEW": "#f59e0b",
  "BLOCKED": "#ef4444",
};

const PRIORITY_COLOR = {
  "HIGH": "#f97316", "MEDIUM": "#f59e0b", "LOW": "#3b82f6",
  "HIGHEST": "#ef4444", "LOWEST": "#6b7280",
};

function Badge({ label, colorMap, fallback = "#94a3b8", small }) {
  const color = colorMap?.[label?.toUpperCase()] || fallback;
  return (
    <span className={`sd-badge${small ? " sd-badge-sm" : ""}`} style={{ "--badge-color": color }}>
      {label || "—"}
    </span>
  );
}

function ProgressBar({ done, total }) {
  const pct = total > 0 ? Math.round((done / total) * 100) : 0;
  const color = pct === 100 ? "#22c55e" : pct >= 50 ? "#3b82f6" : "#f59e0b";
  return (
    <div className="sd-progress-wrap">
      <div className="sd-progress-bar">
        <div className="sd-progress-fill" style={{ width: `${pct}%`, "--fill-color": color }} />
      </div>
      <span className="sd-progress-label">{done}/{total} ({pct}%)</span>
    </div>
  );
}

function SubtaskRow({ groupId, storyId }) {
  const [subtasks, setSubtasks] = useState(null);
  const [loading, setLoading] = useState(false);

  const authHeader = () => ({ Authorization: `Bearer ${localStorage.getItem("token")}` });

  const load = useCallback(async () => {
    if (subtasks !== null) return;
    setLoading(true);
    try {
      const res = await fetch(`${API_URL}/${groupId}/stories/${storyId}/subtasks`, { headers: authHeader() });
      const data = await res.json();
      setSubtasks(data.data || []);
    } catch { setSubtasks([]); }
    finally { setLoading(false); }
  }, [groupId, storyId, subtasks]);

  // trigger load on mount
  useState(() => { load(); });

  if (loading) return (
    <tr className="sd-subtask-loading"><td colSpan="4"><span className="sd-spinner-sm" /> Loading subtasks...</td></tr>
  );

  if (!subtasks || subtasks.length === 0) return (
    <tr className="sd-subtask-empty"><td colSpan="4">No subtasks</td></tr>
  );

  return subtasks.map((sub) => (
    <tr key={sub.taskId} className="sd-subtask-row">
      <td><span className="sd-sub-key">{sub.subtaskKey || sub.storyKey || "—"}</span></td>
      <td className="sd-sub-summary">{sub.summary || "—"}</td>
      <td><Badge label={sub.statusRaw || sub.statusCode} colorMap={STATUS_COLOR} small /></td>
      <td>
        {sub.assigneeName
          ? <div className="sd-assignee-sm"><span className="sd-avatar-sm">{sub.assigneeName.charAt(0)}</span>{sub.assigneeName}</div>
          : <span className="sd-null">—</span>}
      </td>
    </tr>
  ));
}

export default function StoryDashboard({ groupId, requirementId, epicKey, epicSummary, onClose }) {
  const [keyword, setKeyword] = useState("");
  const [statusId, setStatusId] = useState("");
  const [myTasks, setMyTasks] = useState(false);

  const [response, setResponse] = useState(null);
  const [stories, setStories] = useState([]);
  const [pagination, setPagination] = useState(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [fetched, setFetched] = useState(false);
  const [expandedIds, setExpandedIds] = useState(new Set());

  const authHeader = () => ({ Authorization: `Bearer ${localStorage.getItem("token")}` });

  const fetchStories = useCallback(async (p = 0) => {
    setError("");
    setLoading(true);
    if (p === 0) { setStories([]); setFetched(false); setExpandedIds(new Set()); }

    const params = new URLSearchParams();
    params.set("page", p);
    params.set("size", "20");
    if (statusId) params.set("statusId", statusId);
    if (keyword.trim()) params.set("keyword", keyword.trim());
    if (myTasks) params.set("myTasks", "true");

    try {
      const res = await fetch(
        `${API_URL}/${groupId}/requirements/${requirementId}/stories?${params}`,
        { headers: authHeader() }
      );
      const data = await res.json();
      if (!res.ok) {
        setError(data.message || "Failed to fetch stories");
      } else {
        const r = data.data;
        setResponse(r);
        setStories(r.page?.content || []);
        setPagination({
          totalElements: r.page?.totalElements,
          totalPages: r.page?.totalPages,
          isFirst: r.page?.first,
          isLast: r.page?.last,
        });
        setPage(p);
        setFetched(true);
      }
    } catch { setError("Network error"); }
    finally { setLoading(false); }
  }, [groupId, requirementId, statusId, keyword, myTasks]);

  // Auto-fetch on mount
  useState(() => { fetchStories(0); });

  const toggleExpand = (id) => {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const progress = response?.progressSummary;

  const formatDate = (iso) => {
    if (!iso) return "—";
    return new Date(iso).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
  };

  return (
    <div className="sd-root">
      {/*  Header  */}
      <div className="sd-header">
        <div className="sd-title-group">
          <button className="sd-back-btn" onClick={onClose}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <path d="M19 12H5M12 5l-7 7 7 7"/>
            </svg>
            Back to Epics
          </button>
          <div className="sd-epic-info">
            <span className="sd-epic-key">{epicKey}</span>
            <span className="sd-epic-summary">{epicSummary}</span>
          </div>
        </div>

        {/* Progress summary */}
        {progress && (
          <div className="sd-progress-section">
            <ProgressBar done={progress.doneCount} total={progress.totalCount} />
            {progress.byStatus?.length > 0 && (
              <div className="sd-status-breakdown">
                {progress.byStatus.map((item) => {
                  const color = STATUS_COLOR[item.statusCode?.toUpperCase()] || STATUS_COLOR[item.statusName?.toUpperCase()] || "#94a3b8";
                  return (
                    <span key={item.statusCode || item.statusName} className="sd-breakdown-chip" style={{ "--chip-color": color }}>
                      {item.statusName || item.statusCode}: {item.count}
                    </span>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </div>

      {/*  Filter bar  */}
      <div className="sd-filter-bar">
        <div className="sd-filter-row">
          <div className="sd-search-wrap">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
            </svg>
            <input
              className="sd-search-input"
              placeholder="Search by key or summary..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && fetchStories(0)}
            />
            {keyword && <button className="sd-clear-btn" onClick={() => setKeyword("")}>×</button>}
          </div>

          <select className="sd-select" value={statusId} onChange={(e) => setStatusId(e.target.value)}>
            <option value="">All Statuses</option>
            <option value="1">TODO</option>
            <option value="2">IN PROGRESS</option>
            <option value="3">DONE</option>
          </select>

          <label className="sd-mytasks-toggle">
            <input type="checkbox" checked={myTasks} onChange={(e) => setMyTasks(e.target.checked)} />
            My Tasks
          </label>

          <button className="sd-fetch-btn" onClick={() => fetchStories(0)} disabled={loading}>
            {loading ? <span className="sd-spinner" /> : (
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
              </svg>
            )}
            {loading ? "Loading..." : "Search"}
          </button>
        </div>
      </div>

      {error && <div className="sd-error">{error}</div>}

      {/*  Empty  */}
      {!loading && fetched && stories.length === 0 && (
        <div className="sd-empty">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
            <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/>
            <rect x="9" y="3" width="6" height="4" rx="1"/>
          </svg>
          <p>No stories found for this epic.</p>
        </div>
      )}

      {/*  Table  */}
      {!loading && stories.length > 0 && (
        <div className="sd-table-wrap">
          <table className="sd-table">
            <thead>
              <tr>
                <th style={{ width: 32 }}></th>
                <th>Story Key</th>
                <th>Summary</th>
                <th>Status</th>
                <th>Priority</th>
                <th>Assignee</th>
                <th>Subtasks</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {stories.map((story) => {
                const expanded = expandedIds.has(story.taskId);
                return (
                  <>
                    <tr key={story.taskId} className={`sd-row${expanded ? " sd-row-expanded" : ""}`}>
                      <td>
                        <button className="sd-expand-btn" onClick={() => toggleExpand(story.taskId)}>
                          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
                            style={{ transform: expanded ? "rotate(90deg)" : "none", transition: "transform 0.15s" }}>
                            <path d="M9 18l6-6-6-6"/>
                          </svg>
                        </button>
                      </td>
                      <td><span className="sd-story-key">{story.storyKey || "—"}</span></td>
                      <td className="sd-summary-cell">
                        <span className="sd-summary" title={story.summary}>{story.summary || "—"}</span>
                      </td>
                      <td><Badge label={story.statusRaw || story.statusCode} colorMap={STATUS_COLOR} /></td>
                      <td>
                        {story.priorityRaw
                          ? <Badge label={story.priorityRaw} colorMap={PRIORITY_COLOR} />
                          : <span className="sd-null">—</span>}
                      </td>
                      <td>
                        {story.assigneeName ? (
                          <div className="sd-assignee">
                            <span className="sd-avatar">{story.assigneeName.charAt(0).toUpperCase()}</span>
                            <span>{story.assigneeName}</span>
                          </div>
                        ) : <span className="sd-null">Unassigned</span>}
                      </td>
                      <td>
                        <span className="sd-subtask-count">
                          {story.subtasksCount > 0 ? `${story.subtasksCount} subtask${story.subtasksCount > 1 ? "s" : ""}` : <span className="sd-null">—</span>}
                        </span>
                      </td>
                      <td className="sd-date">{formatDate(story.updated)}</td>
                    </tr>

                    {/* Subtask rows */}
                    {expanded && (
                      <tr key={`sub-${story.taskId}`} className="sd-subtask-container">
                        <td colSpan="8" className="sd-subtask-td">
                          <table className="sd-subtask-table">
                            <thead>
                              <tr>
                                <th>Key</th>
                                <th>Summary</th>
                                <th>Status</th>
                                <th>Assignee</th>
                              </tr>
                            </thead>
                            <tbody>
                              <SubtaskRow groupId={groupId} storyId={story.taskId} />
                            </tbody>
                          </table>
                        </td>
                      </tr>
                    )}
                  </>
                );
              })}
            </tbody>
          </table>

          {/* Pagination */}
          {pagination && pagination.totalPages > 1 && (
            <div className="sd-pagination">
              <button className="sd-page-btn" onClick={() => fetchStories(page - 1)} disabled={pagination.isFirst || loading}>← Prev</button>
              <div className="sd-page-numbers">
                {Array.from({ length: pagination.totalPages }, (_, i) => (
                  <button key={i} className={`sd-page-num${i === page ? " active" : ""}`} onClick={() => fetchStories(i)} disabled={loading}>
                    {i + 1}
                  </button>
                ))}
              </div>
              <button className="sd-page-btn" onClick={() => fetchStories(page + 1)} disabled={pagination.isLast || loading}>Next →</button>
              <span className="sd-page-info">{pagination.totalElements} stories total</span>
            </div>
          )}
        </div>
      )}
    </div>
  );
}