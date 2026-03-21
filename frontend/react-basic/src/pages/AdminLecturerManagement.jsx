import { useEffect, useState } from "react";
import "./AdminLecturerManagement.css";

const LECTURER_API = "/api/admin/users?roleCode=LECTURER&page=0&size=999";
const CLASS_API = "/api/classes?page=0&size=999";
const ASSIGN_API = "/api/admin/classes";

export default function AdminLecturerManagement() {
  const [lecturers, setLecturers] = useState([]);
  const [classes, setClasses] = useState([]);
  const [loading, setLoading] = useState(false);

  // Assign modal
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [selectedLecturer, setSelectedLecturer] = useState(null);
  const [assigning, setAssigning] = useState(null);
  const [assignError, setAssignError] = useState("");
  const [assignSuccess, setAssignSuccess] = useState("");
  const [classSearch, setClassSearch] = useState("");

  const token = localStorage.getItem("token");
  const auth = () => ({ Authorization: `Bearer ${token}` });
  const authJson = () => ({ ...auth(), "Content-Type": "application/json" });

  useEffect(() => { fetchData(); }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [lecRes, clsRes] = await Promise.all([
        fetch(LECTURER_API, { headers: auth() }),
        fetch(CLASS_API, { headers: auth() }),
      ]);
      const lecData = await lecRes.json();
      const clsData = await clsRes.json();
      setLecturers(lecData.data?.content || lecData.data || []);
      setClasses(clsData.data?.content || clsData.data || []);
    } catch { }
    finally { setLoading(false); }
  };

  // Get classes assigned to a lecturer
  const getAssignedClasses = (lecturerId) =>
    classes.filter(c => c.lecturerId === lecturerId);

  const openAssignModal = (lecturer) => {
    setSelectedLecturer(lecturer);
    setAssignError(""); setAssignSuccess(""); setClassSearch("");
    setShowAssignModal(true);
  };

  const handleAssign = async (classId) => {
    setAssigning(classId); setAssignError(""); setAssignSuccess("");
    const res = await fetch(`${ASSIGN_API}/${classId}/lecturer`, {
      method: "PUT", headers: authJson(),
      body: JSON.stringify({ lecturerId: selectedLecturer.userId }),
    });
    if (res.ok) {
      setAssignSuccess("Assigned successfully!");
      await fetchData();
    } else {
      const err = await res.json();
      setAssignError(err.message || "Failed to assign");
    }
    setAssigning(null);
  };

  const filteredClasses = classes.filter(c =>
    c.classCode?.toLowerCase().includes(classSearch.toLowerCase()) ||
    c.semesterCode?.toLowerCase().includes(classSearch.toLowerCase())
  );

  return (
    <div className="alm-root">
      <div className="alm-page-header">
        <h1 className="alm-page-title">Lecturer Management</h1>
        <p className="alm-page-desc">View lecturers and manage their class assignments</p>
      </div>

      <div className="alm-card alm-table-card">
        {loading ? (
          <div className="alm-loading"><span className="alm-spinner" /> Loading...</div>
        ) : (
          <table className="alm-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Full Name</th>
                <th>Username</th>
                <th>Email</th>
                <th>Assigned Classes</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {lecturers.length > 0 ? lecturers.map((l, i) => {
                const assigned = getAssignedClasses(l.userId);
                return (
                  <tr key={l.userId}>
                    <td className="alm-td-num">{i + 1}</td>
                    <td className="alm-name-cell">{l.fullName}</td>
                    <td><span className="alm-username">{l.username}</span></td>
                    <td className="alm-email">{l.email}</td>
                    <td>
                      {assigned.length === 0
                        ? <span className="alm-no-class">Not assigned</span>
                        : <div className="alm-class-chips">
                            {assigned.map(c => (
                              <span key={c.classId} className="alm-class-chip" title={c.semesterName}>
                                {c.classCode}
                                <span className="alm-chip-sem">{c.semesterCode}</span>
                              </span>
                            ))}
                          </div>
                      }
                    </td>
                    <td>
                      <button className="alm-btn-action alm-btn-assign" onClick={() => openAssignModal(l)}>
                        Assign Class
                      </button>
                    </td>
                  </tr>
                );
              }) : (
                <tr><td colSpan="6" className="alm-empty-row">No lecturers found</td></tr>
              )}
            </tbody>
          </table>
        )}
      </div>

      {/* Assign Modal */}
      {showAssignModal && (
        <div className="alm-modal-overlay" onClick={() => setShowAssignModal(false)}>
          <div className="alm-modal" onClick={e => e.stopPropagation()}>
            <div className="alm-modal-header">
              <div>
                <div className="alm-modal-title">Assign Class</div>
                <div className="alm-modal-subtitle">{selectedLecturer?.fullName} · {selectedLecturer?.username}</div>
              </div>
              <button className="alm-modal-close" onClick={() => setShowAssignModal(false)}>×</button>
            </div>
            <div className="alm-modal-body">
              {assignSuccess && <div className="alm-success">{assignSuccess}</div>}
              {assignError && <div className="alm-error">{assignError}</div>}

              {/* Search */}
              <div className="alm-search-wrap">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
                </svg>
                <input
                  className="alm-search-input"
                  placeholder="Search class code or semester..."
                  value={classSearch}
                  onChange={e => setClassSearch(e.target.value)}
                  autoFocus
                />
              </div>

              <table className="alm-table">
                <thead>
                  <tr>
                    <th>Class Code</th>
                    <th>Course</th>
                    <th>Semester</th>
                    <th>Current Lecturer</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredClasses.length > 0 ? filteredClasses.map(c => {
                    const isAssignedToThis = c.lecturerId === selectedLecturer?.userId;
                    return (
                      <tr key={c.classId} className={isAssignedToThis ? "alm-row-assigned" : ""}>
                        <td><strong>{c.classCode}</strong></td>
                        <td><span className="alm-course-badge">{c.courseCode}</span></td>
                        <td><span className="alm-sem-badge">{c.semesterCode}</span></td>
                        <td>
                          {c.lecturerName
                            ? <span className={`alm-lecturer-chip ${isAssignedToThis ? "alm-chip-current" : ""}`}>
                                {c.lecturerName}
                              </span>
                            : <span className="alm-no-class">Not assigned</span>
                          }
                        </td>
                        <td>
                          {isAssignedToThis
                            ? <span className="alm-assigned-tag">✓ Assigned</span>
                            : <button
                                className="alm-btn-action alm-btn-assign"
                                onClick={() => handleAssign(c.classId)}
                                disabled={assigning === c.classId}
                              >
                                {assigning === c.classId ? <span className="alm-spinner-sm"/> : "Assign"}
                              </button>
                          }
                        </td>
                      </tr>
                    );
                  }) : (
                    <tr><td colSpan="5" className="alm-empty-row">No classes found</td></tr>
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