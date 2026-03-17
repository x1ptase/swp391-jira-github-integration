import { useEffect, useState } from "react";
import "./AdminClassManagement.css";

const CLASS_API = "/api/classes";
const SEMESTER_API = "/api/semesters?page=0&size=999";
const LECTURER_API = "/api/admin/users?roleCode=LECTURER&page=0&size=999";
const ASSIGN_LECTURER_API = "/api/admin/classes";

const COURSE_ID = 1;
const COURSE_CODE = "SWP391";

export default function AdminClassManagement() {
  const [classes, setClasses] = useState([]);
  const [semesters, setSemesters] = useState([]);
  const [lecturers, setLecturers] = useState([]);
  const [loading, setLoading] = useState(false);

  const [filter, setFilter] = useState({ keyword: "", semesterCode: "" });

  const [form, setForm] = useState({ classId: null, classCode: "", semesterId: "" });
  const [formError, setFormError] = useState("");

  const [showLecturerModal, setShowLecturerModal] = useState(false);
  const [selectedClass, setSelectedClass] = useState(null);

  const token = localStorage.getItem("token");
  const auth = () => ({ Authorization: `Bearer ${token}` });
  const authJson = () => ({ ...auth(), "Content-Type": "application/json" });

  useEffect(() => {
    fetchClasses();
    fetchSemesters();
    fetchLecturers();
  }, []);

  const fetchClasses = async () => {
    setLoading(true);
    const params = new URLSearchParams();
    if (filter.keyword) params.set("keyword", filter.keyword);
    if (filter.semesterCode) params.set("semesterCode", filter.semesterCode);
    params.set("page", "0"); params.set("size", "999");
    const res = await fetch(`${CLASS_API}?${params}`, { headers: auth() });
    const data = await res.json();
    setClasses(data.data?.content || data.data || []);
    setLoading(false);
  };

  const fetchSemesters = async () => {
    const res = await fetch(SEMESTER_API, { headers: auth() });
    const data = await res.json();
    setSemesters(data.data?.content || data.data || []);
  };

  const fetchLecturers = async () => {
    const res = await fetch(LECTURER_API, { headers: auth() });
    const data = await res.json();
    setLecturers(data.data?.content || data.data || []);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError("");
    if (!form.semesterId) { setFormError("Please select a semester"); return; }

    const method = form.classId ? "PUT" : "POST";
    const url = form.classId ? `${CLASS_API}/${form.classId}` : CLASS_API;
    const body = {
      classCode: form.classCode,
      course: { courseId: COURSE_ID },
      semester: { semesterId: Number(form.semesterId) },
    };

    const res = await fetch(url, { method, headers: authJson(), body: JSON.stringify(body) });
    if (!res.ok) {
      const err = await res.json();
      setFormError(err.message || "Error occurred");
      return;
    }
    resetForm();
    fetchClasses();
  };

  const handleEdit = (c) => {
    setForm({ classId: c.classId, classCode: c.classCode, semesterId: c.semesterId || "" });
    setFormError("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Delete this class?")) return;
    await fetch(`${CLASS_API}/${id}`, { method: "DELETE", headers: auth() });
    fetchClasses();
  };

  const resetForm = () => {
    setForm({ classId: null, classCode: "", semesterId: "" });
    setFormError("");
  };

  const openLecturerModal = (c) => {
    setSelectedClass(c);
    setShowLecturerModal(true);
  };

  const handleAssignLecturer = async (lecturerId) => {
    if (!selectedClass) return;
    const res = await fetch(`${ASSIGN_LECTURER_API}/${selectedClass.classId}/lecturer`, {
      method: "PUT",
      headers: authJson(),
      body: JSON.stringify({ lecturerId }),
    });
    if (res.ok) {
      setShowLecturerModal(false);
      fetchClasses();
    } else {
      const err = await res.json();
      alert("Failed: " + (err.message || "Unknown error"));
    }
  };

  return (
    <div className="acm-root">
      <div className="acm-page-header">
        <div>
          <h1 className="acm-page-title">Class Management</h1>
          <p className="acm-page-desc">Create and manage academic classes for {COURSE_CODE}</p>
        </div>
      </div>

      {/* Form */}
      <div className="acm-card">
        <div className="acm-card-title">
          {form.classId ? "Edit Class" : "New Class"}
        </div>
        <form className="acm-form" onSubmit={handleSubmit}>
          <div className="acm-form-row">
            <div className="acm-field">
              <label className="acm-label">Class Code</label>
              <input
                className="acm-input"
                placeholder="e.g. SE1921"
                value={form.classCode}
                onChange={e => setForm({ ...form, classCode: e.target.value })}
                required
              />
            </div>
            <div className="acm-field">
              <label className="acm-label">Course</label>
              <input className="acm-input acm-disabled" value={COURSE_CODE} disabled />
            </div>
            <div className="acm-field">
              <label className="acm-label">Semester</label>
              <select
                className="acm-select"
                value={form.semesterId}
                onChange={e => setForm({ ...form, semesterId: e.target.value })}
                required
              >
                <option value="">Select semester...</option>
                {semesters.map(s => (
                  <option key={s.semesterId} value={s.semesterId}>
                    {s.semesterCode} — {s.semesterName}
                  </option>
                ))}
              </select>
            </div>
          </div>
          {formError && <div className="acm-form-error">{formError}</div>}
          <div className="acm-form-actions">
            <button type="submit" className="acm-btn-primary">
              {form.classId ? "Update Class" : "Create Class"}
            </button>
            {form.classId && (
              <button type="button" className="acm-btn-ghost" onClick={resetForm}>Cancel</button>
            )}
          </div>
        </form>
      </div>

      {/* Filter */}
      <div className="acm-filter-bar">
        <div className="acm-search-wrap">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
          </svg>
          <input
            className="acm-search-input"
            placeholder="Search class code..."
            value={filter.keyword}
            onChange={e => setFilter({ ...filter, keyword: e.target.value })}
            onKeyDown={e => e.key === "Enter" && fetchClasses()}
          />
        </div>
        <select
          className="acm-select acm-select-sm"
          value={filter.semesterCode}
          onChange={e => setFilter({ ...filter, semesterCode: e.target.value })}
        >
          <option value="">All Semesters</option>
          {semesters.map(s => (
            <option key={s.semesterId} value={s.semesterCode}>{s.semesterCode}</option>
          ))}
        </select>
        <button className="acm-btn-primary acm-btn-sm" onClick={fetchClasses}>Search</button>
        <button className="acm-btn-ghost acm-btn-sm" onClick={() => { setFilter({ keyword: "", semesterCode: "" }); setTimeout(fetchClasses, 0); }}>Clear</button>
      </div>

      {/* Table */}
      <div className="acm-card acm-table-card">
        {loading ? (
          <div className="acm-loading"><span className="acm-spinner" /> Loading...</div>
        ) : (
          <table className="acm-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Class Code</th>
                <th>Course</th>
                <th>Semester</th>
                <th>Assigned Lecturer</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {classes.length > 0 ? classes.map((c, i) => (
                <tr key={c.classId}>
                  <td className="acm-td-num">{i + 1}</td>
                  <td><strong>{c.classCode}</strong></td>
                  <td><span className="acm-course-badge">{c.courseCode}</span></td>
                  <td>
                    <span className="acm-semester-badge">{c.semesterCode}</span>
                    <span className="acm-semester-name">{c.semesterName}</span>
                  </td>
                  <td>
                    {c.lecturerName
                      ? <span className="acm-lecturer-badge">{c.lecturerName}</span>
                      : <span className="acm-no-lecturer">Not assigned</span>
                    }
                  </td>
                  <td>
                    <div className="acm-actions">
                      <button className="acm-btn-action acm-btn-edit" onClick={() => handleEdit(c)}>Edit</button>
                      <button className="acm-btn-action acm-btn-lecturer" onClick={() => openLecturerModal(c)}>
                        {c.lecturerName ? "Change Lecturer" : "Assign Lecturer"}
                      </button>
                      <button className="acm-btn-action acm-btn-danger" onClick={() => handleDelete(c.classId)}>Delete</button>
                    </div>
                  </td>
                </tr>
              )) : (
                <tr>
                  <td colSpan="6" className="acm-empty-row">No classes found</td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>

      {/* Lecturer Modal */}
      {showLecturerModal && (
        <div className="acm-modal-overlay" onClick={() => setShowLecturerModal(false)}>
          <div className="acm-modal" onClick={e => e.stopPropagation()}>
            <div className="acm-modal-header">
              <div>
                <div className="acm-modal-title">Assign Lecturer</div>
                <div className="acm-modal-subtitle">{selectedClass?.classCode}</div>
              </div>
              <button className="acm-modal-close" onClick={() => setShowLecturerModal(false)}>×</button>
            </div>
            <div className="acm-modal-body">
              <table className="acm-table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Full Name</th>
                    <th>Username</th>
                    <th>Email</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {lecturers.length > 0 ? lecturers.map((l, i) => (
                    <tr key={l.userId}>
                      <td className="acm-td-num">{i + 1}</td>
                      <td>{l.fullName}</td>
                      <td><span className="acm-username">{l.username}</span></td>
                      <td>{l.email}</td>
                      <td>
                        <button
                          className="acm-btn-action acm-btn-lecturer"
                          onClick={() => handleAssignLecturer(l.userId)}
                        >
                          Assign
                        </button>
                      </td>
                    </tr>
                  )) : (
                    <tr><td colSpan="5" className="acm-empty-row">No lecturers found</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}