// src/pages/admin/AdminGroupManagement.jsx
import { useEffect, useState } from "react";
import "./AdminGroupManagement.css";

const API_URL = "/api/student_group";

function AdminGroupManagement() {
  const [groups, setGroups] = useState([]);
  const [form, setForm] = useState({
    groupId: null,
    groupCode: "",
    groupName: "",
    courseCode: "",
    semester: "",
  });

  const [filter, setFilter] = useState({
    courseCode: "",
    semester: "",
  });

  const [error, setError] = useState("");

  const token = localStorage.getItem("token");

  useEffect(() => {
    fetchGroups();
  }, []);

  const fetchGroups = async () => {
    let query = [];
    if (filter.courseCode) query.push(`course_code=${filter.courseCode}`);
    if (filter.semester) query.push(`semester=${filter.semester}`);

    const url = query.length ? `${API_URL}?${query.join("&")}` : API_URL;

    const res = await fetch(url, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
    const data = await res.json();
    setGroups(data.data || []);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    const method = form.groupId ? "PUT" : "POST";
    const url = form.groupId
      ? `${API_URL}/update/${form.groupId}`
      : `${API_URL}/add`;

    const res = await fetch(url, {
      method,
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        group_code: form.groupCode,
        group_name: form.groupName,
        course_code: form.courseCode,
        semester: form.semester,
      }),
    });

    if (!res.ok) {
      const err = await res.json();
      if (res.status === 409) {
        setError("Group Code already exists");
      } else {
        setError(err.message || "Error occurred");
      }
      return;
    }

    resetForm();
    fetchGroups();
  };

  const handleEdit = (g) => {
    setForm({
      groupId: g.groupId,
      groupCode: g.groupCode,
      groupName: g.groupName,
      courseCode: g.courseCode,
      semester: g.semester,
    });
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Delete this group?")) return;

    await fetch(`${API_URL}/delete/${id}`, {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    fetchGroups();
  };

  const resetForm = () => {
    setForm({
      groupId: null,
      groupCode: "",
      groupName: "",
      courseCode: "",
      semester: "",
    });
    setError("");
  };

  return (
    <div className="group-container">
      <h3>Group Management</h3>

      {/* FILTER */}
      <div className="group-filter">
        <input
          placeholder="Course Code"
          value={filter.courseCode}
          onChange={(e) =>
            setFilter({ ...filter, courseCode: e.target.value })
          }
        />
        <input
          placeholder="Semester"
          value={filter.semester}
          onChange={(e) =>
            setFilter({ ...filter, semester: e.target.value })
          }
        />
        <button onClick={fetchGroups}>Filter</button>
        <button className="cancel" onClick={() => {
          setFilter({ courseCode: "", semester: "" });
          fetchGroups();
        }}>
          Clear
        </button>
      </div>

      {/* FORM */}
      <form className="group-form" onSubmit={handleSubmit}>
        <input
          placeholder="Group Code"
          value={form.groupCode}
          disabled={!!form.groupId}
          onChange={(e) =>
            setForm({ ...form, groupCode: e.target.value })
          }
          required
        />

        <input
          placeholder="Group Name"
          value={form.groupName}
          onChange={(e) =>
            setForm({ ...form, groupName: e.target.value })
          }
          required
        />

        <input
          placeholder="Course Code"
          value={form.courseCode}
          onChange={(e) =>
            setForm({ ...form, courseCode: e.target.value })
          }
          required
        />

        <input
          placeholder="Semester"
          value={form.semester}
          onChange={(e) =>
            setForm({ ...form, semester: e.target.value })
          }
          required
        />

        <div className="form-actions">
          <button type="submit">
            {form.groupId ? "Update" : "Create"}
          </button>
          {form.groupId && (
            <button type="button" className="cancel" onClick={resetForm}>
              Cancel
            </button>
          )}
        </div>

        {error && <p className="error">{error}</p>}
      </form>

      {/* TABLE */}
      <table className="group-table">
        <thead>
          <tr>
            <th>#</th>
            <th>Group Code</th>
            <th>Group Name</th>
            <th>Course Code</th>
            <th>Semester</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {groups.map((g, index) => (
            <tr key={g.groupId}>
              <td>{index + 1}</td>
              <td>{g.groupCode}</td>
              <td>{g.groupName}</td>
              <td>{g.courseCode}</td>
              <td>{g.semester}</td>
              <td>
                <button onClick={() => handleEdit(g)}>Edit</button>
                <button
                  className="danger"
                  onClick={() => handleDelete(g.groupId)}
                >
                  Delete
                </button>
              </td>
            </tr>
          ))}
          {groups.length === 0 && (
            <tr>
              <td colSpan="6" style={{ textAlign: "center" }}>
                No data
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

export default AdminGroupManagement;
