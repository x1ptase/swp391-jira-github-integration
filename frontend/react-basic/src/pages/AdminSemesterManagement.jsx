import { useEffect, useState } from "react";
import "./AdminSemesterManagement.css";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";

const SEMESTER_API = "/api/semesters";

export default function AdminSemesterManagement() {
  const [semesters, setSemesters] = useState([]);
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({ semesterId: null, semesterCode: "", semesterName: "", startDate: "", endDate: "" });
  const [formError, setFormError] = useState("");

  const token = localStorage.getItem("token");
  const auth = () => ({ Authorization: `Bearer ${token}` });
  const authJson = () => ({ ...auth(), "Content-Type": "application/json" });
  const parseDate = (str) => str ? new Date(str) : null;
  const formatISO = (date) => {
    if (!date) return "";
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, "0");
    const d = String(date.getDate()).padStart(2, "0");
    return `${y}-${m}-${d}`;
  };

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
    const start = parseLocalDate(form.startDate);
    const end = parseLocalDate(form.endDate);

    if (start && end && start > end) {
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

  const handleDelete = async (s) => {
    if (!window.confirm(`Are you sure you want to delete semester "${s.semesterName}"? This action cannot be undone.`)) {
      return;
    }
    const res = await fetch(`${SEMESTER_API}/${s.semesterId}`, { headers: auth(), method: "DELETE" });
    if (!res.ok) {
      const err = await res.json();
      setFormError(err.message || "Error occurred");
      return;
    }
    fetchSemesters();
  };


  const resetForm = () => {
    setForm({ semesterId: null, semesterCode: "", semesterName: "", startDate: "", endDate: "" });
    setFormError("");
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return "—";
    const [y, m, d] = dateStr.split("-");
    return `${d}/${m}/${y}`;
  };
  const parseLocalDate = (dateStr) => {
    if (!dateStr) return null;
    const [y, m, d] = dateStr.split("-");
    return new Date(y, m - 1, d);
  };
  const getStatus = (s) => {
    if (!s.startDate || !s.endDate) return null;

    const now = new Date();
    const start = parseLocalDate(s.startDate);
    const end = parseLocalDate(s.endDate);

    // reset giờ về 00:00 cho chắc
    now.setHours(0, 0, 0, 0);
    start.setHours(0, 0, 0, 0);
    end.setHours(0, 0, 0, 0);

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
              <DatePicker
                className="asm-input"
                selected={parseDate(form.startDate)}
                onChange={date => setForm({ ...form, startDate: formatISO(date) })}
                dateFormat="dd/MM/yyyy"
                maxDate={parseDate(form.endDate)}
                placeholderText="dd/mm/yyyy"
                isClearable
              />
            </div>
            <div className="asm-field">
              <label className="asm-label">End Date</label>
              <DatePicker
                className="asm-input"
                selected={parseDate(form.endDate)}
                onChange={date => setForm({ ...form, endDate: formatISO(date) })}
                dateFormat="dd/MM/yyyy"
                minDate={parseDate(form.startDate)}
                placeholderText="dd/mm/yyyy"
                isClearable
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
                        <button className="asm-btn-action asm-btn-delete" onClick={() => handleDelete(s)}>Delete</button>
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