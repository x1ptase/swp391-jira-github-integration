import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./StudentDashboard.css";
import UpdateProfileModal from "./UpdateProfileModal";
import user from "../assets/user.png";
import logo from "../assets/logo.png";

const API_URL = "/api/student_group";

export default function StudentDashboard() {
  const navigate = useNavigate();
  const [showProfile, setShowProfile] = useState(false);
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

  useEffect(() => {
    fetchGroups();
    fetchEnrolledClass();
  }, []);

  const fetchEnrolledClass = async () => {
    setEnrolledClassLoading(true);
    try {
      const res = await fetch("/api/classes/me", { headers: auth() });
      if (!res.ok) { setEnrolledClass(null); return; }
      const data = await res.json();
      setEnrolledClass(data.data || null);
    } catch {
      setEnrolledClass(null);
    } finally {
      setEnrolledClassLoading(false);
    }
  };

  const searchClasses = async (keyword) => {
    setClassSearch(keyword);
    if (!keyword.trim()) {
      setClassResults([]);
      return;
    }
    setSearchLoading(true);
    try {
      const res = await fetch(`/api/classes?keyword=${encodeURIComponent(keyword)}&page=0&size=10`, { headers: auth() });
      const data = await res.json();
      setClassResults(data.data?.content || data.data || []);
    } catch {
      setClassResults([]);
    } finally {
      setSearchLoading(false);
    }
  };

  const handleEnroll = async (classId) => {
    setEnrollingId(classId);
    setEnrollError("");
    setEnrollSuccess("");
    try {
      const res = await fetch(`/api/classes/${classId}/enroll`, { method: "POST", headers: authJson() });
      const data = await res.json();
      if (!res.ok) {
        setEnrollError(data.message || "Failed to enroll");
        return;
      }
      setEnrollSuccess("Enrolled! Your lecturer can now add you to a group.");
      setClassResults(prev => prev.filter(c => c.classId !== classId));
      fetchEnrolledClass();
    } catch {
      setEnrollError("Network error");
    } finally {
      setEnrollingId(null);
    }
  };

  const fetchGroups = async () => {
    setLoading(true);
    setError("");
    try {
      const res = await fetch(API_URL, { headers: auth() });
      const data = await res.json();
      if (!res.ok) {
        setError(data.message || "Failed to load groups");
        return;
      }
      setGroups((data.data || []).filter(g => g.memberRole != null));
    } catch {
      setError("Network error");
    } finally {
      setLoading(false);
    }
  };



  const isLeader = (g) => g.memberRole === "LEADER";

  return (
    <div className="sd-root">
      {/* Navbar */}
      <header className="sd-navbar">
        <img src={logo} alt="Logo" className="lcl-navbar-logo" />
        <div className="sd-navbar-brand">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
            <circle cx="9" cy="7" r="4" />
            <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
          </svg>
          Student Portal
        </div>
        <div className="sd-navbar-actions">
          <span className="sd-navbar-user">
          </span>
          <button className="sd-profile-btn" onClick={() => setShowProfile(true)}>
            <img src={user} alt="profile icon" className="sd-profile-icon" /> {username}
          </button>
        </div>
      </header>

      {/* Main Content */}
      <main className="sd-main">
        <div className="sd-page-header">
          <div>
            <h1 className="sd-page-title">My Groups</h1>
            <p className="sd-page-desc">Groups you are currently a member of</p>
          </div>
          <div className="sd-header-actions">
            <button className="sd-refresh-btn" onClick={fetchGroups} disabled={loading}>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" className={loading ? "sd-spin" : ""}>
                <path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8" />
                <path d="M21 3v5h-5" />
              </svg>
              Refresh
            </button>
          </div>
        </div>


        {error && (
          <div className="sd-error">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            {error}
          </div>
        )}

        {loading && (
          <div className="sd-loading">
            <span className="sd-spinner" />
            <span>Loading your groups...</span>
          </div>
        )}

        {!loading && groups.length === 0 && !error && (
          <div className="sd-empty">
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
          <div className="sd-grid">
            {groups.map((g) => (
              <div key={g.groupId} className="sd-card">
                <div className="sd-card-header">
                  <div className="sd-card-icon">
                    {g.groupName?.charAt(0).toUpperCase() || "G"}
                  </div>
                  <div className="sd-card-title-wrap">
                    <h3 className="sd-card-title">{g.groupName}</h3>
                    <span className="sd-card-code">{g.classCode}</span>
                  </div>
                  <span className={`sd-role-badge ${isLeader(g) ? "sd-leader" : "sd-member"}`}>
                    {isLeader(g) && (
                      <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                      </svg>
                    )}
                    {g.memberRole}
                  </span>
                </div>

                <div className="sd-card-body">
                  <div className="sd-card-row">
                    <span className="sd-card-label">Course</span>
                    <span className="sd-card-val">{g.courseCode || "—"}</span>
                  </div>
                  <div className="sd-card-row">
                    <span className="sd-card-label">Semester</span>
                    <span className="sd-card-val">{g.semesterCode || "—"}</span>
                  </div>
                  <div className="sd-card-row">
                    <span className="sd-card-label">Lecturer</span>
                    <span className="sd-card-val">{g.lecturerName || "—"}</span>
                  </div>
                </div>

                <div className="sd-card-actions">
                  <button
                    className={isLeader(g) ? "sd-btn-manage" : "sd-btn-view"}
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
      </main>

      {/* Enroll Modal */}
      {showEnrollModal && (
        <div className="sd-modal-overlay" onClick={() => setShowEnrollModal(false)}>
          <div className="sd-modal" onClick={e => e.stopPropagation()}>
            <div className="sd-modal-header">
              <div>
                <div className="sd-modal-title">Find &amp; Enroll Class</div>
                <div className="sd-modal-subtitle">Search by class code to enroll</div>
              </div>
              <button className="sd-modal-close" onClick={() => setShowEnrollModal(false)}>×</button>
            </div>
            <div className="sd-modal-body">
              <div className="sd-search-wrap">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
                </svg>
                <input
                  className="sd-search-input"
                  placeholder="Enter class code..."
                  value={classSearch}
                  onChange={e => searchClasses(e.target.value)}
                />
                {searchLoading && <span className="sd-spinner-sm" />}
              </div>

              {enrollSuccess && <div className="sd-enroll-success">{enrollSuccess}</div>}
              {enrollError && <div className="sd-enroll-error">{enrollError}</div>}

              {classResults.length === 0 && classSearch.trim() !== "" && !searchLoading && (
                <div className="sd-no-results">No classes found</div>
              )}
              {classResults.length > 0 && (
                <table className="sd-enroll-table">
                  <thead>
                    <tr><th>Class Code</th><th>Course</th><th>Semester</th><th>Action</th></tr>
                  </thead>
                  <tbody>
                    {classResults.map(c => (
                      <tr key={c.classId}>
                        <td><strong>{c.classCode}</strong></td>
                        <td>{c.courseCode}</td>
                        <td><span className="sd-sem-badge">{c.semesterCode}</span></td>
                        <td>
                          <button
                            className="sd-btn-enroll"
                            onClick={() => handleEnroll(c.classId)}
                            disabled={enrollingId === c.classId}
                          >
                            {enrollingId === c.classId ? <span className="sd-spinner-sm" /> : "Enroll"}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              {classSearch.trim() === "" && !searchLoading && (
                <div className="sd-search-hint">Start typing to search for classes</div>
              )}
            </div>
          </div>
        </div>
      )}

      {showProfile && <UpdateProfileModal onClose={() => setShowProfile(false)} />}
    </div>
  );
}