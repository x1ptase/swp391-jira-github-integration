import { useEffect, useState } from "react";
import "./AdminClassManagement.css";

const CLASS_API = "/api/classes";
const SEMESTER_API = "/api/semesters?page=0&size=999";
const STUDENT_API = "/api/admin/users/unassigned-students";
const ASSIGN_STUDENT_API = "/api/admin/classes";

const COURSE_ID = 1;
const COURSE_CODE = "SWP391";

export default function AdminClassManagement() {
  const [classes, setClasses] = useState([]);
  const [semesters, setSemesters] = useState([]);
  const [loading, setLoading] = useState(false);

  const [filter, setFilter] = useState({ keyword: "", semesterCode: "" });
  const [form, setForm] = useState({ classId: null, classCode: "", semesterId: "" });
  const [formError, setFormError] = useState("");

  // Detail modal
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [detailClass, setDetailClass] = useState(null);
  const [detailStudents, setDetailStudents] = useState([]);
  const [detailLoading, setDetailLoading] = useState(false);

  // Add student modal
  const [showAddStudentModal, setShowAddStudentModal] = useState(false);
  const [addStudentClass, setAddStudentClass] = useState(null);
  const [allStudents, setAllStudents] = useState([]);
  const [studentSearch, setStudentSearch] = useState("");
  const [addingId, setAddingId] = useState(null);
  const [addError, setAddError] = useState("");
  const [addSuccess, setAddSuccess] = useState("");
  const [enrolledStudentIds, setEnrolledStudentIds] = useState(new Set());

  const token = localStorage.getItem("token");
  const auth = () => ({ Authorization: `Bearer ${token}` });
  const authJson = () => ({ ...auth(), "Content-Type": "application/json" });
  useEffect(() => { fetchClasses(); fetchSemesters(); }, []);


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


  const fetchAllUnassignedStudents = async () => {
    const res = await fetch(STUDENT_API, { headers: auth() });
    const data = await res.json();
    setAllStudents(data.data?.content || data.data || []);
  };

  // Form
  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError("");
    if (!form.semesterId) { setFormError("Please select a semester"); return; }
    const method = form.classId ? "PUT" : "POST";
    const url = form.classId ? `${CLASS_API}/${form.classId}` : CLASS_API;
    const body = { classCode: form.classCode, course: { courseId: COURSE_ID }, semester: { semesterId: Number(form.semesterId) } };
    const res = await fetch(url, { method, headers: authJson(), body: JSON.stringify(body) });
    if (!res.ok) { const err = await res.json(); setFormError(err.message || "Error occurred"); return; }
    resetForm(); fetchClasses();
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

  const resetForm = () => { setForm({ classId: null, classCode: "", semesterId: "" }); setFormError(""); };

  // Detail 
  const openDetail = async (cls) => {
    setDetailClass(cls); setDetailLoading(true); setShowDetailModal(true);
    const res = await fetch(`${ASSIGN_STUDENT_API}/${cls.classId}/students`, { headers: auth() });
    const data = await res.json();
    setDetailStudents(data.data || []);
    setDetailLoading(false);
  };

  const handleRemoveStudent = async (classId, studentId) => {
    if (!window.confirm("Remove this student from class?")) return;
    const res = await fetch(`${ASSIGN_STUDENT_API}/${classId}/students/${studentId}`, { method: "DELETE", headers: auth() });
    if (res.ok) {
      setDetailStudents(prev => prev.filter(s => s.userId !== studentId));
      setEnrolledStudentIds(prev => { const next = new Set(prev); next.delete(studentId); return next; });
      fetchClasses();
    } else { const err = await res.json(); alert("Failed: " + (err.message || "Unknown")); }
  };

  // Add Student
  const openAddStudent = async (cls) => {
    setAddStudentClass(cls); setStudentSearch(""); setAddError(""); setAddSuccess("");
    const [studentsRes, enrolledRes] = await Promise.all([
      fetchAllUnassignedStudents(),
      fetch(`${ASSIGN_STUDENT_API}/${cls.classId}/students`, { headers: auth() })
    ]);
    const enrolledData = await enrolledRes.json();
    const ids = new Set((enrolledData.data || []).map(s => s.userId));
    setEnrolledStudentIds(ids);
    setShowAddStudentModal(true);
  };

  const handleAddStudent = async (studentId) => {
    setAddingId(studentId); setAddError(""); setAddSuccess("");
    const res = await fetch(`${ASSIGN_STUDENT_API}/${addStudentClass.classId}/students`, {
      method: "POST", headers: authJson(), body: JSON.stringify({ studentId }),
    });
    const data = await res.json();
    if (!res.ok) { setAddError(data.message || "Failed to add student"); }
    else {
      setAddSuccess("Student added successfully!");
      setEnrolledStudentIds(prev => new Set([...prev, studentId]));
      fetchClasses();
    }
    setAddingId(null);
  };

  const filteredStudents = allStudents.filter(s =>
    s.fullName?.toLowerCase().includes(studentSearch.toLowerCase()) ||
    s.username?.toLowerCase().includes(studentSearch.toLowerCase()) ||
    s.email?.toLowerCase().includes(studentSearch.toLowerCase())
  );

  const getStatusStyle = (status) => {
    if (status === "OPEN") return { bg: "#f0fdf4", color: "#16a34a", border: "#bbf7d0" };
    if (status === "CLOSED") return { bg: "#fef2f2", color: "#dc2626", border: "#fecaca" };
    return { bg: "#f8fafc", color: "#94a3b8", border: "#e2e8f0" };
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
        <div className="acm-card-title">{form.classId ? "Edit Class" : "New Class"}</div>
        <form className="acm-form" onSubmit={handleSubmit}>
          <div className="acm-form-row">
            <div className="acm-field">
              <label className="acm-label">Class Code</label>
              <input className="acm-input" placeholder="e.g. SE1921" value={form.classCode}
                onChange={e => setForm({ ...form, classCode: e.target.value })} required />
            </div>
            <div className="acm-field">
              <label className="acm-label">Course</label>
              <input className="acm-input acm-disabled" value={COURSE_CODE} disabled />
            </div>
            <div className="acm-field">
              <label className="acm-label">Semester</label>
              <select className="acm-select" value={form.semesterId}
                onChange={e => setForm({ ...form, semesterId: e.target.value })} required>
                <option value="">Select semester...</option>
                {semesters.map(s => (
                  <option key={s.semesterId} value={s.semesterId}>{s.semesterCode} — {s.semesterName}</option>
                ))}
              </select>
            </div>
          </div>
          {formError && <div className="acm-form-error">{formError}</div>}
          <div className="acm-form-actions">
            <button type="submit" className="acm-btn-primary">{form.classId ? "Update Class" : "Create Class"}</button>
            {form.classId && <button type="button" className="acm-btn-ghost" onClick={resetForm}>Cancel</button>}
          </div>
        </form>
      </div>

      {/* Filter */}
      <div className="acm-filter-bar">
        <div className="acm-search-wrap">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
          </svg>
          <input className="acm-search-input" placeholder="Search class code..."
            value={filter.keyword} onChange={e => setFilter({ ...filter, keyword: e.target.value })}
            onKeyDown={e => e.key === "Enter" && fetchClasses()} />
        </div>
        <select className="acm-select acm-select-sm" value={filter.semesterCode}
          onChange={e => setFilter({ ...filter, semesterCode: e.target.value })}>
          <option value="">All Semesters</option>
          {semesters.map(s => <option key={s.semesterId} value={s.semesterCode}>{s.semesterCode}</option>)}
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
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {classes.length > 0 ? classes.map((c, i) => {
                const st = getStatusStyle(c.status);
                return (
                  <tr key={c.classId}>
                    <td className="acm-td-num">{i + 1}</td>
                    <td><strong>{c.classCode}</strong></td>
                    <td><span className="acm-course-badge">{c.courseCode}</span></td>
                    <td>
                      <span className="acm-semester-badge">{c.semesterCode}</span>
                      <span className="acm-semester-name">{c.semesterName}</span>
                    </td>

                    <td>
                      <span className="acm-status-badge" style={{ background: st.bg, color: st.color, border: `1px solid ${st.border}` }}>
                        {c.status || "—"}
                      </span>
                    </td>
                    <td>
                      <div className="acm-actions">
                        <button className="acm-btn-action acm-btn-edit" onClick={() => handleEdit(c)}>Edit</button>
                        <button className="acm-btn-action acm-btn-detail" onClick={() => openDetail(c)}>Detail</button>
                        <button className="acm-btn-action acm-btn-student"
                          onClick={() => openAddStudent(c)}
                          disabled={c.status === "CLOSED"}
                          title={c.status === "CLOSED" ? "Class is closed" : "Add student"}>
                          + Student
                        </button>
                        <button className="acm-btn-action acm-btn-danger" onClick={() => handleDelete(c.classId)}>Delete</button>
                      </div>
                    </td>
                  </tr>
                );
              }) : (
                <tr><td colSpan="7" className="acm-empty-row">No classes found</td></tr>
              )}
            </tbody>
          </table>
        )}
      </div>

      {/* Detail Modal */}
      {showDetailModal && (
        <div className="acm-modal-overlay" onClick={() => setShowDetailModal(false)}>
          <div className="acm-modal acm-modal-lg" onClick={e => e.stopPropagation()}>
            <div className="acm-modal-header">
              <div>
                <div className="acm-modal-title">Class Detail — {detailClass?.classCode}</div>
                <div className="acm-modal-subtitle">
                  {detailClass?.lecturerName
                    ? <span>👨‍🏫 {detailClass.lecturerName}</span>
                    : <span style={{ color: "#cbd5e1" }}>No lecturer assigned</span>
                  }
                  {" · "}
                  <span className="acm-status-badge acm-status-badge-sm" style={{
                    ...(() => { const st = getStatusStyle(detailClass?.status); return { background: st.bg, color: st.color, border: `1px solid ${st.border}` }; })(),
                  }}>{detailClass?.status || "—"}</span>
                </div>
              </div>
              <button className="acm-modal-close" onClick={() => setShowDetailModal(false)}>×</button>
            </div>
            <div className="acm-modal-body">
              {detailLoading ? (
                <div className="acm-loading"><span className="acm-spinner" /> Loading...</div>
              ) : detailStudents.length === 0 ? (
                <div className="acm-empty-row" style={{ padding: "32px", textAlign: "center", color: "#94a3b8" }}>No students enrolled</div>
              ) : (
                <table className="acm-table">
                  <thead><tr><th>#</th><th>Full Name</th><th>Username</th><th>Email</th><th>Student Code</th><th>Action</th></tr></thead>
                  <tbody>
                    {detailStudents.map((s, i) => (
                      <tr key={s.userId}>
                        <td className="acm-td-num">{i + 1}</td>
                        <td>{s.fullName}</td>
                        <td><span className="acm-username">{s.username}</span></td>
                        <td>{s.email}</td>
                        <td>{s.studentCode || "—"}</td>
                        <td>
                          <button className="acm-btn-action acm-btn-danger"
                            onClick={() => handleRemoveStudent(detailClass.classId, s.userId)}>
                            Remove
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Add Student Modal */}
      {showAddStudentModal && (
        <div className="acm-modal-overlay" onClick={() => setShowAddStudentModal(false)}>
          <div className="acm-modal acm-modal-lg" onClick={e => e.stopPropagation()}>
            <div className="acm-modal-header">
              <div>
                <div className="acm-modal-title">Add Student — {addStudentClass?.classCode}</div>
                <div className="acm-modal-subtitle">{addStudentClass?.semesterCode}</div>
              </div>
              <button className="acm-modal-close" onClick={() => setShowAddStudentModal(false)}>×</button>
            </div>
            <div className="acm-modal-body">
              {addSuccess && <div className="acm-add-success">{addSuccess}</div>}
              {addError && <div className="acm-add-error">{addError}</div>}
              <div className="acm-search-wrap" style={{ marginBottom: 14 }}>
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
                </svg>
                <input className="acm-search-input" placeholder="Search by name, username, email..."
                  value={studentSearch} onChange={e => setStudentSearch(e.target.value)} autoFocus />
              </div>
              <table className="acm-table">
                <thead><tr><th>#</th><th>Full Name</th><th>Username</th><th>Email</th><th>Student Code</th><th>Action</th></tr></thead>
                <tbody>
                  {filteredStudents.length > 0 ? filteredStudents.map((s, i) => (
                    <tr key={s.userId}>
                      <td className="acm-td-num">{i + 1}</td>
                      <td>{s.fullName}</td>
                      <td><span className="acm-username">{s.username}</span></td>
                      <td>{s.email}</td>
                      <td>{s.studentCode || "—"}</td>

                      <td>
                        {enrolledStudentIds.has(s.userId)
                          ? <span className="acm-enrolled-tag">✓ Enrolled</span>
                          : <button className="acm-btn-action acm-btn-student"
                            onClick={() => handleAddStudent(s.userId)}
                            disabled={addingId === s.userId}>
                            {addingId === s.userId ? <span className="acm-spinner-sm" /> : "Add"}
                          </button>
                        }

                      </td>
                    </tr>
                  )) : (
                    <tr><td colSpan="6" className="acm-empty-row">No students found</td></tr>
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