import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./UpdateProfileModal.css";

export default function UpdateProfileModal({ onClose }) {
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: "", githubUsername: "", jiraAccountId: "" });
  const [loading, setLoading] = useState(false);
  const [fetchLoading, setFetchLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const auth = () => ({ Authorization: `Bearer ${localStorage.getItem("token")}` });
  const authJson = () => ({ ...auth(), "Content-Type": "application/json" });

  useEffect(() => {
    // Fetch current profile
    fetch("/api/users/me", { headers: auth() })
      .then(r => r.json())
      .then(data => {
        const u = data.data || {};
        setForm({
          email: u.email || "",
          githubUsername: u.githubUsername || "",
          jiraAccountId: u.jiraAccountId || "",
        });
      })
      .catch(() => {})
      .finally(() => setFetchLoading(false));
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true); setError(""); setSuccess("");
    try {
      const res = await fetch("/api/users/profile", {
        method: "PUT",
        headers: authJson(),
        body: JSON.stringify(form),
      });
      // Safe parse: backend có thể trả plain text khi lỗi
      let data = {};
      const text = await res.text();
      try { data = JSON.parse(text); } catch { /* not JSON */ }
      if (!res.ok) { setError(data.message || `Error ${res.status}: Failed to update profile`); return; }
      setSuccess("Profile updated successfully!");
    } catch { setError("Network error. Please try again."); }
    finally { setLoading(false); }
  };

  const handleLogout = () => {
    localStorage.clear();
    navigate("/login");
  };

  return (
    <div className="upm-overlay" onClick={onClose}>
      <div className="upm-modal" onClick={e => e.stopPropagation()}>
        <div className="upm-header">
          <div>
            <div className="upm-title">Update Profile</div>
            <div className="upm-subtitle">Update your GitHub, Jira and email information</div>
          </div>
          <button className="upm-close" onClick={onClose}>×</button>
        </div>

        <div className="upm-body">
          {fetchLoading ? (
            <div className="upm-loading"><span className="upm-spinner"/> Loading...</div>
          ) : (
            <form onSubmit={handleSubmit}>
              <div className="upm-field">
                <label className="upm-label">Email</label>
                <input className="upm-input" type="email" placeholder="your@email.com"
                  value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} />
              </div>
              <div className="upm-field">
                <label className="upm-label">GitHub Username</label>
                <div className="upm-input-wrap">
                  <span className="upm-prefix">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z"/>
                    </svg>
                  </span>
                  <input className="upm-input upm-input-prefixed" placeholder="e.g. johndoe"
                    value={form.githubUsername} onChange={e => setForm({ ...form, githubUsername: e.target.value })} />
                </div>
                <span className="upm-hint">Used for commit statistics mapping</span>
              </div>
              <div className="upm-field">
                <label className="upm-label">Jira Account ID</label>
                <div className="upm-input-wrap">
                  <span className="upm-prefix">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                      <path d="M11.53 2.034a.9.9 0 0 1 .94 0l8.1 4.95a.9.9 0 0 1 .43.768v9.496a.9.9 0 0 1-.43.768l-8.1 4.95a.9.9 0 0 1-.94 0l-8.1-4.95A.9.9 0 0 1 3 17.248V7.752a.9.9 0 0 1 .43-.768l8.1-4.95z" fill="#0052CC"/>
                      <path d="M12 7.5L8 12l4 4.5 4-4.5L12 7.5z" fill="white"/>
                    </svg>
                  </span>
                  <input className="upm-input upm-input-prefixed" placeholder="e.g. 712020:abc123..."
                    value={form.jiraAccountId} onChange={e => setForm({ ...form, jiraAccountId: e.target.value })} />
                </div>
                <span className="upm-hint">Found in your Jira profile settings</span>
              </div>

              {error && <div className="upm-error">{error}</div>}
              {success && <div className="upm-success">{success}</div>}

              <div className="upm-actions">
                <button type="submit" className="upm-btn-primary" disabled={loading}>
                  {loading ? <><span className="upm-spinner-sm"/> Saving...</> : "Save Changes"}
                </button>
                <button type="button" className="upm-btn-ghost" onClick={onClose}>Cancel</button>
                <button type="button" className="upm-btn-danger" onClick={handleLogout}>
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                    <polyline points="16 17 21 12 16 7" />
                    <line x1="21" y1="12" x2="9" y2="12" />
                  </svg>
                  Logout
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}