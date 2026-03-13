import { useState, useEffect } from "react";
import "./GroupMemberList.css";

const API_URL = "/api/groups";

export default function GroupMemberList({ groupId }) {
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const auth = () => ({ Authorization: `Bearer ${localStorage.getItem("token")}` });

  useEffect(() => { if (groupId) fetchMembers(); }, [groupId]);

  const fetchMembers = async () => {
    setLoading(true); setError("");
    try {
      const res = await fetch(`${API_URL}/${groupId}/members`, { headers: auth() });
      const data = await res.json();
      if (!res.ok) { setError(data.message || "Failed to load members"); return; }
      setMembers(data.data || []);
    } catch {
      setError("Network error");
    } finally {
      setLoading(false);
    }
  };

  const leader = members.find(m => m.memberRole === "LEADER");
  const others = members.filter(m => m.memberRole !== "LEADER");

  const Avatar = ({ name }) => {
    const initials = name
      ? name.split(" ").map(w => w[0]).slice(0, 2).join("").toUpperCase()
      : "?";
    const colors = ["#6366f1","#8b5cf6","#ec4899","#f59e0b","#10b981","#3b82f6","#ef4444","#14b8a6"];
    const bg = colors[(initials.charCodeAt(0) || 0) % colors.length];
    return <div className="gml-avatar" style={{ background: bg }}>{initials}</div>;
  };

  return (
    <div className="gml-root">
      {/* Header */}
      <div className="gml-header">
        <div className="gml-title">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          <span>Members</span>
          {!loading && members.length > 0 && (
            <span className="gml-count-chip">{members.length} members</span>
          )}
        </div>
        <button className="gml-refresh-btn" onClick={fetchMembers} disabled={loading}>
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
            className={loading ? "gml-spin" : ""}>
            <path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8"/>
            <path d="M21 3v5h-5"/>
          </svg>
          Refresh
        </button>
      </div>

      {error && (
        <div className="gml-error">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
          {error}
        </div>
      )}

      {loading && (
        <div className="gml-loading"><span className="gml-spinner"/> Loading members...</div>
      )}

      {!loading && members.length === 0 && !error && (
        <div className="gml-empty">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.3">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          <p>No members found.</p>
        </div>
      )}

      {!loading && members.length > 0 && (
        <div className="gml-list">
          {/* Leader first */}
          {leader && (
            <div className="gml-member gml-member-leader">
              <Avatar name={leader.fullName} />
              <div className="gml-info">
                <span className="gml-name">{leader.fullName || leader.username}</span>
                <span className="gml-username">@{leader.username}</span>
                {leader.email && <span className="gml-email">{leader.email}</span>}
              </div>
              <span className="gml-role-badge gml-leader">
                <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                </svg>
                Leader
              </span>
            </div>
          )}

          {/* Divider if both exist */}
          {leader && others.length > 0 && (
            <div className="gml-divider">
              <span>Members ({others.length})</span>
            </div>
          )}

          {/* Other members */}
          {others.map(m => (
            <div key={m.userId} className="gml-member">
              <Avatar name={m.fullName} />
              <div className="gml-info">
                <span className="gml-name">{m.fullName || m.username}</span>
                <span className="gml-username">@{m.username}</span>
                {m.email && <span className="gml-email">{m.email}</span>}
              </div>
              <span className="gml-role-badge gml-member-role">Member</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}