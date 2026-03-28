import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./LogoutButton.css";

export default function LogoutButton() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const username = localStorage.getItem("username") || "";

  const handleLogout = async () => {
    setLoading(true);
    try {
      await fetch("/api/auth/logout", {
        method: "POST",
        headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
      });
    } catch {}
    localStorage.clear();
    navigate("/login");
  };

  return (
    <div className="lgb-wrap">
      {username && <span className="lgb-username">Welcome, {username}</span>}
      <button className="lgb-btn" onClick={handleLogout} disabled={loading}>
        {loading ? <span className="lgb-spinner"/> : (
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
            <polyline points="16 17 21 12 16 7"/>
            <line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
        )}
        Logout
      </button>
    </div>
  );
}