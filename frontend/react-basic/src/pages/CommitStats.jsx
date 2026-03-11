import { useState, useEffect } from "react";
import "./CommitStats.css";

const API_URL = "/api/stats/commits";

function Avatar({ name, login }) {
  const initials = name
    ? name.split(" ").map((w) => w[0]).slice(0, 2).join("").toUpperCase()
    : (login || "?")[0].toUpperCase();
  const colors = ["#6366f1","#8b5cf6","#ec4899","#f59e0b","#10b981","#3b82f6","#ef4444","#14b8a6"];
  const color = colors[(initials.charCodeAt(0) || 0) % colors.length];
  return (
    <div className="cs-avatar" style={{ background: color }}>
      {initials}
    </div>
  );
}

function CommitBar({ count, max }) {
  const pct = max > 0 ? (count / max) * 100 : 0;
  return (
    <div className="cs-bar-wrap">
      <div className="cs-bar-track">
        <div className="cs-bar-fill" style={{ width: `${pct}%` }} />
      </div>
      <span className="cs-bar-count">{count}</span>
    </div>
  );
}

function formatDate(iso) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("vi-VN", {
    day: "2-digit", month: "2-digit", year: "numeric",
    hour: "2-digit", minute: "2-digit",
  });
}

export default function CommitStats({ groupId }) {
  const [stats, setStats] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [emptyMessage, setEmptyMessage] = useState("");
  const [fetched, setFetched] = useState(false);
  const [search, setSearch] = useState("");
  const [sortKey, setSortKey] = useState("commitCount");
  const [sortDir, setSortDir] = useState("desc");

  const authHeader = () => ({
    Authorization: `Bearer ${localStorage.getItem("token")}`,
  });

  const fetchStats = async () => {
    setLoading(true);
    setError("");
    setEmptyMessage("");
    try {
      const res = await fetch(`${API_URL}/${groupId}`, { headers: authHeader() });
      const data = await res.json();
      if (!res.ok) {
        setError(data.message || "Failed to fetch commit stats");
      } else {
        setStats(data.data || []);
        if ((data.data || []).length === 0 && data.message) {
          setEmptyMessage(data.message);
        }
        setFetched(true);
      }
    } catch {
      setError("Network error");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (groupId) fetchStats();
  }, [groupId]);

  const toggleSort = (key) => {
    if (sortKey === key) setSortDir((d) => (d === "asc" ? "desc" : "asc"));
    else { setSortKey(key); setSortDir("desc"); }
  };

  const SortIcon = ({ col }) => {
    if (sortKey !== col) return <span className="cs-sort-icon">↕</span>;
    return <span className="cs-sort-icon active">{sortDir === "asc" ? "↑" : "↓"}</span>;
  };

  const filtered = stats.filter((s) => {
    if (!search.trim()) return true;
    const q = search.toLowerCase();
    return (
      s.authorFullName?.toLowerCase().includes(q) ||
      s.authorName?.toLowerCase().includes(q) ||
      s.authorEmail?.toLowerCase().includes(q) ||
      s.authorLogin?.toLowerCase().includes(q)
    );
  });

  const sorted = [...filtered].sort((a, b) => {
    let av = a[sortKey] ?? 0;
    let bv = b[sortKey] ?? 0;
    if (av < bv) return sortDir === "asc" ? -1 : 1;
    if (av > bv) return sortDir === "asc" ? 1 : -1;
    return 0;
  });

  const maxCommits = Math.max(...stats.map((s) => s.commitCount || 0), 1);
  const totalCommits = stats.reduce((sum, s) => sum + (s.commitCount || 0), 0);

  return (
    <div className="cs-root">
      {/*  Header  */}
      <div className="cs-header">
        <div className="cs-title">
          <span className="cs-title-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z"/>
            </svg>
          </span>
          <span>Commit Statistics</span>
          {fetched && stats.length > 0 && (
            <span className="cs-count-chip">{stats.length} contributors · {totalCommits} total commits</span>
          )}
        </div>

        <button className="cs-refresh-btn" onClick={fetchStats} disabled={loading} title="Refresh">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
            className={loading ? "cs-spin" : ""}>
            <path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8"/>
            <path d="M21 3v5h-5"/>
          </svg>
          {loading ? "Loading..." : "Refresh"}
        </button>
      </div>

      {/*  Search  */}
      {fetched && stats.length > 0 && (
        <div className="cs-search-wrap">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
          </svg>
          <input
            className="cs-search-input"
            placeholder="Filter by name, email, GitHub login..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          {search && <button className="cs-clear-btn" onClick={() => setSearch("")}>×</button>}
          {search && (
            <span className="cs-filter-chip">{filtered.length}/{stats.length}</span>
          )}
        </div>
      )}

      {/*  Error  */}
      {error && <div className="cs-error">{error}</div>}

      {/*  Loading  */}
      {loading && (
        <div className="cs-loading">
          <span className="cs-spinner" />
          <span>Loading commit statistics...</span>
        </div>
      )}

      {/*  Empty  */}
      {!loading && fetched && stats.length === 0 && (
        <div className="cs-empty">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="currentColor" opacity="0.15">
            <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z"/>
          </svg>
          <p>{emptyMessage || "No commit statistics available."}</p>
          <span>Sync GitHub data first to populate statistics.</span>
        </div>
      )}

      {/*  Table  */}
      {!loading && sorted.length > 0 && (
        <div className="cs-table-wrap">
          <table className="cs-table">
            <thead>
              <tr>
                <th>Contributor</th>
                <th onClick={() => toggleSort("commitCount")} className="cs-th-sortable">
                  Commits <SortIcon col="commitCount" />
                </th>
                <th onClick={() => toggleSort("totalAdditions")} className="cs-th-sortable">
                  Additions <SortIcon col="totalAdditions" />
                </th>
                <th onClick={() => toggleSort("totalDeletions")} className="cs-th-sortable">
                  Deletions <SortIcon col="totalDeletions" />
                </th>
                <th onClick={() => toggleSort("totalFilesChanged")} className="cs-th-sortable">
                  Files <SortIcon col="totalFilesChanged" />
                </th>
                <th onClick={() => toggleSort("latestCommitDate")} className="cs-th-sortable">
                  Last Commit <SortIcon col="latestCommitDate" />
                </th>
              </tr>
            </thead>
            <tbody>
              {sorted.map((s, i) => {
                const displayName = s.authorFullName || s.authorName || s.authorLogin || "Unknown";
                const subLabel = s.authorLogin
                  ? `@${s.authorLogin}`
                  : s.authorEmail || "";
                const isMapped = !!s.authorUserId;
                return (
                  <tr key={i} className="cs-row">
                    <td>
                      <div className="cs-contributor">
                        <Avatar name={displayName} login={s.authorLogin} />
                        <div className="cs-contributor-info">
                          <span className="cs-contributor-name">
                            {displayName}
                            {isMapped && <span className="cs-mapped-badge">✓ mapped</span>}
                          </span>
                          {subLabel && (
                            <span className="cs-contributor-sub">{subLabel}</span>
                          )}
                          {s.authorEmail && s.authorLogin && (
                            <span className="cs-contributor-email">{s.authorEmail}</span>
                          )}
                        </div>
                      </div>
                    </td>
                    <td>
                      <CommitBar count={s.commitCount || 0} max={maxCommits} />
                    </td>
                    <td>
                      {s.totalAdditions != null
                        ? <span className="cs-additions">+{s.totalAdditions.toLocaleString()}</span>
                        : <span className="cs-null">—</span>}
                    </td>
                    <td>
                      {s.totalDeletions != null
                        ? <span className="cs-deletions">-{s.totalDeletions.toLocaleString()}</span>
                        : <span className="cs-null">—</span>}
                    </td>
                    <td>
                      {s.totalFilesChanged != null
                        ? <span className="cs-files">{s.totalFilesChanged.toLocaleString()}</span>
                        : <span className="cs-null">—</span>}
                    </td>
                    <td className="cs-date">{formatDate(s.latestCommitDate)}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}