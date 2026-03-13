import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import "./JiraConfig.css";
import JiraIssuesPreview from "./JiraIssuesPreview";
import RequirementDashboard from "./RequirementDashboard";

const API_URL = "/api/groups";

function JiraConfig() {
  const { groupId } = useParams();

  const [config, setConfig] = useState(null);
  const [form, setForm] = useState({ baseUrl: "", projectKey: "", jiraEmail: "" });
  const [tokenInput, setTokenInput] = useState("");
  const [showTokenInput, setShowTokenInput] = useState(false);
  const [hasToken, setHasToken] = useState(false);
  const [maskedToken, setMaskedToken] = useState("");

  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [isTesting, setIsTesting] = useState(false);

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [testResult, setTestResult] = useState("");

  const auth = () => ({ Authorization: `Bearer ${localStorage.getItem("token")}` });

  useEffect(() => { if (groupId) fetchConfig(); }, [groupId]);

  const fetchConfig = async () => {
    setIsLoading(true);
    try {
      const res = await fetch(`${API_URL}/${groupId}/jira-config`, { headers: auth() });
      if (res.ok) {
        const data = await res.json();
        setConfig(data);
        setForm({ baseUrl: data.baseUrl || "", projectKey: data.projectKey || "", jiraEmail: data.jiraEmail || "" });
        setHasToken(data.hasToken);
        setMaskedToken(data.tokenMasked || "");
        setIsEditing(false);
      } else if (res.status === 404) {
        setConfig(null);
        setIsEditing(true);
      } else if (res.status === 403) {
        setError("You don't have permission to view this configuration.");
      } else {
        setError("Failed to load Jira configuration.");
      }
    } catch {
      setError("Network error.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(""); setSuccess("");
    if (!form.baseUrl.trim()) { setError("Base URL is required."); return; }
    if (!form.projectKey.trim()) { setError("Project Key is required."); return; }
    if (!form.jiraEmail.trim()) { setError("Jira Email is required."); return; }
    if (!config && !tokenInput.trim()) { setError("API Token is required for new configuration."); return; }

    setIsSaving(true);
    try {
      const res = await fetch(`${API_URL}/${groupId}/jira-config`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...auth() },
        body: JSON.stringify({
          baseUrl: form.baseUrl.trim(),
          projectKey: form.projectKey.trim().toUpperCase(),
          jiraEmail: form.jiraEmail.trim(),
          token: tokenInput.trim() || null,
        }),
      });
      const data = await res.json();
      if (!res.ok) { setError(data.message || "Save failed."); return; }
      setConfig(data);
      setForm({ baseUrl: data.baseUrl, projectKey: data.projectKey, jiraEmail: data.jiraEmail });
      setHasToken(data.hasToken);
      setMaskedToken(data.tokenMasked || "");
      setTokenInput("");
      setShowTokenInput(false);
      setIsEditing(false);
      setSuccess(config ? "Updated successfully! ✓" : "Created successfully! ✓");
      setTimeout(() => setSuccess(""), 3000);
    } catch {
      setError("Network error.");
    } finally {
      setIsSaving(false);
    }
  };

  const handleTest = async () => {
    setTestResult(""); setIsTesting(true);
    try {
      const res = await fetch(`${API_URL}/${groupId}/jira/test-connection`, { method: "POST", headers: auth() });
      if (res.status === 403) { setTestResult("error:You are not authorized to test this integration."); return; }
      if (res.status === 404) { setTestResult("error:Jira configuration not found."); return; }
      const data = await res.json();
      if (!res.ok) { setTestResult(`error:${data.message || "Connection failed."}`); return; }
      setTestResult(`ok:Connected! Project: ${data.key} — ${data.name}`);
    } catch {
      setTestResult("error:Network error while testing connection.");
    } finally {
      setIsTesting(false);
    }
  };

  const handleCancel = () => {
    setIsEditing(false);
    setForm({ baseUrl: config?.baseUrl || "", projectKey: config?.projectKey || "", jiraEmail: config?.jiraEmail || "" });
    setTokenInput(""); setShowTokenInput(false); setError("");
  };

  const testOk = testResult.startsWith("ok:");
  const testMsg = testResult.replace(/^(ok|error):/, "");

  if (isLoading) return (
    <div className="jc-root">
      <div className="jc-loading"><span className="jc-spinner" /><p>Loading Jira configuration...</p></div>
    </div>
  );

  return (
    <div className="jc-root">
      {/* ── Header ── */}
      <div className="jc-header">
        <div className="jc-header-left">
          <div className="jc-logo">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
              <path d="M11.53 2.034a.9.9 0 0 1 .94 0l8.1 4.95a.9.9 0 0 1 .43.768v9.496a.9.9 0 0 1-.43.768l-8.1 4.95a.9.9 0 0 1-.94 0l-8.1-4.95A.9.9 0 0 1 3 17.248V7.752a.9.9 0 0 1 .43-.768l8.1-4.95z" fill="#0052CC" stroke="none"/>
              <path d="M12 7.5L8 12l4 4.5 4-4.5L12 7.5z" fill="white" stroke="none"/>
            </svg>
          </div>
          <div>
            <h2 className="jc-title">Jira Integration</h2>
            <p className="jc-subtitle">
              {config && !isEditing ? "Jira project connected to this group" : "Connect your Jira project to sync issues"}
            </p>
          </div>
        </div>
        {config && !isEditing && (
          <span className={`jc-status-badge ${hasToken ? "jc-connected" : "jc-warning"}`}>
            <span className="jc-status-dot" />
            {hasToken ? "Connected" : "No Token"}
          </span>
        )}
      </div>

      {/* ── Alerts ── */}
      {error && (
        <div className="jc-alert jc-alert-error">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
          <span>{error}</span>
          <button className="jc-alert-close" onClick={() => setError("")}>×</button>
        </div>
      )}
      {success && (
        <div className="jc-alert jc-alert-success">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M20 6 9 17l-5-5"/>
          </svg>
          <span>{success}</span>
        </div>
      )}
      {testResult && (
        <div className={`jc-alert ${testOk ? "jc-alert-success" : "jc-alert-error"}`}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            {testOk
              ? <path d="M20 6 9 17l-5-5"/>
              : <><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></>
            }
          </svg>
          <span>{testMsg}</span>
          <button className="jc-alert-close" onClick={() => setTestResult("")}>×</button>
        </div>
      )}

      {/* ── Read mode ── */}
      {config && !isEditing && (
        <div className="jc-config-display">
          <div className="jc-config-grid">
            <div className="jc-config-item">
              <span className="jc-config-label">Base URL</span>
              <a href={config.baseUrl} target="_blank" rel="noopener noreferrer" className="jc-config-link">
                {config.baseUrl}
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/>
                  <polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/>
                </svg>
              </a>
            </div>
            <div className="jc-config-item">
              <span className="jc-config-label">Project Key</span>
              <span className="jc-config-value jc-key-badge">{config.projectKey}</span>
            </div>
            <div className="jc-config-item">
              <span className="jc-config-label">Email</span>
              <span className="jc-config-value">{config.jiraEmail}</span>
            </div>
            <div className="jc-config-item">
              <span className="jc-config-label">API Token</span>
              {hasToken ? (
                <div className="jc-token-display">
                  <code className="jc-token-mask">{maskedToken}</code>
                  <span className="jc-encrypted-badge">
                    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                      <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                    </svg>
                    Encrypted
                  </span>
                </div>
              ) : (
                <span className="jc-config-value jc-no-token">No token configured</span>
              )}
            </div>
            {config.updatedAt && (
              <div className="jc-config-item">
                <span className="jc-config-label">Last Updated</span>
                <span className="jc-config-value">{new Date(config.updatedAt).toLocaleString()}</span>
              </div>
            )}
          </div>

          <div className="jc-config-actions">
            <button className="jc-btn jc-btn-primary" onClick={() => { setIsEditing(true); setError(""); }}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
              </svg>
              Edit Configuration
            </button>
            <button className="jc-btn jc-btn-secondary" onClick={handleTest} disabled={isTesting || !hasToken}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
                className={isTesting ? "jc-spin" : ""}>
                <path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8"/>
                <path d="M21 3v5h-5"/>
              </svg>
              {isTesting ? "Testing..." : "Test Connection"}
            </button>
          </div>
        </div>
      )}

      {/* ── Edit / Create form ── */}
      {(isEditing || !config) && (
        <form className="jc-form" onSubmit={handleSubmit}>
          <div className="jc-form-grid">
            {/* Base URL */}
            <div className="jc-form-group jc-form-full">
              <label className="jc-form-label">
                Base URL <span className="jc-required">*</span>
              </label>
              <input
                className="jc-input"
                type="url"
                placeholder="https://yourcompany.atlassian.net"
                value={form.baseUrl}
                onChange={(e) => { setForm({ ...form, baseUrl: e.target.value }); setError(""); }}
                disabled={isSaving}
              />
              <p className="jc-input-hint">Your Atlassian domain URL</p>
            </div>

            {/* Project Key */}
            <div className="jc-form-group">
              <label className="jc-form-label">
                Project Key <span className="jc-required">*</span>
              </label>
              <input
                className="jc-input jc-input-mono"
                type="text"
                placeholder="e.g. SWP391"
                value={form.projectKey}
                onChange={(e) => { setForm({ ...form, projectKey: e.target.value.toUpperCase() }); setError(""); }}
                disabled={isSaving}
              />
              <p className="jc-input-hint">Find in Jira project settings</p>
            </div>

            {/* Email */}
            <div className="jc-form-group">
              <label className="jc-form-label">
                Jira Email <span className="jc-required">*</span>
              </label>
              <input
                className="jc-input"
                type="email"
                placeholder="you@company.com"
                value={form.jiraEmail}
                onChange={(e) => { setForm({ ...form, jiraEmail: e.target.value }); setError(""); }}
                disabled={isSaving}
              />
              <p className="jc-input-hint">Atlassian account email</p>
            </div>

            {/* Token */}
            <div className="jc-form-group jc-form-full">
              <label className="jc-form-label">
                API Token
                {!config && <span className="jc-required">*</span>}
                {config && <span className="jc-optional"> (leave empty to keep existing)</span>}
              </label>

              {config && hasToken && !showTokenInput && (
                <div className="jc-token-current">
                  <div className="jc-token-info">
                    <span className="jc-token-label">Current:</span>
                    <code className="jc-token-mask">{maskedToken}</code>
                  </div>
                  <button type="button" className="jc-btn-token-update" onClick={() => setShowTokenInput(true)} disabled={isSaving}>
                    Update Token
                  </button>
                </div>
              )}

              {(showTokenInput || !config) && (
                <div className="jc-token-input-wrap">
                  <input
                    className="jc-input"
                    type="password"
                    placeholder="Paste your Jira API Token"
                    value={tokenInput}
                    onChange={(e) => { setTokenInput(e.target.value); setError(""); }}
                    disabled={isSaving}
                  />
                  <p className="jc-input-hint jc-security-note">
                    <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                      <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                    </svg>
                    Encrypted with AES-256, never displayed in plain text
                  </p>
                  {showTokenInput && config && (
                    <button type="button" className="jc-btn-cancel-token" onClick={() => { setShowTokenInput(false); setTokenInput(""); }}>
                      Cancel Token Update
                    </button>
                  )}
                </div>
              )}

              <details className="jc-token-guide">
                <summary>How to create a Jira API Token?</summary>
                <div className="jc-guide-content">
                  <ol>
                    <li>Go to <a href="https://id.atlassian.com/manage-profile/security/api-tokens" target="_blank" rel="noopener noreferrer">Atlassian Account Security</a></li>
                    <li>Click <strong>Create API token</strong></li>
                    <li>Give it a label (e.g. "SWP391 Integration")</li>
                    <li>Copy the token and paste here</li>
                  </ol>
                </div>
              </details>
            </div>
          </div>

          <div className="jc-form-actions">
            <button type="submit" className="jc-btn jc-btn-primary" disabled={isSaving}>
              {isSaving
                ? <><span className="jc-spinner-sm" /> Saving...</>
                : config ? "Update Configuration" : "Create Configuration"
              }
            </button>
            {isEditing && config && (
              <button type="button" className="jc-btn jc-btn-ghost" onClick={handleCancel} disabled={isSaving}>
                Cancel
              </button>
            )}
          </div>
        </form>
      )}

      {/* ── Security note ── */}
      {config && (
        <div className="jc-security-section">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
          </svg>
          <span>Tokens encrypted with AES-256 · Only last 4 chars shown · Only Leader/Admin can access</span>
        </div>
      )}

      {/* ── Sub-components ── */}
      {config && hasToken && <JiraIssuesPreview groupId={groupId} />}
      {config && hasToken && <RequirementDashboard groupId={groupId} />}
    </div>
  );
}

export default JiraConfig;