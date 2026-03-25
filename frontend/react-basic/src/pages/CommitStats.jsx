import { useState, useEffect } from "react";
import "./CommitStats.css";

const API_URL = "/api/stats/commits";

const COLORS = ["#6366f1","#8b5cf6","#ec4899","#f59e0b","#10b981","#3b82f6","#ef4444","#14b8a6","#f97316","#06b6d4"];

function Avatar({ name, login, color }) {
  const initials = name
    ? name.split(" ").map((w) => w[0]).slice(0, 2).join("").toUpperCase()
    : (login || "?")[0].toUpperCase();
  const bg = color || COLORS[(initials.charCodeAt(0) || 0) % COLORS.length];
  return <div className="cs-avatar" style={{ background: bg }}>{initials}</div>;
}

function formatDate(iso) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("vi-VN", {
    day: "2-digit", month: "2-digit", year: "numeric",
    hour: "2-digit", minute: "2-digit",
  });
}

// ── Bar Chart ──────────────────────────────────────────────────────────────────
function CommitBarChart({ data }) {
  const max = Math.max(...data.map(d => d.commitCount || 0), 1);
  return (
    <div className="cs-chart-wrap">
      <div className="cs-chart-title">Commits per Contributor</div>
      <div className="cs-bar-chart">
        {data.map((s, i) => {
          const pct = ((s.commitCount || 0) / max) * 100;
          const name = s.authorFullName || s.authorName || s.authorLogin || "Unknown";
          const color = COLORS[i % COLORS.length];
          return (
            <div key={i} className="cs-bar-row">
              <div className="cs-bar-label" title={name}>{name}</div>
              <div className="cs-bar-track-h">
                <div className="cs-bar-fill-h" style={{ width: `${pct}%`, background: color }} />
                <span className="cs-bar-val">{s.commitCount || 0}</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ── Donut Chart ────────────────────────────────────────────────────────────────
function DonutChart({ data }) {
  const total = data.reduce((s, d) => s + (d.commitCount || 0), 0);
  if (total === 0) return null;

  const size = 180;
  const r = 70;
  const cx = size / 2;
  const cy = size / 2;
  const circumference = 2 * Math.PI * r;

  let cumulative = 0;
  const slices = data.map((d, i) => {
    const pct = (d.commitCount || 0) / total;
    const offset = circumference * (1 - cumulative);
    const dash = circumference * pct;
    cumulative += pct;
    return { ...d, pct, offset, dash, color: COLORS[i % COLORS.length] };
  });

  return (
    <div className="cs-chart-wrap">
      <div className="cs-chart-title">Commit Distribution</div>
      <div className="cs-donut-layout">
        <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
          {slices.map((s, i) => (
            <circle key={i} cx={cx} cy={cy} r={r}
              fill="none" stroke={s.color} strokeWidth="28"
              strokeDasharray={`${s.dash} ${circumference - s.dash}`}
              strokeDashoffset={s.offset}
              transform={`rotate(-90 ${cx} ${cy})`}
              style={{ transition: "stroke-dasharray 0.5s ease" }} />
          ))}
          <text x={cx} y={cy - 6} textAnchor="middle" className="cs-donut-total-num">{total}</text>
          <text x={cx} y={cy + 14} textAnchor="middle" className="cs-donut-total-label">commits</text>
        </svg>
        <div className="cs-donut-legend">
          {slices.map((s, i) => {
            const name = s.authorFullName || s.authorName || s.authorLogin || "Unknown";
            return (
              <div key={i} className="cs-legend-row">
                <span className="cs-legend-dot" style={{ background: s.color }} />
                <span className="cs-legend-name" title={name}>{name}</span>
                <span className="cs-legend-pct">{Math.round(s.pct * 100)}%</span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

// ── Main ───────────────────────────────────────────────────────────────────────
export default function CommitStats({ groupId }) {
  const [stats, setStats] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [emptyMessage, setEmptyMessage] = useState("");
  const [fetched, setFetched] = useState(false);
  const [search, setSearch] = useState("");
  const [sortKey, setSortKey] = useState("commitCount");
  const [sortDir, setSortDir] = useState("desc");
  const [view, setView] = useState("chart"); // "chart" | "table"

  const authHeader = () => ({ Authorization: `Bearer ${localStorage.getItem("token")}` });

  const fetchStats = async () => {
    setLoading(true); setError(""); setEmptyMessage("");
    try {
      const res = await fetch(`${API_URL}/${groupId}`, { headers: authHeader() });
      const data = await res.json();
      if (!res.ok) { setError(data.message || "Failed to fetch commit stats"); }
      else {
        setStats(data.data || []);
        if ((data.data || []).length === 0 && data.message) setEmptyMessage(data.message);
        setFetched(true);
      }
    } catch { setError("Network error"); }
    finally { setLoading(false); }
  };

  useEffect(() => { if (groupId) fetchStats(); }, [groupId]);

  const toggleSort = (key) => {
    if (sortKey === key) setSortDir(d => d === "asc" ? "desc" : "asc");
    else { setSortKey(key); setSortDir("desc"); }
  };

  const SortIcon = ({ col }) => {
    if (sortKey !== col) return <span className="cs-sort-icon">↕</span>;
    return <span className="cs-sort-icon active">{sortDir === "asc" ? "↑" : "↓"}</span>;
  };

  const filtered = stats.filter(s => {
    if (!search.trim()) return true;
    const q = search.toLowerCase();
    return s.authorFullName?.toLowerCase().includes(q) ||
      s.authorName?.toLowerCase().includes(q) ||
      s.authorEmail?.toLowerCase().includes(q) ||
      s.authorLogin?.toLowerCase().includes(q);
  });

  const sorted = [...filtered].sort((a, b) => {
    let av = a[sortKey] ?? 0, bv = b[sortKey] ?? 0;
    if (av < bv) return sortDir === "asc" ? -1 : 1;
    if (av > bv) return sortDir === "asc" ? 1 : -1;
    return 0;
  });

  const totalCommits = stats.reduce((s, d) => s + (d.commitCount || 0), 0);

  return (
    <div className="cs-root">
      {/* Header */}
      <div className="cs-header">
        <div className="cs-title">
          <span className="cs-title-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z"/>
            </svg>
          </span>
          <span>Commit Statistics</span>
          {fetched && stats.length > 0 && (
            <span className="cs-count-chip">{stats.length} contributors · {totalCommits} commits</span>
          )}
        </div>
        <div className="cs-header-right">
          {fetched && stats.length > 0 && (
            <div className="cs-view-toggle">
              <button className={`cs-view-btn ${view === "chart" ? "active" : ""}`} onClick={() => setView("chart")}>
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                  <rect x="3" y="12" width="4" height="9"/><rect x="10" y="7" width="4" height="14"/>
                  <rect x="17" y="3" width="4" height="18"/>
                </svg>
                Chart
              </button>
              <button className={`cs-view-btn ${view === "table" ? "active" : ""}`} onClick={() => setView("table")}>
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                  <path d="M3 3h18v18H3z"/><path d="M3 9h18M3 15h18M9 3v18"/>
                </svg>
                Table
              </button>
            </div>
          )}
          <button className="cs-refresh-btn" onClick={fetchStats} disabled={loading}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
              className={loading ? "cs-spin" : ""}>
              <path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8"/>
              <path d="M21 3v5h-5"/>
            </svg>
            {loading ? "Loading..." : "Refresh"}
          </button>
        </div>
      </div>

      {error && <div className="cs-error">{error}</div>}
      {loading && <div className="cs-loading"><span className="cs-spinner"/><span>Loading...</span></div>}

      {!loading && fetched && stats.length === 0 && (
        <div className="cs-empty">
          <p>{emptyMessage || "No commit statistics available."}</p>
          <span>Sync GitHub data first to populate statistics.</span>
        </div>
      )}

      {/* Chart view */}
      {!loading && sorted.length > 0 && view === "chart" && (
        <div className="cs-charts-grid">
          <CommitBarChart data={sorted} />
          <DonutChart data={sorted} />
        </div>
      )}

      {/* Table view */}
      {!loading && sorted.length > 0 && view === "table" && (
        <>
          <div className="cs-search-wrap">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
            </svg>
            <input className="cs-search-input" placeholder="Filter by name, email, GitHub login..."
              value={search} onChange={e => setSearch(e.target.value)} />
            {search && <button className="cs-clear-btn" onClick={() => setSearch("")}>×</button>}
          </div>
          <div className="cs-table-wrap">
            <table className="cs-table">
              <thead>
                <tr>
                  <th>Contributor</th>
                  <th onClick={() => toggleSort("commitCount")} className="cs-th-sortable">Commits <SortIcon col="commitCount"/></th>
                  <th onClick={() => toggleSort("totalAdditions")} className="cs-th-sortable">Additions <SortIcon col="totalAdditions"/></th>
                  <th onClick={() => toggleSort("totalDeletions")} className="cs-th-sortable">Deletions <SortIcon col="totalDeletions"/></th>
                  <th onClick={() => toggleSort("totalFilesChanged")} className="cs-th-sortable">Files <SortIcon col="totalFilesChanged"/></th>
                  <th onClick={() => toggleSort("latestCommitDate")} className="cs-th-sortable">Last Commit <SortIcon col="latestCommitDate"/></th>
                </tr>
              </thead>
              <tbody>
                {sorted.map((s, i) => {
                  const displayName = s.authorFullName || s.authorName || s.authorLogin || "Unknown";
                  const subLabel = s.authorLogin ? `@${s.authorLogin}` : s.authorEmail || "";
                  const isMapped = !!s.authorUserId;
                  const color = COLORS[i % COLORS.length];
                  return (
                    <tr key={i} className="cs-row">
                      <td>
                        <div className="cs-contributor">
                          <Avatar name={displayName} login={s.authorLogin} color={color} />
                          <div className="cs-contributor-info">
                            <span className="cs-contributor-name">
                              {displayName}
                              {isMapped && <span className="cs-mapped-badge">✓ mapped</span>}
                            </span>
                            {subLabel && <span className="cs-contributor-sub">{subLabel}</span>}
                          </div>
                        </div>
                      </td>
                      <td><span className="cs-bar-count" style={{ color }}>{s.commitCount || 0}</span></td>
                      <td>{s.totalAdditions != null ? <span className="cs-additions">+{s.totalAdditions.toLocaleString()}</span> : <span className="cs-null">—</span>}</td>
                      <td>{s.totalDeletions != null ? <span className="cs-deletions">-{s.totalDeletions.toLocaleString()}</span> : <span className="cs-null">—</span>}</td>
                      <td>{s.totalFilesChanged != null ? <span className="cs-files">{s.totalFilesChanged.toLocaleString()}</span> : <span className="cs-null">—</span>}</td>
                      <td className="cs-date">{formatDate(s.latestCommitDate)}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}