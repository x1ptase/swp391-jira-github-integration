import { useEffect, useState, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import "./StudentsWatchlist.css";

const PAGE_SIZE = 10;

function getInitials(name = "") {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(-2)
    .map((w) => w[0].toUpperCase())
    .join("");
}

function getAvatarColor(name = "") {
  const colors = [
    { bg: "#dbeafe", text: "#1d4ed8" }, // blue
    { bg: "#fce7f3", text: "#be185d" }, // pink
    { bg: "#e0e7ff", text: "#4338ca" }, // indigo
    { bg: "#d1fae5", text: "#065f46" }, // green
    { bg: "#fef3c7", text: "#92400e" }, // amber
    { bg: "#ede9fe", text: "#5b21b6" }, // violet
    { bg: "#fee2e2", text: "#991b1b" }, // red
  ];
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
  return colors[Math.abs(hash) % colors.length];
}

function normalizeStatus(status) {
  if (!status) return null;
  return status.toString().toUpperCase().replace(/-/g, "_").replace(/\s+/g, "_");
}

function getStatusBadge(status) {
  const s = normalizeStatus(status);
  if (s === "ACTIVE") return { className: "sw-badge-active", label: "ACTIVE" };
  if (s === "LOW" || s === "LOW_CONTRIBUTION") return { className: "sw-badge-low", label: "LOW" };
  if (s === "NO_CONTRIBUTION" || s === "NONE" || s === "INACTIVE")
    return { className: "sw-badge-no-contribution", label: "NO CONTRIBUTION" };
  return { className: "sw-badge-default", label: status || "—" };
}

function formatLastActive(dateStr) {
  if (!dateStr) return "—";
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now - date;
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
  if (diffDays === 0) return "Today";
  if (diffDays === 1) return "Yesterday";
  return `${diffDays} days ago`;
}


export default function StudentsWatchlist({ classId, classInfo }) {
  const navigate = useNavigate();
  const token = localStorage.getItem("token");

  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [groupFilter, setGroupFilter] = useState("all");
  const [statusFilter, setStatusFilter] = useState("all");
  const [page, setPage] = useState(1);
  const [totalCount, setTotalCount] = useState(0);

  // Source: real API data 
  const sourceList = useMemo(
    () => students,
    [students]
  );

  // Unique groups for filter dropdown
  const groupOptions = useMemo(() => {
    const groups = [...new Set(sourceList.map((s) => s.groupName).filter(Boolean))];
    return groups.sort();
  }, [sourceList]);

  const statusOptions = ["ACTIVE", "LOW", "NO CONTRIBUTION"];

  useEffect(() => {
    const fetchStudents = async () => {
      setLoading(true);
      try {
        const res = await fetch(`/api/classes/${classId}/monitoring/students`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        const data = await res.json();
        const list = data.data || [];
        setStudents(list);
        setTotalCount(list.length);
      } catch (err) {
        console.error("Failed to fetch student watchlist", err);
      } finally {
        setLoading(false);
      }
    };
    fetchStudents();
  }, [classId, token]);

  // Filtered + searched list
  const filtered = useMemo(() => {
    return sourceList.filter((s) => {
      const q = searchQuery.toLowerCase();
      const matchSearch =
        !q ||
        s.fullName?.toLowerCase().includes(q) ||
        s.studentCode?.toLowerCase().includes(q);
      const matchGroup =
        groupFilter === "all" || s.groupName === groupFilter;
      const matchStatus =
        statusFilter === "all" ||
        normalizeStatus(s.contributionStatus ?? s.status) === normalizeStatus(statusFilter);
      return matchSearch && matchGroup && matchStatus;
    });
  }, [sourceList, searchQuery, groupFilter, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const paginated = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const displayList = loading ? [] : paginated;
  const displayTotal = filtered.length;
  const classCode = classInfo?.classCode || "SE1931";

  return (
    <div className="sw-container">
      {/* Page Header */}
      <div className="sw-page-header">
        <h2 className="sw-page-title">{classCode} - Students Watchlist</h2>
        <p className="sw-page-subtitle">
          Monitor students with low or no contribution in the current week. Academic
          performance is curated based on repository activity and peer feedback.
        </p>
      </div>

      {/* Filter Bar */}
      <div className="sw-filter-bar">
        <div className="sw-search-wrapper">
          <svg className="sw-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8" />
            <path d="m21 21-4.35-4.35" />
          </svg>
          <input
            className="sw-search-input"
            type="text"
            placeholder="Search student by name or ID..."
            value={searchQuery}
            onChange={(e) => { setSearchQuery(e.target.value); setPage(1); }}
          />
        </div>

        <div className="sw-filter-group">
          <label className="sw-filter-label">GROUP</label>
          <select
            className="sw-filter-select"
            value={groupFilter}
            onChange={(e) => { setGroupFilter(e.target.value); setPage(1); }}
          >
            <option value="all">All Groups</option>
            {groupOptions.map((g) => (
              <option key={g} value={g}>{g}</option>
            ))}
          </select>
        </div>

        <div className="sw-filter-group">
          <label className="sw-filter-label">STATUS</label>
          <select
            className="sw-filter-select"
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value); setPage(1); }}
          >
            <option value="all">All Status</option>
            {statusOptions.map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Table */}
      <div className="sw-table-card">
        {loading ? (
          <div className="sw-loading">
            <div className="sw-spinner" />
            <span>Loading students...</span>
          </div>
        ) : (
          <>
            <table className="sw-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>STUDENT</th>
                  <th>CODE</th>
                  <th>GROUP</th>
                  <th>STATUS</th>
                  <th>LAST ACTIVE</th>
                  <th>ACTION</th>
                </tr>
              </thead>
              <tbody>
                {displayList.map((s, idx) => {
                  const initials = getInitials(s.fullName);
                  const avatarColor = getAvatarColor(s.fullName);
                  const rawStatus = s.contributionStatus ?? s.status;
                  const rawLastActive = s.lastActiveAt ?? s.lastActiveDate;
                  const badge = getStatusBadge(rawStatus);
                  const lastActive = formatLastActive(rawLastActive);

                  return (
                    <tr key={s.userId ?? s.studentId ?? idx}>
                      <td className="sw-td-num">{(page - 1) * PAGE_SIZE + idx + 1}</td>
                      <td className="sw-td-student">
                        <div
                          className="sw-avatar"
                          style={{ background: avatarColor.bg, color: avatarColor.text }}
                        >
                          {initials}
                        </div>
                        <span className="sw-student-name">{s.fullName}</span>
                      </td>
                      <td className="sw-td-code">{s.studentCode}</td>
                      <td className="sw-td-group">{s.groupName}</td>
                      <td>
                        <span className={`sw-badge ${badge.className}`}>{badge.label}</span>
                      </td>
                      <td className="sw-td-active">{lastActive}</td>
                      <td>
                        <button
                          className="sw-view-btn"
                          onClick={() => navigate(`/lecturer/groups/${s.groupId}/detail`)}
                        >
                          View group
                        </button>
                      </td>
                    </tr>
                  );
                })}
                {displayList.length === 0 && (
                  <tr>
                    <td colSpan={7} className="sw-empty">No students found.</td>
                  </tr>
                )}
              </tbody>
            </table>

            {/* Footer */}
            <div className="sw-table-footer">
              <span className="sw-footer-count">
                Showing {displayList.length} of {displayTotal} students in {classCode}
              </span>
              <div className="sw-pagination">
                <button
                  className="sw-page-btn"
                  disabled={page <= 1}
                  onClick={() => setPage((p) => p - 1)}
                >
                  ‹
                </button>
                <span className="sw-page-current">{page}</span>
                <button
                  className="sw-page-btn"
                  disabled={page >= totalPages}
                  onClick={() => setPage((p) => p + 1)}
                >
                  ›
                </button>
              </div>
            </div>
          </>
        )}
      </div>

      {/* Bottom Note Removed */}
    </div>
  );
}
