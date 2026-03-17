import { useEffect, useState } from "react";
import "./AdminSemesterManagement.css";

const SEMESTER_API = "/api/semesters";

export default function AdminSemesterManagement() {
  const [semesters, setSemesters] = useState([]);
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({ semesterId: null, semesterCode: "", semesterName: "", startDate: "", endDate: "" });
  const [formError, setFormError] = useState("");

  const token = localStorage.getItem("token");
  const auth = () => ({ Authorization: `Bearer ${token}` });
  const authJson = () => ({ ...auth(), "Content-Type": "application/json" });

  useEffect(() => { fetchSemesters(); }, []);

  const fetchSemesters = async () => {
    setLoading(true);
    const res = await fetch(`${SEMESTER_API}?page=0&size=999`, { headers: auth() });
    const data = await res.json();
    setSemesters(data.data?.content || data.data || []);
    setLoading(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError("");
    if (form.startDate && form.endDate && form.startDate > form.endDate) {
      setFormError("End date must be after start date");
      return;
    }

    const method = form.semesterId ? "PUT" : "POST";
    const url = form.semesterId ? `${SEMESTER_API}/${form.semesterId}` : SEMESTER_API;
    const body = {
      semesterCode: form.semesterCode,
      semesterName: form.semesterName,
      startDate: form.startDate || null,
      endDate: form.endDate || null,
    };

    const res = await fetch(url, { method, headers: authJson(), body: JSON.stringify(body) });
    if (!res.ok) {
      const err = await res.json();
      setFormError(err.message || "Error occurred");
      return;
    }
    resetForm();
    fetchSemesters();
  };

  const handleEdit = (s) => {
    setForm({
      semesterId: s.semesterId,
      semesterCode: s.semesterCode,
      semesterName: s.semesterName,
      startDate: s.startDate || "",
      endDate: s.endDate || "",
    });
    setFormError("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const resetForm = () => {
    setForm({ semesterId: null, semesterCode: "", semesterName: "", startDate: "", endDate: "" });
    setFormError("");
  };

  const formatDate = (d) => d ? new Date(d).toLocaleDateString("vi-VN") : "—";

  const getStatus = (s) => {
    if (!s.startDate || !s.endDate) return null;
    const now = new Date(); 
    const start = new Date(s.startDate); 
    const end = new Date(s.endDate);
    if (now < start) return { label: "Upcoming", cls: "asm-status-upcoming" };
    if (now > end) return { label: "Ended", cls: "asm-status-ended" };
    return { label: "Active", cls: "asm-status-active" };
  };

  return (
    <div className="asm-root">
      <div className="asm-page-header">
        <h1 className="asm-page-title">Semester Management</h1>
        <p className="asm-page-desc">Create and manage academic semesters</p>
      </div>

      {/* Form */}
      <div className="asm-card">
        <div className="asm-card-title">{form.semesterId ? "Edit Semester" : "New Semester"}</div>
        <form onSubmit={handleSubmit}>
          <div className="asm-form-row">
            <div className="asm-field">
              <label className="asm-label">Semester Code</label>
              <input
                className="asm-input"
                placeholder="e.g. SP25"
                value={form.semesterCode}
                disabled={!!form.semesterId}
                onChange={e => setForm({ ...form, semesterCode: e.target.value })}
                required
              />
            </div>
            <div className="asm-field asm-field-wide">
              <label className="asm-label">Semester Name</label>
              <input
                className="asm-input"
                placeholder="e.g. Spring 2025"
                value={form.semesterName}
                onChange={e => setForm({ ...form, semesterName: e.target.value })}
                required
              />
            </div>
            <div className="asm-field">
              <label className="asm-label">Start Date</label>
              <input
                className="asm-input"
                type="date"
                value={form.startDate}
                onChange={e => setForm({ ...form, startDate: e.target.value })}
              />
            </div>
            <div className="asm-field">
              <label className="asm-label">End Date</label>
              <input
                className="asm-input"
                type="date"
                value={form.endDate}
                onChange={e => setForm({ ...form, endDate: e.target.value })}
              />
            </div>
          </div>
          {formError && <div className="asm-form-error">{formError}</div>}
          <div className="asm-form-actions">
            <button type="submit" className="asm-btn-primary">
              {form.semesterId ? "Update Semester" : "Create Semester"}
            </button>
            {form.semesterId && (
              <button type="button" className="asm-btn-ghost" onClick={resetForm}>Cancel</button>
            )}
          </div>
        </form>
      </div>

      {/* Table */}
      <div className="asm-card asm-table-card">
        {loading ? (
          <div className="asm-loading"><span className="asm-spinner" /> Loading...</div>
        ) : (
          <table className="asm-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Code</th>
                <th>Name</th>
                <th>Start Date</th>
                <th>End Date</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {semesters.length > 0 ? semesters.map((s, i) => {
                const status = getStatus(s);
                return (
                  <tr key={s.semesterId}>
                    <td className="asm-td-num">{i + 1}</td>
                    <td><span className="asm-code-badge">{s.semesterCode}</span></td>
                    <td className="asm-name-cell">{s.semesterName}</td>
                    <td className="asm-date-cell">{formatDate(s.startDate)}</td>
                    <td className="asm-date-cell">{formatDate(s.endDate)}</td>
                    <td>
                      {status
                        ? <span className={`asm-status ${status.cls}`}>{status.label}</span>
                        : <span className="asm-status asm-status-none">—</span>
                      }
                    </td>
                    <td>
                      <div className="asm-actions">
                        <button className="asm-btn-action asm-btn-edit" onClick={() => handleEdit(s)}>Edit</button>
                      </div>
                    </td>
                  </tr>
                );
              }) : (
                <tr><td colSpan="7" className="asm-empty-row">No semesters found</td></tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}