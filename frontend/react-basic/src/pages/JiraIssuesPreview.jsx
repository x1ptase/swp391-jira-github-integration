import { useEffect, useState, useCallback } from "react";
import "./JiraIssuesPreview.css";

const API_URL = "/api/integrations/jira";

const FILTER_TYPES = ["ALL", "SPRINT", "VERSION", "LABEL"];

const STATUS_COLOR = {
  "To Do": "#6b7280",
  "In Progress": "#3b82f6",
  Done: "#22c55e",
  "In Review": "#f59e0b",
  Blocked: "#ef4444",
};

const PRIORITY_COLOR = {
  Highest: "#ef4444",
  High: "#f97316",
  Medium: "#f59e0b",
  Low: "#3b82f6",
  Lowest: "#6b7280",
};

function Badge({ label, colorMap, fallback = "#94a3b8" }) {
  const color = colorMap?.[label] || fallback;
  return (
    <span className="jip-badge" style={{ "--badge-color": color }}>
      {label || "—"}
    </span>
  );
}

function JiraIssuesPreview({ groupId }) {
  //  Filter state 
  const [filterType, setFilterType] = useState("ALL");
  const [sprintId, setSprintId] = useState("");
  const [versionId, setVersionId] = useState("");
  const [label, setLabel] = useState("");

  //  Dropdown data 
  const [boards, setBoards] = useState([]);
  const [selectedBoardId, setSelectedBoardId] = useState("");
  const [sprints, setSprints] = useState([]);
  const [versions, setVersions] = useState([]);
  const [labelSuggestions, setLabelSuggestions] = useState([]);
  const [labelQuery, setLabelQuery] = useState("");
  const [loadingBoards, setLoadingBoards] = useState(false);
  const [loadingSprints, setLoadingSprints] = useState(false);

  //  Issues + pagination state 
  const [issues, setIssues] = useState([]);
  const [nextPageToken, setNextPageToken] = useState(null);
  const [isLast, setIsLast] = useState(false);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");
  const [fetched, setFetched] = useState(false);

  //  Sync state 
  const [syncing, setSyncing] = useState(false);
  const [syncResult, setSyncResult] = useState(null);

  //  Sort 
  const [sortKey, setSortKey] = useState("updated");
  const [sortDir, setSortDir] = useState("desc");

  const authHeader = () => ({
    Authorization: `Bearer ${localStorage.getItem("token")}`,
  });

  //  Load boards when SPRINT tab selected 
  useEffect(() => {
    if (filterType !== "SPRINT") return;
    if (boards.length > 0) return;
    setLoadingBoards(true);
    fetch(`${API_URL}/${groupId}/boards`, { headers: authHeader() })
      .then((r) => r.json())
      .then((d) => setBoards(d.data || []))
      .catch(() => {})
      .finally(() => setLoadingBoards(false));
  }, [filterType]);

  //  Load sprints when board selected 
  useEffect(() => {
    if (!selectedBoardId) { setSprints([]); setSprintId(""); return; }
    setLoadingSprints(true);
    setSprints([]);
    setSprintId("");
    fetch(
      `${API_URL}/${groupId}/boards/${selectedBoardId}/sprints?state=active,future,closed`,
      { headers: authHeader() }
    )
      .then((r) => r.json())
      .then((d) => setSprints(d.data || []))
      .catch(() => {})
      .finally(() => setLoadingSprints(false));
  }, [selectedBoardId]);

  //  Load versions when VERSION tab selected 
  useEffect(() => {
    if (filterType !== "VERSION") return;
    if (versions.length > 0) return;
    fetch(`${API_URL}/${groupId}/versions`, { headers: authHeader() })
      .then((r) => r.json())
      .then((d) => setVersions(d.data || []))
      .catch(() => {});
  }, [filterType]);

  //  Label suggestions 
  useEffect(() => {
    if (filterType !== "LABEL") return;
    const timer = setTimeout(() => {
      fetch(
        `${API_URL}/${groupId}/labels?q=${encodeURIComponent(labelQuery)}&limit=20`,
        { headers: authHeader() }
      )
        .then((r) => r.json())
        .then((d) => setLabelSuggestions(d.data || []))
        .catch(() => {});
    }, 300);
    return () => clearTimeout(timer);
  }, [labelQuery, filterType]);

  //  Build params helper 
  const buildParams = useCallback(
    (pageToken = null) => {
      const params = new URLSearchParams();
      params.set("filterType", filterType);
      params.set("fetchAll", "false");
      params.set("maxResults", "20");
      if (filterType === "SPRINT" && sprintId) params.set("sprintId", sprintId);
      if (filterType === "VERSION" && versionId) params.set("versionId", versionId);
      if (filterType === "LABEL" && label) params.set("label", label);
      if (pageToken) params.set("pageToken", pageToken);
      return params;
    },
    [filterType, sprintId, versionId, label]
  );

  //  Fetch first page 
  const fetchIssues = useCallback(async () => {
    setError("");
    setSyncResult(null);
    setLoading(true);
    setFetched(false);
    setIssues([]);
    setNextPageToken(null);
    setIsLast(false);

    try {
      const res = await fetch(
        `${API_URL}/${groupId}/issues?${buildParams()}`,
        { headers: authHeader() }
      );
      const data = await res.json();

      if (!res.ok) {
        setError(data.message || "Failed to fetch issues");
      } else {
        const page = data.data;
        setIssues(page.items || []);
        setNextPageToken(page.nextPageToken || null);
        setIsLast(Boolean(page.isLast));
        setFetched(true);
      }
    } catch {
      setError("Network error");
    } finally {
      setLoading(false);
    }
  }, [groupId, buildParams]);

  //  Load more 
  const loadMore = async () => {
    if (!nextPageToken || loadingMore) return;
    setLoadingMore(true);
    setError("");

    try {
      const res = await fetch(
        `${API_URL}/${groupId}/issues?${buildParams(nextPageToken)}`,
        { headers: authHeader() }
      );
      const data = await res.json();

      if (!res.ok) {
        setError(data.message || "Failed to load more");
      } else {
        const page = data.data;
        setIssues((prev) => [...prev, ...(page.items || [])]);
        setNextPageToken(page.nextPageToken || null);
        setIsLast(Boolean(page.isLast));
      }
    } catch {
      setError("Network error");
    } finally {
      setLoadingMore(false);
    }
  };

  //  Manual sync 
  const handleSync = async () => {
    if (syncing) return;
    setSyncing(true);
    setSyncResult(null);
    setError("");

    try {
      const res = await fetch(`${API_URL}/${groupId}/sync`, {
        method: "POST",
        headers: authHeader(),
      });
      const data = await res.json();

      if (!res.ok) {
        if (res.status === 409) {
          setError("A sync is already running for this group. Please wait.");
        } else {
          setError(data.message || "Sync failed");
        }
      } else {
        setSyncResult(data.data);
      }
    } catch {
      setError("Network error during sync");
    } finally {
      setSyncing(false);
    }
  };

  //  Reset filter state 
  const resetFilterState = (ft) => {
    setFilterType(ft);
    setSprintId("");
    setVersionId("");
    setLabel("");
    setLabelQuery("");
    setSelectedBoardId("");
    setSprints([]);
    setIssues([]);
    setFetched(false);
    setNextPageToken(null);
    setIsLast(false);
    setError("");
    setSyncResult(null);
  };

  //  Sort logic 
  const toggleSort = (key) => {
    if (sortKey === key) setSortDir((d) => (d === "asc" ? "desc" : "asc"));
    else { setSortKey(key); setSortDir("asc"); }
  };

  const sorted = [...issues].sort((a, b) => {
    let av = a[sortKey] ?? "";
    let bv = b[sortKey] ?? "";
    if (sortKey === "updated") {
      av = av ? new Date(av).getTime() : 0;
      bv = bv ? new Date(bv).getTime() : 0;
    }
    if (av < bv) return sortDir === "asc" ? -1 : 1;
    if (av > bv) return sortDir === "asc" ? 1 : -1;
    return 0;
  });

  const SortIcon = ({ col }) => {
    if (sortKey !== col) return <span className="jip-sort-icon">↕</span>;
    return <span className="jip-sort-icon active">{sortDir === "asc" ? "↑" : "↓"}</span>;
  };

  const canFetch =
    filterType === "ALL" ||
    (filterType === "SPRINT" && sprintId) ||
    (filterType === "VERSION" && versionId) ||
    (filterType === "LABEL" && label.trim());

  const formatDate = (iso) => {
    if (!iso) return "—";
    return new Date(iso).toLocaleDateString("vi-VN", {
      day: "2-digit", month: "2-digit", year: "numeric",
    });
  };

  return (
    <div className="jip-root">
      {/*  Header  */}
      <div className="jip-header">
        <div className="jip-title">
          <span className="jip-title-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <rect x="3" y="3" width="7" height="7" rx="1" />
              <rect x="14" y="3" width="7" height="7" rx="1" />
              <rect x="3" y="14" width="7" height="7" rx="1" />
              <rect x="14" y="14" width="7" height="7" rx="1" />
            </svg>
          </span>
          <span>Issues Preview</span>
          {fetched && (
            <span className="jip-count-chip">
              {issues.length} issues{!isLast ? "+" : ""}
            </span>
          )}
        </div>

        {/* Sync button */}
        <button
          className="jip-sync-btn"
          onClick={handleSync}
          disabled={syncing}
          title="Sync Jira → Requirements/Tasks"
        >
          <svg
            width="14" height="14" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" strokeWidth="2.5"
            className={syncing ? "jip-spin" : ""}
          >
            <path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8" />
            <path d="M21 3v5h-5" />
          </svg>
          {syncing ? "Syncing..." : "Sync to Tasks"}
        </button>
      </div>

      {/*  Sync result  */}
      {syncResult && (
        <div className="jip-sync-result">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M20 6 9 17l-5-5" />
          </svg>
          Sync complete —{" "}
          {syncResult.createdRequirements != null && (
            <span>{syncResult.createdRequirements} requirements, </span>
          )}
          {syncResult.createdTasks != null && (
            <span>{syncResult.createdTasks} tasks created</span>
          )}
          {syncResult.message && <span>{syncResult.message}</span>}
        </div>
      )}

      {/*  Filter bar  */}
      <div className="jip-filter-bar">
        <div className="jip-filter-row">
          <div className="jip-filter-tabs">
            {FILTER_TYPES.map((ft) => (
              <button
                key={ft}
                className={`jip-tab ${filterType === ft ? "active" : ""}`}
                onClick={() => resetFilterState(ft)}
              >
                {ft}
              </button>
            ))}
          </div>

          {/* SPRINT: Board → Sprint cascade */}
          {filterType === "SPRINT" && (
            <>
              <select
                className="jip-select"
                value={selectedBoardId}
                onChange={(e) => setSelectedBoardId(e.target.value)}
                disabled={loadingBoards}
              >
                <option value="">
                  {loadingBoards ? "Loading boards..." : "— Select Board —"}
                </option>
                {boards.map((b) => (
                  <option key={b.id} value={b.id}>{b.name}</option>
                ))}
              </select>

              <select
                className="jip-select"
                value={sprintId}
                onChange={(e) => setSprintId(e.target.value)}
                disabled={!selectedBoardId || loadingSprints}
              >
                <option value="">
                  {loadingSprints ? "Loading sprints..." : "— Select Sprint —"}
                </option>
                {sprints.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} {s.state ? `(${s.state})` : ""}
                  </option>
                ))}
              </select>
            </>
          )}

          {filterType === "VERSION" && (
            <select
              className="jip-select"
              value={versionId}
              onChange={(e) => setVersionId(e.target.value)}
            >
              <option value="">— Select Version —</option>
              {versions.map((v) => (
                <option key={v.id} value={v.id}>{v.name}</option>
              ))}
            </select>
          )}

          {filterType === "LABEL" && (
            <div className="jip-label-wrap">
              <input
                className="jip-input"
                placeholder="Type to search labels..."
                value={labelQuery}
                onChange={(e) => { setLabelQuery(e.target.value); setLabel(e.target.value); }}
                list="jip-label-list"
              />
              <datalist id="jip-label-list">
                {labelSuggestions.map((l) => <option key={l} value={l} />)}
              </datalist>
            </div>
          )}

          <button
            className="jip-fetch-btn"
            onClick={fetchIssues}
            disabled={loading || !canFetch}
          >
            {loading ? <span className="jip-spinner" /> : (
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
              </svg>
            )}
            {loading ? "Fetching..." : "Fetch Issues"}
          </button>
        </div>
      </div>

      {/*  Error  */}
      {error && <div className="jip-error">{error}</div>}

      {/*  Empty state  */}
      {!loading && fetched && issues.length === 0 && (
        <div className="jip-empty">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
            <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2" />
            <rect x="9" y="3" width="6" height="4" rx="1" />
          </svg>
          <p>No issues found for this filter.</p>
        </div>
      )}

      {/*  Table  */}
      {!loading && issues.length > 0 && (
        <div className="jip-table-wrap">
          <table className="jip-table">
            <thead>
              <tr>
                <th onClick={() => toggleSort("key")} className="jip-th-sortable">Key <SortIcon col="key" /></th>
                <th onClick={() => toggleSort("summary")} className="jip-th-sortable">Summary <SortIcon col="summary" /></th>
                <th>Type</th>
                <th onClick={() => toggleSort("status")} className="jip-th-sortable">Status <SortIcon col="status" /></th>
                <th onClick={() => toggleSort("priority")} className="jip-th-sortable">Priority <SortIcon col="priority" /></th>
                <th onClick={() => toggleSort("assignee")} className="jip-th-sortable">Assignee <SortIcon col="assignee" /></th>
                <th onClick={() => toggleSort("updated")} className="jip-th-sortable">Updated <SortIcon col="updated" /></th>
              </tr>
            </thead>
            <tbody>
              {sorted.map((issue) => (
                <tr key={issue.key} className="jip-row">
                  <td><span className="jip-key">{issue.key}</span></td>
                  <td className="jip-summary-cell">
                    <span className="jip-summary" title={issue.summary}>{issue.summary || "—"}</span>
                    {issue.description && (
                      <span className="jip-desc" title={issue.description}>{issue.description}</span>
                    )}
                  </td>
                  <td><span className="jip-type">{issue.issueType || "—"}</span></td>
                  <td><Badge label={issue.status} colorMap={STATUS_COLOR} /></td>
                  <td>
                    {issue.priority
                      ? <Badge label={issue.priority} colorMap={PRIORITY_COLOR} />
                      : <span className="jip-null">—</span>}
                  </td>
                  <td>
                    {issue.assignee ? (
                      <div className="jip-assignee">
                        <span className="jip-avatar">{issue.assignee.charAt(0).toUpperCase()}</span>
                        <span className="jip-assignee-name">{issue.assignee}</span>
                      </div>
                    ) : (
                      <span className="jip-null">Unassigned</span>
                    )}
                  </td>
                  <td className="jip-date">{formatDate(issue.updated)}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {/*  Load More  */}
          {!isLast && (
            <div className="jip-load-more-wrap">
              <button className="jip-load-more-btn" onClick={loadMore} disabled={loadingMore}>
                {loadingMore ? (
                  <><span className="jip-spinner jip-spinner-dark" /> Loading...</>
                ) : (
                  <>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                      <path d="M12 5v14M5 12l7 7 7-7" />
                    </svg>
                    Load More
                  </>
                )}
              </button>
            </div>
          )}

          {/*  All loaded  */}
          {isLast && issues.length > 0 && (
            <div className="jip-all-loaded">
              ✓ All {issues.length} issues loaded
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default JiraIssuesPreview;