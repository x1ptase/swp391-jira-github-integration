import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import logo from "../assets/logo.png";
import "./LecturerClassList.css";

// helpers
const STATUS_CONFIG = {
  HEALTHY: { label: "Healthy", dot: "#10b981", bg: "#d1fae5", text: "#065f46" },
  WARNING: { label: "Warning", dot: "#8b5cf6", bg: "#ede9fe", text: "#4c1d95" },
  CRITICAL: { label: "Critical", dot: "#ef4444", bg: "#fee2e2", text: "#991b1b" },
  CLOSED: { label: "Closed", dot: "#94a3b8", bg: "#f1f5f9", text: "#475569" },
};

const username = localStorage.getItem("username") || "";

function deriveClassStatus(summary) {
  if (!summary) return null;
  const groupsAtRisk = summary.atRisk ?? summary.groupsAtRisk ?? 0;
  const studentsFlagged = summary.studentsFlagged ?? 0;
  if (groupsAtRisk >= 5 || studentsFlagged >= 10) return "CRITICAL";
  if (groupsAtRisk >= 2 || studentsFlagged >= 3) return "WARNING";
  return "HEALTHY";
}

function StatusBadge({ status }) {
  const cfg = STATUS_CONFIG[status] ?? STATUS_CONFIG.HEALTHY;
  return (
    <span style={{
      display: "inline-flex", alignItems: "center", gap: 5,
      padding: "3px 10px", borderRadius: 999, fontSize: 11, fontWeight: 700,
      letterSpacing: "0.06em", textTransform: "uppercase",
      background: cfg.bg, color: cfg.text,
    }}>
      <span style={{
        width: 7, height: 7, borderRadius: "50%", background: cfg.dot,
        flexShrink: 0,
        boxShadow: status === "CRITICAL" ? `0 0 0 2px ${cfg.dot}44` : "none",
      }} />
      {cfg.label}
    </span>
  );
}

// icons
const IconGroups = () => (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
    <circle cx="9" cy="7" r="4" />
    <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
  </svg>
);
const IconRisk = () => (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
    <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
  </svg>
);
const IconFlag = () => (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
    <circle cx="9" cy="7" r="4" />
    <line x1="19" y1="8" x2="19" y2="14" />
    <line x1="22" y1="11" x2="16" y2="11" />
  </svg>
);
const IconRefresh = ({ spinning }) => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
    style={spinning ? { animation: "spin 0.9s linear infinite" } : {}}>
    <path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8" />
    <path d="M21 3v5h-5" />
  </svg>
);
const IconSearch = () => (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" strokeWidth="2">
    <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
  </svg>
);

const IconLogout = () => (
  <svg
    width="18"
    height="18"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    strokeLinecap="round"
    strokeLinejoin="round"
  >
    {/* cửa */}
    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />

    {/* mũi tên đi ra */}
    <polyline points="16 17 21 12 16 7" />
    <line x1="21" y1="12" x2="9" y2="12" />
  </svg>
);

// main component
export default function LecturerClassList() {
  const navigate = useNavigate();
  const token = localStorage.getItem("token");
  const auth = useCallback(() => ({ Authorization: `Bearer ${token}` }), [token]);

  const [classes, setClasses] = useState([]);
  const [semesters, setSemesters] = useState([]);
  const [summaries, setSummaries] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [keyword, setKeyword] = useState("");
  const [semester, setSemester] = useState("");

  // data fetching
  const fetchClasses = useCallback(async () => {
    setLoading(true); setError("");
    try {
      const res = await fetch("/api/lecturer/classes", { headers: auth() });
      if (!res.ok) throw new Error("Failed to fetch classes");
      const data = await res.json();
      setClasses(data.data || []);
    } catch (err) {
      setError(err.message || "Error occurred");
    } finally {
      setLoading(false);
    }
  }, [auth]);

  const handleLogout = () => {
    localStorage.clear();
    window.location.href = "/login";
  };

  const fetchSemesters = useCallback(async () => {
    try {
      const res = await fetch("/api/semesters/list", { headers: auth() });
      const data = await res.json();
      setSemesters(data.data?.content || data.data || []);
    } catch { setSemesters([]); }
  }, [auth]);

  const fetchSummary = useCallback(async (classId) => {
    try {
      const now = new Date();
      const from = new Date(now); from.setDate(from.getDate() - 30);
      const params = new URLSearchParams({
        fromDate: from.toISOString(),
        toDate: now.toISOString(),
      });
      const res = await fetch(
        `/api/classes/${classId}/monitoring/summary?${params}`,
        { headers: auth() }
      );
      const data = await res.json();
      setSummaries(prev => ({ ...prev, [classId]: data.data }));
    } catch { /* silent */ }
  }, [auth]);

  useEffect(() => { fetchClasses(); fetchSemesters(); }, [fetchClasses, fetchSemesters]);
  useEffect(() => { classes.forEach(c => fetchSummary(c.classId)); }, [classes, fetchSummary]);

  // filter
  const filtered = classes.filter(c => {
    const matchKw = !keyword.trim() || c.classCode?.toLowerCase().includes(keyword.toLowerCase());
    const matchSem = !semester || c.semesterCode === semester;
    return matchKw && matchSem;
  });

  const now = new Date();
  const lastUpdate = now.toLocaleString("en-US", { hour: "2-digit", minute: "2-digit", hour12: true });


  return (

    <div className="lcd-root">
      {/* Navbar */}
      <nav className="lcd-nav">
        <img src={logo} alt="Logo" className="lcd-nav-logo" />
        <span className="lcd-nav-brand">The Jira & Github Tool Support</span>
        <div className="lcd-nav-spacer" />
        <div className="lcd-nav-user">
          <div className="lcd-nav-userinfo">
            <div className="lcd-nav-name">{username}</div>
            <div className="lcd-nav-role">Academic Lecturer</div>
          </div>
          <button className="lcd-nav-icon-btn" title="logout" onClick={handleLogout}>
            <IconLogout />
          </button>
        </div>
      </nav>

      {/* Main */}
      <main className="lcd-main">
        {/* Header */}
        <div className="lcd-header">
          <div className="lcd-title-block">
            <h1 className="lcd-title">My Classes Dashboard</h1>
            <p className="lcd-subtitle">Manage and monitor academic performance for the current term.</p>
          </div>

          <div className="lcd-controls">
            {/* Semester dropdown */}
            <div className="lcd-select-wrap">
              <select
                className="lcd-select"
                value={semester}
                onChange={e => setSemester(e.target.value)}
              >
                <option value="">All Semesters</option>
                {semesters.map(s => (
                  <option key={s.semesterId} value={s.semesterCode}>{s.semesterCode}</option>
                ))}
              </select>
            </div>

            {/* Search */}
            <div className="lcd-search-wrap">
              <span className="lcd-search-icon"><IconSearch /></span>
              <input
                className="lcd-search"
                type="text"
                placeholder="Search class..."
                value={keyword}
                onChange={e => setKeyword(e.target.value)}
              />
            </div>

            {/* Refresh */}
            <button className="lcd-refresh-btn" onClick={fetchClasses} disabled={loading}>
              <IconRefresh spinning={loading} />
            </button>
          </div>
        </div>

        {/* Error */}
        {error && <div className="lcd-error">{error}</div>}

        {/* Loading */}
        {loading ? (
          <div className="lcd-grid">
            {[1, 2, 3].map(i => (
              <div key={i} className="lcd-skeleton-card" style={{ animationDelay: `${i * 0.12}s` }}>
                <div className="skel" style={{ height: 10, width: 80, marginBottom: 10 }} />
                <div className="skel" style={{ height: 34, width: 130, marginBottom: 20 }} />
                {[1, 2, 3].map(j => <div key={j} className="skel" style={{ height: 14, marginBottom: 12 }} />)}
                <div className="skel" style={{ height: 42, marginTop: 12, borderRadius: 10 }} />
              </div>
            ))}
          </div>
        ) : classes.length === 0 ? (
          <div className="lcd-empty">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#cbd5e1" strokeWidth="1.2">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" /><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
            </svg>
            <p>No classes assigned yet</p>
            <span>Contact your admin to assign you to classes</span>
          </div>
        ) : (
          <div className="lcd-grid">
            {filtered.map((cls, idx) => {
              const sum = summaries[cls.classId];
              const status = sum?.classHealth ?? cls.classHealth ?? deriveClassStatus(sum);
              const riskVal = sum !== undefined ? (sum?.atRisk ?? sum?.groupsAtRisk ?? 0) : null;
              const flaggedVal = sum !== undefined ? (sum?.studentsFlagged ?? 0) : null;

              return (
                <div
                  key={cls.classId}
                  className="lcd-card"
                  style={{ animationDelay: `${idx * 0.06}s` }}
                >
                  {/* Top row */}
                  <div className="lcd-card-top">
                    <div>
                      <div className="lcd-card-label">Course Code</div>
                      <div className="lcd-card-code">{cls.classCode}</div>
                    </div>
                    {status ? <StatusBadge status={status} /> : (
                      <span style={{ height: 22, width: 70, borderRadius: 999, background: "#f1f5f9", display: "block" }} />
                    )}
                  </div>

                  {/* Stats */}
                  <div className="lcd-stats">
                    <div className="lcd-stat-row">
                      <span className="lcd-stat-left"><IconGroups /> Total Groups</span>
                      <span className="lcd-stat-val">
                        {sum ? String(sum.totalGroups ?? 0).padStart(2, "0") : "—"}
                      </span>
                    </div>
                    <div className="lcd-stat-row">
                      <span className="lcd-stat-left"><IconRisk /> Groups at Risk</span>
                      <span className={`lcd-stat-val ${riskVal >= 5 ? "danger" : riskVal >= 2 ? "warn" : ""}`}>
                        {riskVal !== null ? riskVal : "—"}
                      </span>
                    </div>
                    <div className="lcd-stat-row">
                      <span className="lcd-stat-left"><IconFlag /> Students Flagged</span>
                      <span className={`lcd-stat-val ${flaggedVal >= 10 ? "danger" : flaggedVal >= 3 ? "warn" : ""}`}>
                        {flaggedVal !== null ? flaggedVal : "—"}
                      </span>
                    </div>
                  </div>

                  {/* CTA */}
                  <button
                    className="lcd-view-btn"
                    onClick={() => navigate(`/lecturer/classes/${cls.classId}`)}
                  >
                    View class
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                      <line x1="5" y1="12" x2="19" y2="12" /><polyline points="12 5 19 12 12 19" />
                    </svg>
                  </button>
                </div>
              );
            })}
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="lcd-footer">
        <div className="lcd-footer-item">
          <strong>Last Update</strong>
          Today at {lastUpdate}
        </div>
        <span className="lcd-footer-copy">© 2026 The Academic Jira & Github Support Tool</span>
      </footer>
    </div>
  );
}