import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "./LecturerClassList.css";
import LogoutButton from "./LogoutButton";
import logo from "../assets/logo.png";

const CLASS_API = "/api/lecturer/classes";

export default function LecturerClassList() {
  const [classes, setClasses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const token = localStorage.getItem("token");
  const auth = () => ({ Authorization: `Bearer ${token}` });

  useEffect(() => { fetchClasses(); }, []);
  // utils/color.js (hoặc đặt trong component)
  function hexToLuminance(hex) {
    const c = hex.replace("#", "");
    const r = parseInt(c.substring(0, 2), 16) / 255;
    const g = parseInt(c.substring(2, 4), 16) / 255;
    const b = parseInt(c.substring(4, 6), 16) / 255;
    const srgb = [r, g, b].map(v => (v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4)));
    return 0.2126 * srgb[0] + 0.7152 * srgb[1] + 0.0722 * srgb[2];
  }

  function pickColorFromId(id) {
    const palette = ["#60A5FA", "#FBBF24", "#A78BFA", "#FB7185", "#34D399", "#F97316", "#60C5A8", "#FBCFE8"];
    const sum = id.toString().split("").reduce((s, ch) => s + ch.charCodeAt(0), 0);
    const color = palette[sum % palette.length];
    const lum = hexToLuminance(color);
    // nếu nền sáng -> text tối, ngược lại text trắng
    const textColor = lum > 0.5 ? "#0f172a" : "#ffffff";
    return { color, textColor };
  }

  const fetchClasses = async () => {
    setLoading(true); setError("");
    try {
      const res = await fetch(CLASS_API, { headers: auth() });
      if (!res.ok) throw new Error("Failed to fetch classes");
      const data = await res.json();
      setClasses(data.data || []);
    } catch (err) {
      setError(err.message || "Error occurred");
    } finally {
      setLoading(false);
    }
  };

  function getColorFromId(id) {
    const colors = ["#FFCDD2", "#C8E6C9", "#BBDEFB", "#FFF9C4", "#D1C4E9", "#FFE0B2"];
    // Tạo chỉ số bằng cách cộng mã ASCII của các ký tự trong id
    const index = id
      .toString()
      .split("")
      .reduce((acc, char) => acc + char.charCodeAt(0), 0) % colors.length;
    return colors[index];
  }

  return (
    <div className="lcl-root">
      {/* Navbar */}
      <div className="lcl-navbar">
        <img src={logo} alt="Logo" className="lcl-navbar-logo" />
        <div className="lcl-navbar-brand">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" /><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
          </svg>
          Lecturer Portal
        </div>
        <LogoutButton />
      </div>

      {/* Main */}
      <div className="lcl-main">
        <div className="lcl-content-card">
          <div className="lcl-page-header">
            <div>
              <h1 className="lcl-page-title">My Assigned Classes</h1>
              <p className="lcl-page-desc">You are assigned to {classes.length} class(es)</p>
            </div>
            <button className="lcl-refresh-btn" onClick={fetchClasses} disabled={loading}>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
                style={loading ? { animation: "lcl-spin 1s linear infinite" } : {}}>
                <path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8" /><path d="M21 3v5h-5" />
              </svg>
              Refresh
            </button>
          </div>

          {error && <div className="lcl-error">{error}</div>}

          {loading ? (
            <div className="lcl-loading"><span className="lcl-spinner" />Loading classes...</div>
          ) : classes.length === 0 ? (
            <div className="lcl-empty">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.2">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" /><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
              </svg>
              <p>No classes assigned yet</p>
              <span>Contact your admin to assign you to classes</span>
            </div>
          ) : (
            <div className="lcl-grid">
              {classes.map(cls => {
                const { color: accent, textColor } = pickColorFromId(cls.classId);
                return (
                  <div key={cls.classId} className="lcl-card">
                    {/* Accent stripe (left) */}
                    <div className="lcl-card-accent" style={{ background: accent }} />
                    <div className="lcl-card-inner">
                      <div className="lcl-card-header">
                        <div className="lcl-card-icon" style={{ background: accent, color: textColor }}>
                          {cls.classCode?.charAt(0) || "C"}
                        </div>
                        <div className="lcl-card-title-wrap">
                          <h3 className="lcl-card-title">{cls.classCode}</h3>
                          <span className="lcl-course-badge">{cls.courseCode}</span>
                        </div>

                      </div>
                      <div className="lcl-card-body">
                        <div className="lcl-card-row">
                          <span className="lcl-card-label">Course</span>
                          <span className="lcl-card-val">{cls.courseName || cls.courseCode}</span>
                        </div>
                        <div className="lcl-card-row">
                          <span className="lcl-card-label">Semester</span>
                          <span className="lcl-card-val">{cls.semesterCode || "—"}</span>
                        </div>
                        <div className="lcl-card-row">
                          <span className="lcl-card-label">Lecturer</span>
                          <span className="lcl-card-val">{cls.lecturerName || "—"}</span>
                        </div>
                      </div>
                      <div className="lcl-card-actions">
                        <button className="lcl-btn-manage" onClick={() => navigate(`/lecturer/classes/${cls.classId}`)}>
                          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                            <circle cx="9" cy="7" r="4" />
                            <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
                          </svg>
                          Manage Class
                        </button>
                      </div>
                    </div>

                  </div>
                )
              }
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}