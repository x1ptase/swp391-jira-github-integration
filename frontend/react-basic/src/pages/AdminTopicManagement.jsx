import { useEffect, useState } from "react";
import "./AdminTopicManagement.css";

const TOPIC_API = "/api/topics";

export default function AdminTopicManagement() {
  const [topics, setTopics] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [keyword, setKeyword] = useState("");

  const [form, setForm] = useState({ topicId: null, topicCode: "", topicName: "", description: "" });
  const [formError, setFormError] = useState("");

  // Detail modal
  const [showDetail, setShowDetail] = useState(false);
  const [detailTopic, setDetailTopic] = useState(null);

  const token = localStorage.getItem("token");
  const auth = () => ({ Authorization: `Bearer ${token}` });
  const authJson = () => ({ ...auth(), "Content-Type": "application/json" });

  useEffect(() => { fetchTopics(0); }, []);

  const fetchTopics = async (p = 0, kw = keyword) => {
    setLoading(true);
    const params = new URLSearchParams({ page: p, size: 10 });
    if (kw.trim()) params.set("keyword", kw.trim());
    const res = await fetch(`${TOPIC_API}?${params}`, { headers: auth() });
    const data = await res.json();
    setTopics(data.data?.content || []);
    setTotalPages(data.data?.totalPages || 0);
    setPage(p);
    setLoading(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError("");
    const method = form.topicId ? "PUT" : "POST";
    const url = form.topicId ? `${TOPIC_API}/${form.topicId}` : TOPIC_API;
    const body = form.topicId
      ? { topicName: form.topicName, description: form.description }
      : { topicCode: form.topicCode, topicName: form.topicName, description: form.description };

    const res = await fetch(url, { method, headers: authJson(), body: JSON.stringify(body) });
    if (!res.ok) {
      const err = await res.json();
      setFormError(err.message || "Error occurred");
      return;
    }
    resetForm();
    fetchTopics(page);
  };

  const handleEdit = (t) => {
    setForm({ topicId: t.topicId, topicCode: t.topicCode, topicName: t.topicName, description: t.description || "" });
    setFormError("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Delete this topic?")) return;
    const res = await fetch(`${TOPIC_API}/${id}`, { method: "DELETE", headers: auth() });
    if (!res.ok) { const err = await res.json(); alert(err.message || "Failed to delete"); return; }
    fetchTopics(page);
  };

  const handleDetail = async (id) => {
    const res = await fetch(`${TOPIC_API}/${id}`, { headers: auth() });
    const data = await res.json();
    setDetailTopic(data.data);
    setShowDetail(true);
  };

  const resetForm = () => {
    setForm({ topicId: null, topicCode: "", topicName: "", description: "" });
    setFormError("");
  };

  const formatDate = (iso) => {
    if (!iso) return "—";
    return new Date(iso).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
  };

  return (
    <div className="atm-root">
      <div className="atm-page-header">
        <h1 className="atm-page-title">Topic Management</h1>
        <p className="atm-page-desc">Create and manage project topics for students</p>
      </div>

      {/* Form */}
      <div className="atm-card">
        <div className="atm-card-title">{form.topicId ? "✏️ Edit Topic" : "➕ New Topic"}</div>
        <form onSubmit={handleSubmit}>
          <div className="atm-form-row">
            <div className="atm-field">
              <label className="atm-label">Topic Code</label>
              <input
                className="atm-input"
                placeholder="e.g. TOPIC001"
                value={form.topicCode}
                disabled={!!form.topicId}
                onChange={e => setForm({ ...form, topicCode: e.target.value })}
                required
              />
            </div>
            <div className="atm-field atm-field-wide">
              <label className="atm-label">Topic Name</label>
              <input
                className="atm-input"
                placeholder="e.g. E-commerce Web Application"
                value={form.topicName}
                onChange={e => setForm({ ...form, topicName: e.target.value })}
                required
              />
            </div>
          </div>
          <div className="atm-field atm-field-full" style={{ marginTop: 10 }}>
            <label className="atm-label">Description</label>
            <textarea
              className="atm-textarea"
              placeholder="Describe the topic..."
              value={form.description}
              onChange={e => setForm({ ...form, description: e.target.value })}
              rows={3}
            />
          </div>
          {formError && <div className="atm-form-error">{formError}</div>}
          <div className="atm-form-actions">
            <button type="submit" className="atm-btn-primary">
              {form.topicId ? "Update Topic" : "Create Topic"}
            </button>
            {form.topicId && (
              <button type="button" className="atm-btn-ghost" onClick={resetForm}>Cancel</button>
            )}
          </div>
        </form>
      </div>

      {/* Filter */}
      <div className="atm-filter-bar">
        <div className="atm-search-wrap">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
          </svg>
          <input
            className="atm-search-input"
            placeholder="Search by code or name..."
            value={keyword}
            onChange={e => setKeyword(e.target.value)}
            onKeyDown={e => e.key === "Enter" && fetchTopics(0, keyword)}
          />
          {keyword && <button className="atm-clear-btn" onClick={() => { setKeyword(""); fetchTopics(0, ""); }}>×</button>}
        </div>
        <button className="atm-btn-primary atm-btn-sm" onClick={() => fetchTopics(0, keyword)}>Search</button>
        <button className="atm-btn-ghost atm-btn-sm" onClick={() => { setKeyword(""); fetchTopics(0, ""); }}>Clear</button>
      </div>

      {/* Table */}
      <div className="atm-card atm-table-card">
        {loading ? (
          <div className="atm-loading"><span className="atm-spinner"/> Loading...</div>
        ) : (
          <>
            <table className="atm-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Code</th>
                  <th>Name</th>
                  <th>Description</th>
                  <th>Created</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {topics.length > 0 ? topics.map((t, i) => (
                  <tr key={t.topicId}>
                    <td className="atm-td-num">{page * 10 + i + 1}</td>
                    <td><span className="atm-code-badge">{t.topicCode}</span></td>
                    <td className="atm-name-cell" title={t.topicName}>{t.topicName}</td>
                    <td className="atm-desc-cell">
                      {t.description
                        ? <span title={t.description}>{t.description.length > 60 ? t.description.slice(0, 60) + "..." : t.description}</span>
                        : <span className="atm-null">—</span>}
                    </td>
                    <td className="atm-date-cell">{formatDate(t.createdAt)}</td>
                    <td>
                      <div className="atm-actions">
                        <button className="atm-btn-action atm-btn-detail" onClick={() => handleDetail(t.topicId)}>Detail</button>
                        <button className="atm-btn-action atm-btn-edit" onClick={() => handleEdit(t)}>Edit</button>
                        <button className="atm-btn-action atm-btn-danger" onClick={() => handleDelete(t.topicId)}>Delete</button>
                      </div>
                    </td>
                  </tr>
                )) : (
                  <tr><td colSpan="6" className="atm-empty-row">No topics found</td></tr>
                )}
              </tbody>
            </table>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="atm-pagination">
                <button className="atm-page-btn" onClick={() => fetchTopics(page - 1)} disabled={page === 0 || loading}>← Prev</button>
                <div className="atm-page-numbers">
                  {Array.from({ length: totalPages }, (_, i) => (
                    <button key={i} className={`atm-page-num ${i === page ? "active" : ""}`}
                      onClick={() => fetchTopics(i)} disabled={loading}>
                      {i + 1}
                    </button>
                  ))}
                </div>
                <button className="atm-page-btn" onClick={() => fetchTopics(page + 1)} disabled={page >= totalPages - 1 || loading}>Next →</button>
              </div>
            )}
          </>
        )}
      </div>

      {/* Detail Modal */}
      {showDetail && detailTopic && (
        <div className="atm-modal-overlay" onClick={() => setShowDetail(false)}>
          <div className="atm-modal" onClick={e => e.stopPropagation()}>
            <div className="atm-modal-header">
              <div>
                <div className="atm-modal-title">{detailTopic.topicName}</div>
                <div className="atm-modal-subtitle"><span className="atm-code-badge">{detailTopic.topicCode}</span></div>
              </div>
              <button className="atm-modal-close" onClick={() => setShowDetail(false)}>×</button>
            </div>
            <div className="atm-modal-body">
              <div className="atm-detail-section">
                <span className="atm-detail-label">Description</span>
                <p className="atm-detail-desc">{detailTopic.description || "No description provided."}</p>
              </div>
              <div className="atm-detail-meta">
                <div className="atm-detail-meta-row">
                  <span className="atm-detail-label">Created</span>
                  <span>{formatDate(detailTopic.createdAt)}</span>
                </div>
                <div className="atm-detail-meta-row">
                  <span className="atm-detail-label">Updated</span>
                  <span>{formatDate(detailTopic.updatedAt)}</span>
                </div>
              </div>
              <div className="atm-detail-actions">
                <button className="atm-btn-primary" onClick={() => { setShowDetail(false); handleEdit(detailTopic); }}>Edit</button>
                <button className="atm-btn-ghost" onClick={() => setShowDetail(false)}>Close</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}