import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import "./JiraConfig.css";

const API_URL = "/api/groups";

function JiraConfig() {
    const { groupId } = useParams();

    const [config, setConfig] = useState(null);
    const [form, setForm] = useState({
        baseUrl: "",
        projectKey: "",
        jiraEmail: "",
    });

    const [tokenInput, setTokenInput] = useState("");
    const [hasToken, setHasToken] = useState(false);
    const [maskedToken, setMaskedToken] = useState("");

    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [isEditing, setIsEditing] = useState(false);

    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [testResult, setTestResult] = useState("");
    const [isTesting, setIsTesting] = useState(false);

    useEffect(() => {
        fetchConfig();
    }, [groupId]);


    const handleTest = async () => {
        setTestResult("");
        setError("");

        try {
            setIsTesting(true);

            const res = await fetch(
                `${API_URL}/${groupId}/jira-config/test`,
                {
                    headers: {
                        Authorization: `Bearer ${localStorage.getItem("token")}`,
                    },
                }
            );

            if (res.status === 403) {
                setTestResult("❌ You are not authorized to test this integration.");
                return;
            }

            if (res.status === 404) {
                setTestResult("❌ Jira configuration not found.");
                return;
            }

            const data = await res.json();

            if (!res.ok) {
                setTestResult("❌ " + (data.message || "Connection failed"));
                return;
            }

            // BE nên trả projectName / projectKey
            setTestResult(
                ` Connected successfully! Project: ${data.projectKey} - ${data.projectName}`
            );

        } catch {
            setTestResult("❌ Network error while testing connection.");
        } finally {
            setIsTesting(false);
        }
    };

    const fetchConfig = async () => {
        try {
            const res = await fetch(`${API_URL}/${groupId}/jira-config`, {
                headers: {
                    Authorization: `Bearer ${localStorage.getItem("token")}`,
                },
            });

            if (res.ok) {
                const data = await res.json();

                setConfig(data);    
                setForm({
                    baseUrl: data.baseUrl || "",
                    projectKey: data.projectKey || "",
                    jiraEmail: data.jiraEmail || "",
                });

                setHasToken(data.hasToken);
                setMaskedToken(data.tokenMasked);
                setIsEditing(false);
            } else if (res.status === 404) {
                setConfig(null);
                setIsEditing(true);
            } else {
                setError("Failed to load Jira configuration");
            }
        } catch {
            setError("Network error");
        } finally {
            setIsLoading(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");
        setSuccess("");

        const payload = {
            baseUrl: form.baseUrl.trim(),
            projectKey: form.projectKey.trim(),
            jiraEmail: form.jiraEmail.trim(),
            token: tokenInput.trim() || null,
        };

        try {
            setIsSaving(true);

            const res = await fetch(`${API_URL}/${groupId}/jira-config`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${localStorage.getItem("token")}`,
                },
                body: JSON.stringify(payload),
            });

            const data = await res.json();

            if (!res.ok) {
                setError(data.message || "Save failed");
                return;
            }
                
            const cfg = data;

            setConfig(cfg);
            setForm({
                baseUrl: cfg.baseUrl,
                projectKey: cfg.projectKey,
                jiraEmail: cfg.jiraEmail,
            });

            setHasToken(cfg.hasToken);
            setMaskedToken(cfg.tokenMasked);

            setTokenInput("");
            setIsEditing(false);

            setSuccess("Saved successfully ✓");
        } catch {
            setError("Network error");
        } finally {
            setIsSaving(false);
        }
    };

    if (isLoading) return <p>Loading Jira config...</p>;

    return (
        <div className="jira-container">
            <h2>Jira Integration</h2>

            {error && <div className="error">{error}</div>}
            {success && <div className="success">{success}</div>}
            {testResult && <div className="test-result">{testResult}</div>}

            {config && !isEditing ? (
                <div className="config-view">
                    <p><b>Base URL:</b> {config.baseUrl}</p>
                    <p><b>Project Key:</b> {config.projectKey}</p>
                    <p><b>Email:</b> {config.jiraEmail}</p>
                    <p>
                        <b>Token:</b>{" "}
                        {hasToken ? maskedToken : "No token configured"}
                    </p>

                    <div className="button-group">
                        <button onClick={() => setIsEditing(true)}>
                            Edit Configuration
                        </button>

                        <button
                            className="test-btn"
                            onClick={handleTest}
                            disabled={isTesting}
                        >
                            {isTesting ? "Testing..." : "Test Connection"}
                        </button>
                    </div>
                </div>
            ) : (
                <form onSubmit={handleSubmit} className="config-form">
                    <input
                        type="text"
                        placeholder="https://yourcompany.atlassian.net"
                        value={form.baseUrl}
                        onChange={(e) =>
                            setForm({ ...form, baseUrl: e.target.value })
                        }
                    />

                    <input
                        type="text"
                        placeholder="Project Key (e.g. SWP)"
                        value={form.projectKey}
                        onChange={(e) =>
                            setForm({ ...form, projectKey: e.target.value })
                        }
                    />

                    <input
                        type="email"
                        placeholder="jira email"
                        value={form.jiraEmail}
                        onChange={(e) =>
                            setForm({ ...form, jiraEmail: e.target.value })
                        }
                    />

                    <input
                        type="password"
                        placeholder="Jira API Token"
                        value={tokenInput}
                        onChange={(e) => setTokenInput(e.target.value)}
                    />

                    <button type="submit" disabled={isSaving}>
                        {isSaving ? "Saving..." : config ? "Update" : "Create"}
                    </button>
                </form>
            )}
        </div>
    );
}

export default JiraConfig;