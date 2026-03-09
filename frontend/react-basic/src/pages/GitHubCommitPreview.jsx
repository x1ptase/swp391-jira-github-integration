import { useState } from "react";
import "./GitHubCommitPreview.css";

const API_URL = "/api/groups";

const QUICK_FILTERS = [
  { label: "Last 7 days", days: 7 },
  { label: "Last 30 days", days: 30 },
  { label: "Last 90 days", days: 90 },
];

function GitHubCommitPreview({ groupId }) {
  //  Filter state 
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [lastNDays, setLastNDays] = useState(null);
  const [activeQuick, setActiveQuick] = useState(null);

  //  Commits state 
  const [commits, setCommits] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [fetched, setFetched] = useState(false);

  //  Sync state 
  const [syncing, setSyncing] = useState(false);
  const [syncResult, setSyncResult] = useState(null);

  //  Search state 
  const [search, setSearch] = useState("");

  const authHeader = () => ({
    Authorization: `Bearer ${localStorage.getItem("token")}`,
    "Content-Type": "application/json",
  });

  //  Quick filter select ─
  const selectQuick = (days, label) => {
    setActiveQuick(label);
    setLastNDays(days);
    setFromDate("");
    setToDate("");
  };

  const handleFromDate = (val) => {
    setFromDate(val);
    setActiveQuick(null);
    setLastNDays(null);
  };

  const handleToDate = (val) => {
    setToDate(val);
    setActiveQuick(null);
    setLastNDays(null);
  };

  //  Sync GitHub data 
  const handleSync = async () => {
    if (syncing) return;
    setSyncing(true);
    setSyncResult(null);
    setError("");

    try {
      const res = await fetch(`${API_URL}/${groupId}/github/sync`, {
        method: "POST",
        headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
      });
      const data = await res.json();

      if (!res.ok) {
        if (res.status === 409) {
          setError("A sync is already running. Please wait.");
        } else {
          setError(data.message || "Sync failed");
        }
      } else {
        setSyncResult(data);
      }
    } catch {
      setError("Network error during sync");
    } finally {
      setSyncing(false);
    }
  };

  //  Fetch commits ─
  const fetchCommits = async () => {
    setError("");
    setLoading(true);
    setFetched(false);
    setCommits([]);

    const body = {};
    if (lastNDays) body.lastNDays = lastNDays;
    else {
      if (fromDate) body.fromDate = fromDate;
      if (toDate) body.toDate = toDate;
    }

    try {
      const res = await fetch(`${API_URL}/${groupId}/github/commits`, {
        method: "POST",
        headers: authHeader(),
        body: JSON.stringify(body),
      });
      const data = await res.json();

      if (!res.ok) {
        setError(data.message || "Failed to fetch commits");
      } else {
        setCommits(Array.isArray(data) ? data : (data.data || []));
        setFetched(true);
      }
    } catch {
      setError("Network error");
    } finally {
      setLoading(false);
    }
  };

  //  Filter by search 
  const filtered = commits.filter((c) => {
    if (!search.trim()) return true;
    const q = search.toLowerCase();
    return (
      c.sha?.toLowerCase().includes(q) ||
      c.message?.toLowerCase().includes(q) ||
      c.authorName?.toLowerCase().includes(q) ||
      c.authorEmail?.toLowerCase().includes(q)
    );
  });

  const formatDate = (iso) => {
    if (!iso) return "—";
    const d = new Date(iso);
    return d.toLocaleDateString("vi-VN", {
      day: "2-digit", month: "2-digit", year: "numeric",
    }) + " " + d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
  };

  const shortSha = (sha) => sha?.slice(0, 7) ?? "—";

  const firstLine = (msg) => {
    if (!msg) return "—";
    return msg.split("\n")[0];
  };

  const restLines = (msg) => {
    if (!msg) return "";
    const lines = msg.split("\n").slice(1).join("\n").trim();
    return lines;
  };

  return (
    <div className="ghcp-root">
      {/*  Header  */}
      <div className="ghcp-header">
        <div className="ghcp-title">
          <span className="ghcp-title-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z" />
            </svg>
          </span>
          <span>Commit History</span>
          {fetched && (
            <span className="ghcp-count-chip">
              {filtered.length}{filtered.length !== commits.length ? `/${commits.length}` : ""} commits
            </span>
          )}
        </div>

        <button
          className="ghcp-sync-btn"
          onClick={handleSync}
          disabled={syncing}
          title="Sync GitHub commits to system"
        >
          <svg
            width="14" height="14" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" strokeWidth="2.5"
            className={syncing ? "ghcp-spin" : ""}
          >
            <path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8" />
            <path d="M21 3v5h-5" />
          </svg>
          {syncing ? "Syncing..." : "Sync Commits"}
        </button>
      </div>

      {/*  Sync result  */}
      {syncResult && (
        <div className="ghcp-sync-result">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M20 6 9 17l-5-5" />
          </svg>
          {syncResult.message || "Sync complete"}
          {syncResult.insertedCount != null && (
            <span className="ghcp-sync-detail">
              · {syncResult.insertedCount} new, {syncResult.updatedCount} updated
            </span>
          )}
        </div>
      )}

      {/*  Filter bar  */}
      <div className="ghcp-filter-bar">
        {/* Quick filters */}
        <div className="ghcp-quick-row">
          <span className="ghcp-filter-label">Quick:</span>
          {QUICK_FILTERS.map((q) => (
            <button
              key={q.label}
              className={`ghcp-quick-btn ${activeQuick === q.label ? "active" : ""}`}
              onClick={() => selectQuick(q.days, q.label)}
            >
              {q.label}
            </button>
          ))}
        </div>

        {/* Date range + fetch */}
        <div className="ghcp-date-row">
          <div className="ghcp-date-group">
            <label className="ghcp-date-label">From</label>
            <input
              type="date"
              className="ghcp-date-input"
              value={fromDate}
              onChange={(e) => handleFromDate(e.target.value)}
            />
          </div>
          <span className="ghcp-date-sep">→</span>
          <div className="ghcp-date-group">
            <label className="ghcp-date-label">To</label>
            <input
              type="date"
              className="ghcp-date-input"
              value={toDate}
              onChange={(e) => handleToDate(e.target.value)}
            />
          </div>

          <button
            className="ghcp-fetch-btn"
            onClick={fetchCommits}
            disabled={loading}
          >
            {loading ? (
              <span className="ghcp-spinner" />
            ) : (
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
              </svg>
            )}
            {loading ? "Fetching..." : "Fetch Commits"}
          </button>
        </div>

        {/* Search */}
        {fetched && commits.length > 0 && (
          <div className="ghcp-search-row">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
            </svg>
            <input
              className="ghcp-search-input"
              placeholder="Filter by message, author, SHA..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            {search && (
              <button className="ghcp-search-clear" onClick={() => setSearch("")}>×</button>
            )}
          </div>
        )}
      </div>

      {/*  Error  */}
      {error && <div className="ghcp-error">{error}</div>}

      {/*  Empty state  */}
      {!loading && fetched && commits.length === 0 && (
        <div className="ghcp-empty">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
            <circle cx="12" cy="12" r="3" />
            <line x1="12" y1="3" x2="12" y2="9" />
            <line x1="12" y1="15" x2="12" y2="21" />
          </svg>
          <p>No commits found for this period.</p>
        </div>
      )}

      {/*  No search results  */}
      {!loading && fetched && commits.length > 0 && filtered.length === 0 && (
        <div className="ghcp-empty">
          <p>No commits match "<strong>{search}</strong>"</p>
        </div>
      )}

      {/*  Commit list  */}
      {!loading && filtered.length > 0 && (
        <div className="ghcp-list">
          {filtered.map((c, idx) => {
            const title = firstLine(c.message);
            const body = restLines(c.message);
            const authorName = c.authorName || "Unknown";
            const authorLogin = null;
            const date = c.date;

            return (
              <div key={c.sha || idx} className="ghcp-item">
                {/* SHA + date */}
                <div className="ghcp-item-meta">
                  <span className="ghcp-sha">{shortSha(c.sha)}</span>
                  <span className="ghcp-date">{formatDate(date)}</span>
                </div>

                {/* Message */}
                <div className="ghcp-item-body">
                  <p className="ghcp-msg-title">{title}</p>
                  {body && <p className="ghcp-msg-body">{body}</p>}
                </div>

                {/* Author */}
                <div className="ghcp-item-author">
                  <span className="ghcp-avatar">
                    {authorName.charAt(0).toUpperCase()}
                  </span>
                  <div className="ghcp-author-info">
                    <span className="ghcp-author-name">{authorName}</span>
                    {authorLogin && authorLogin !== authorName && (
                      <span className="ghcp-author-login">@{authorLogin}</span>
                    )}
                    {c.authorEmail && (
                      <span className="ghcp-author-email">{c.authorEmail}</span>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/*  Footer count  */}
      {!loading && fetched && filtered.length > 0 && (
        <div className="ghcp-footer">
          Showing {filtered.length} of {commits.length} commits
        </div>
      )}
    </div>
  );
}

export default GitHubCommitPreview;