import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./StudentDashboard.css";

const API_URL = "/api/student_group";



export default function StudentDashboard() {
  const navigate = useNavigate();
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showEnrollModal, setShowEnrollModal] = useState(false);
  const [classSearch, setClassSearch] = useState("");
  const [classResults, setClassResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [enrollingId, setEnrollingId] = useState(null);
  const [enrollSuccess, setEnrollSuccess] = useState("");
  const [enrollError, setEnrollError] = useState("");
  const [enrolledClass, setEnrolledClass] = useState(null);
  const [enrolledClassLoading, setEnrolledClassLoading] = useState(false);

  const auth = () => ({ Authorization: `Bearer ${localStorage.getItem("token")}` });
  const username = localStorage.getItem("username") || "";
  const authJson = () => ({ ...auth(), "Content-Type": "application/json" });

  useEffect(() => { fetchGroups(); fetchEnrolledClass(); }, []);


  const fetchEnrolledClass = async () => {
    setEnrolledClassLoading(true);
    try {
      const res = await fetch("/api/classes/me", { headers: auth() });
      if (!res.ok) { setEnrolledClass(null); return; }
      const data = await res.json();
      setEnrolledClass(data.data || null);
    } catch { setEnrolledClass(null); }
    finally { setEnrolledClassLoading(false); }
  };

  const searchClasses = async (keyword) => {
    setClassSearch(keyword);
    if (!keyword.trim()) { setClassResults([]); return; }
    setSearchLoading(true);
    try {
      const res = await fetch(`/api/classes?keyword=${encodeURIComponent(keyword)}&page=0&size=10`, { headers: auth() });
      const data = await res.json();
      setClassResults(data.data?.content || data.data || []);
    } catch { setClassResults([]); }
    finally { setSearchLoading(false); }
  };

  const handleEnroll = async (classId) => {
    setEnrollingId(classId); setEnrollError(""); setEnrollSuccess("");
    try {
      const res = await fetch(`/api/classes/${classId}/enroll`, { method: "POST", headers: authJson() });
      const data = await res.json();
      if (!res.ok) { setEnrollError(data.message || "Failed to enroll"); return; }
      setEnrollSuccess("Enrolled! Your lecturer can now add you to a group.");
      setClassResults(prev => prev.filter(c => c.classId !== classId));
      fetchEnrolledClass();
    } catch { setEnrollError("Network error"); }
    finally { setEnrollingId(null); }
  };

  const openEnrollModal = () => {
    setShowEnrollModal(true);
    setClassSearch(""); setClassResults([]);
    setEnrollError(""); setEnrollSuccess("");
  };

  const fetchGroups = async () => {
    setLoading(true); setError("");
    try {
      const res = await fetch(API_URL, { headers: auth() });
      const data = await res.json();
      if (!res.ok) { setError(data.message || "Failed to load groups"); return; }
      setGroups((data.data || []).filter(g => g.memberRole != null));
    } catch {
      setError("Network error");
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.clear();
    navigate("/login");
  };

  const isLeader = (g) => g.memberRole === "LEADER";

  return (
    <div className="sgl-root">
      {/* Navbar */}
      <div className="sgl-navbar">
        <div className="sgl-navbar-brand">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
            <circle cx="9" cy="7" r="4" />
            <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
          </svg>
          Student Portal
        </div>
        <div className="sgl-navbar-right">
          <span className="sgl-navbar-user">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
            {username}
          </span>
          <button className="sgl-logout-btn" onClick={handleLogout}>Logout</button>
        </div>
      </div>

      {/* Main */}
      <div className="sgl-main">
        <div className="sgl-page-header">
          <div>
            <h1 className="sgl-page-title">My Groups</h1>
            <p className="sgl-page-desc">Groups you are currently a member of</p>
          </div>

          <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
            
            <button className="sgl-refresh-btn" onClick={fetchGroups} disabled={loading}>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
                className={loading ? "sgl-spin" : ""}>
                <path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8" />
                <path d="M21 3v5h-5" />
              </svg>
              Refresh
            </button>
          </div>
        </div>

        {error && (
          <div className="sgl-error">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            {error}
          </div>
        )}

        {loading && (
          <div className="sgl-loading">
            <span className="sgl-spinner" />
            <span>Loading your groups...</span>
          </div>
        )}
        {/* Enrolled Class Section */}
        {(enrolledClass || enrolledClassLoading) && (
          <div className="sgl-enrolled-section">
            <div className="sgl-enrolled-header">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
              </svg>
              <span>My Enrolled Class</span>
            </div>
            {enrolledClassLoading ? (
              <div className="sgl-enrolled-loading"><span className="sgl-spinner-sm" /> Loading...</div>
            ) : enrolledClass && (
              <div className="sgl-enrolled-card">
                <div className="sgl-enrolled-card-left">
                  <div className="sgl-enrolled-icon">{enrolledClass.classCode?.charAt(0) || "C"}</div>
                  <div>
                    <div className="sgl-enrolled-code">{enrolledClass.classCode}</div>
                    <div className="sgl-enrolled-course">{enrolledClass.courseName || enrolledClass.courseCode}</div>
                  </div>
                </div>
                <div className="sgl-enrolled-card-right">
                  <div className="sgl-enrolled-meta">
                    <span className="sgl-enrolled-label">Semester</span>
                    <span className="sgl-enrolled-val">{enrolledClass.semesterCode}</span>
                  </div>
                  <div className="sgl-enrolled-meta">
                    <span className="sgl-enrolled-label">Lecturer</span>
                    <span className="sgl-enrolled-val">{enrolledClass.lecturerName || "Not assigned"}</span>
                  </div>
                </div>
                <div className="sgl-enrolled-status">
                  <span className="sgl-enrolled-badge">Enrolled</span>
                  {groups.length === 0 && (
                    <span className="sgl-enrolled-hint">Waiting to be added to a group</span>
                  )}
                </div>
              </div>
            )}
          </div>
        )}

        {!loading && groups.length === 0 && !error && (
          <div className="sgl-empty">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
            <p>You are not in any group yet.</p>
            <span>Contact your lecturer to be added to a group.</span>
          </div>
        )}

        {!loading && groups.length > 0 && (
          <div className="sgl-grid">
            {groups.map((g) => (
              <div key={g.groupId} className="sgl-card">
                {/* Card header */}
                <div className="sgl-card-header">
                  <div className="sgl-card-icon">
                    {g.groupName?.charAt(0).toUpperCase() || "G"}
                  </div>
                  <div className="sgl-card-title-wrap">
                    <h3 className="sgl-card-title">{g.groupName}</h3>
                    <span className="sgl-card-code">{g.classCode}</span>
                  </div>
                  {g.memberRole && (
                    <span className={`sgl-role-badge ${isLeader(g) ? "sgl-leader" : "sgl-member"}`}>
                      {isLeader(g) && (
                        <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                        </svg>
                      )}
                      {g.memberRole}
                    </span>
                  )}
                </div>

                {/* Card body */}
                <div className="sgl-card-body">
                  <div className="sgl-card-row">
                    <span className="sgl-card-label">Course</span>
                    <span className="sgl-card-val">{g.courseCode || "—"}</span>
                  </div>
                  <div className="sgl-card-row">
                    <span className="sgl-card-label">Semester</span>
                    <span className="sgl-card-val">{g.semesterCode || "—"}</span>
                  </div>
                  <div className="sgl-card-row">
                    <span className="sgl-card-label">Lecturer</span>
                    <span className="sgl-card-val">{g.lecturerName || "—"}</span>
                  </div>
                </div>

                {/* Card actions — tất cả đều có nút Group Detail */}
                <div className="sgl-card-actions">
                  <button
                    className={isLeader(g) ? "sgl-btn-manage" : "sgl-btn-view"}
                    onClick={() => navigate(`/leader/groups/${g.groupId}?memberRole=${g.memberRole || "MEMBER"}`)}
                  >
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                      {isLeader(g)
                        ? <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                        : <><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" /><circle cx="12" cy="12" r="3" /></>
                      }
                    </svg>
                    Group Detail
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
      {showEnrollModal && (
        <div className="sgl-modal-overlay" onClick={() => setShowEnrollModal(false)}>
          <div className="sgl-modal" onClick={e => e.stopPropagation()}>
            <div className="sgl-modal-header">
              <div>
                <div className="sgl-modal-title">Find &amp; Enroll Class</div>
                <div className="sgl-modal-subtitle">Search by class code to enroll</div>
              </div>
              <button className="sgl-modal-close" onClick={() => setShowEnrollModal(false)}>×</button>
            </div>
            <div className="sgl-modal-body">
              <div className="sgl-search-wrap">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
                </svg>
                <input className="sgl-search-input" placeholder="Search class code, e.g. SE1921..."
                  value={classSearch} onChange={e => searchClasses(e.target.value)} autoFocus />
                {searchLoading && <span className="sgl-spinner-sm" />}
              </div>
              {enrollSuccess && <div className="sgl-enroll-success">{enrollSuccess}</div>}
              {enrollError && <div className="sgl-enroll-error">{enrollError}</div>}
              {classResults.length > 0 && (
                <table className="sgl-enroll-table">
                  <thead><tr><th>Class Code</th><th>Course</th><th>Semester</th><th>Action</th></tr></thead>
                  <tbody>
                    {classResults.map(c => (
                      <tr key={c.classId}>
                        <td><strong>{c.classCode}</strong></td>
                        <td>{c.courseCode}</td>
                        <td><span className="sgl-sem-badge">{c.semesterCode}</span></td>
                        <td>
                          <button className="sgl-btn-enroll" onClick={() => handleEnroll(c.classId)} disabled={enrollingId === c.classId}>
                            {enrollingId === c.classId ? <span className="sgl-spinner-sm" /> : "Enroll"}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              {classSearch && !searchLoading && classResults.length === 0 && (
                <div className="sgl-no-results">No classes found for "{classSearch}"</div>
              )}
              {!classSearch && <div className="sgl-search-hint">Type a class code to search</div>}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}