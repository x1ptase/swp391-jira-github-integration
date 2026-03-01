import { useEffect, useState } from "react";
import "./GitHubConfig.css";
import { useParams } from "react-router-dom";

const API_URL = "/api/groups";

function GitHubConfig({ onSuccess }) {
  const [config, setConfig] = useState(null);
  const [form, setForm] = useState({ repoFullName: "", token: "" });
  const [maskedToken, setMaskedToken] = useState("");
  const [hasToken, setHasToken] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [showTokenInput, setShowTokenInput] = useState(false);
  const [tokenInputValue, setTokenInputValue] = useState("");
  const [isEditing, setIsEditing] = useState(false);
  const [testResult, setTestResult] = useState("");
  const [isTesting, setIsTesting] = useState(false);
  const { groupId } = useParams();
  useEffect(() => {
    if (groupId) {
      fetchConfig();
    }
  }, [groupId]);

  const fetchConfig = async () => {
    try {
      setIsLoading(true);
      const res = await fetch(`${API_URL}/${groupId}/github-config`, {
        headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
      });

      if (res.ok) {
        const data = await res.json();
        const cfg = data;
        setConfig(cfg);
        setForm({ repoFullName: cfg.repoFullName || "", token: "" });
        setMaskedToken(cfg.tokenMasked || "");
        setHasToken(cfg.hasToken || false);
        setShowTokenInput(false);
        setIsEditing(false);
      } else if (res.status === 404) {
        setConfig(null);
        setForm({ repoFullName: "", token: "" });
        setTokenInputValue("");
        setShowTokenInput(true);
        setIsEditing(true);
        setHasToken(false);
      } else if (res.status === 403) {
        setError("You don't have permission to view this configuration");
      } else {
        setError("Failed to fetch configuration");
      }
    } catch (err) {
      console.error("Fetch error:", err);
      setError("Network error. Please try again.");
    } finally {
      setIsLoading(false);
    }
  };


  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");

    if (!validateForm()) return;

    try {
      setIsSaving(true);
      const payload = {
        repoFullName: form.repoFullName.trim(),
        token: tokenInputValue.trim() || null,
      };

      const res = await fetch(`${API_URL}/${groupId}/github-config`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
        body: JSON.stringify(payload),
      });

      const data = await res.json();

      if (!res.ok) {
        if (res.status === 403) {
          setError("You don't have permission to update this configuration");
        } else if (res.status === 400) {
          setError(data.message || "Invalid input. Please check your entries.");
        } else {
          setError(data.message || "Failed to save configuration");
        }
        return;
      }

      const cfg = data;
      const isUpdate = !!config;

      setConfig(cfg);
      setForm({ repoFullName: cfg.repoFullName || "", token: "" });
      setMaskedToken(cfg.tokenMasked || "");
      setHasToken(cfg.hasToken || false);
      setTokenInputValue("");
      setShowTokenInput(false);
      setIsEditing(false);

      const msg = isUpdate ? "Updated successfully! ✓" : "Created successfully! ✓";
      setSuccess(msg);
      setTimeout(() => setSuccess(""), 3000);

      if (onSuccess) onSuccess(cfg);
    } catch (err) {
      console.error("Submit error:", err);
      setError("Network error. Please try again.");
    } finally {
      setIsSaving(false);
    }
  };

  const validateForm = () => {
    if (!form.repoFullName.trim()) {
      setError("Repository name is required");
      return false;
    }

    if (!form.repoFullName.match(/^[a-zA-Z0-9-._]+\/[a-zA-Z0-9-._]+$/)) {
      setError("Invalid format. Expected: owner/repo (e.g., facebook/react)");
      return false;
    }

    if (!config && !tokenInputValue.trim()) {
      setError("Token is required for new configuration");
      return false;
    }

    if (config && showTokenInput && !tokenInputValue.trim()) {
      setError("Please enter a new token or cancel");
      return false;
    }

    return true;
  };

  const handleRepoChange = (e) => {
    setForm({ ...form, repoFullName: e.target.value });
    setError("");
  };

  const handleTokenChange = (e) => {
    setTokenInputValue(e.target.value);
    setError("");
  };

  const toggleTokenInput = () => {
    if (showTokenInput) setTokenInputValue("");
    setShowTokenInput(!showTokenInput);
  };

  const handleEdit = () => {
    setIsEditing(true);
    setError("");
    setShowTokenInput(false);
  };

  const handleCancel = () => {
    setIsEditing(false);
    setForm({ repoFullName: config?.repoFullName || "", token: "" });
    setTokenInputValue("");
    setShowTokenInput(false);
    setError("");
  };
  const handleTestConnection = async () => {
    setTestResult("");
    try {
      setIsTesting(true);

      const res = await fetch(
        `${API_URL}/${groupId}/github/test-connection`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${localStorage.getItem("token")}`,
          },
        }
      );

      if (res.status === 404) {
        setTestResult(" GitHub configuration not found.");
        return;
      }

      if (res.status === 403) {
        setTestResult(" You are not authorized.");
        return;
      }

      const data = await res.json();

      if (!res.ok) {
        setTestResult((data.message || "Connection failed"));
        return;
      }

      setTestResult(
        `Connected! Repo: ${data.fullName} ⭐ ${data.stars} stars`
      );
    } catch (err) {
      setTestResult("Network error.");
    } finally {
      setIsTesting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="github-config-container">
        <div className="loading-state">
          <div className="spinner"></div>
          <p>Loading GitHub configuration...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="github-config-container">
      {/* HEADER */}
      <div className="config-header">
        <div className="header-left">
          <h2>GitHub Integration</h2>
          <p className="header-desc">
            {config && !isEditing
              ? "Your GitHub repository configuration"
              : "Connect your group to a GitHub repository"}
          </p>
        </div>
        {config && !isEditing && (
          <span className={`status ${hasToken ? "connected" : "warning"}`}>
            {hasToken ? "Connected" : "No Token"}
          </span>
        )}
      </div>

      {/* ALERTS */}
      {error && (
        <div className="alert alert-error">
          <span className="alert-icon">!!</span>
          <span className="alert-text">{error}</span>
          <button className="alert-close" onClick={() => setError("")}>×</button>
        </div>
      )}

      {success && (
        <div className="alert alert-success">
          <span className="alert-icon">✓</span>
          <span className="alert-text">{success}</span>
        </div>
      )}

      {/* CONFIG DISPLAY (READ MODE) */}
      {config && !isEditing && (
        <div className="config-display">

          <div className="config-item">
            <span className="config-label">Repository</span>
            <div className="config-value-wrapper">
              <a
                href={`https://github.com/${config.repoFullName}`}
                target="_blank"
                rel="noopener noreferrer"
                className="repo-link"
              >
                <span>🔗</span>
                {config.repoFullName}
              </a>
            </div>
          </div>

          <div className="config-item">
            <span className="config-label">GitHub Token</span>
            <div className="token-display-wrapper">
              {hasToken && maskedToken ? (
                <div className="token-display">
                  <span className="token-mask">{maskedToken}</span>
                  <span className="token-status">Encrypted & Secure</span>
                </div>
              ) : (
                <div className="token-missing">
                  <span>No token configured</span>
                </div>
              )}
            </div>
          </div>

          <div className="config-item">
            <span className="config-label">Last Updated</span>
            <span className="config-value">
              {new Date(config.updatedAt).toLocaleString()}
            </span>
          </div>

          {/* ACTION BUTTONS */}
          <div className="config-actions">
            <button
              className="btn btn-primary"
              onClick={handleEdit}
              disabled={isSaving}
            >
              ✎ Edit Configuration
            </button>

            <button
              className="btn btn-secondary"
              onClick={handleTestConnection}
              disabled={isTesting}
            >
              {isTesting ? "Testing..." : "🔍 Test Connection"}
            </button>
          </div>

          {/* TEST RESULT */}
          {testResult && (
            <div className="test-result">
              {testResult}
            </div>
          )}

        </div>
      )}
      {/* EDIT FORM */}
      {(isEditing || !config) && (
        <form className="config-form" onSubmit={handleSubmit}>
          {/* REPO INPUT */}
          <div className="form-group">
            <label className="form-label">
              Repository Name <span className="required">*</span>
            </label>
            <div className="input-group">
              <span className="input-addon">github.com/</span>
              <input
                type="text"
                placeholder="owner/repo"
                value={form.repoFullName}
                onChange={handleRepoChange}
                disabled={isSaving}
                className="input-field"
              />
            </div>
            <p className="input-help">Format: owner/repository (e.g., facebook/react)</p>
          </div>

          {/* TOKEN INPUT */}
          <div className="form-group">
            <label className="form-label">
              GitHub Personal Access Token
              {!config && <span className="required">*</span>}
              {config && <span className="optional">(leave empty to keep existing)</span>}
            </label>

            {config && hasToken && !showTokenInput && (
              <div className="token-current">
                <div className="token-info">
                  <span className="token-label">Current:</span>
                  <code className="token-code">{maskedToken}</code>
                </div>
                <button
                  type="button"
                  className="btn-token-update"
                  onClick={toggleTokenInput}
                  disabled={isSaving}
                >
                  🔄 Update
                </button>
              </div>
            )}

            {(showTokenInput || !config) && (
              <div className="token-input-wrapper">
                <input
                  type="password"
                  placeholder="Paste your GitHub Personal Access Token"
                  value={tokenInputValue}
                  onChange={handleTokenChange}
                  disabled={isSaving}
                  className="input-field token-input"
                />
                <p className="input-help security-note">
                  Encrypted AES-256 and never displayed in plain text
                </p>

                {showTokenInput && config && (
                  <button
                    type="button"
                    className="btn-cancel-token"
                    onClick={toggleTokenInput}
                    disabled={isSaving}
                  >
                    Cancel Update
                  </button>
                )}
              </div>
            )}

            {/* TOKEN GUIDE */}
            <details className="token-guide">
              <summary>How to create a GitHub token?</summary>
              <div className="guide-content">
                <ol>
                  <li>GitHub Settings → Developer settings</li>
                  <li>Personal access tokens → Tokens (classic)</li>
                  <li>Generate new token (classic)</li>
                  <li>Select scopes: <code>repo</code>, <code>admin:repo_hook</code></li>
                  <li>Copy and paste here</li>
                </ol>
              </div>
            </details>
          </div>

          {/* ACTIONS */}
          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={isSaving}>
              {isSaving ? (
                <>
                  <span className="spinner-small"></span>
                  Saving...
                </>
              ) : config ? (
                "Update"
              ) : (
                "Create"
              )}
            </button>

            {isEditing && config && (
              <button
                type="button"
                className="btn btn-secondary"
                onClick={handleCancel}
                disabled={isSaving}
              >
                Cancel
              </button>
            )}
          </div>
        </form>
      )}

      {/* SECURITY INFO */}
      {config && (
        <div className="security-section">
          <h3>Security</h3>
          <ul>
            <li>Tokens encrypted with AES-256 before storage</li>
            <li>Only last 4 characters displayed (masked)</li>
            <li>Tokens never logged or exposed</li>
            <li>Only group leaders/admins can access</li>
            <li>Update repository without changing token</li>
          </ul>
        </div>
      )}
    </div>
  );
}

export default GitHubConfig;