import { useEffect, useState } from "react";
import "./AdminUserManagement.css";

const API = "/api/admin/users";

function AdminUserManagement() {
  const [users, setUsers] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [error, setError] = useState("");
  const [keyword, setKeyword] = useState("");
  const [filterRole, setFilterRole] = useState("");

  const [form, setForm] = useState({
    userId: null,
    username: "",
    email: "",
    githubUsername: "",
    jiraAccountId: "",
    fullName: "",
    password: "",
    roleCode: "STUDENT",
  });

  // ===== FETCH USERS =====
  const fetchUsers = async (p = 0, search = keyword, role = filterRole) => {
    try {
      const res = await fetch(
        `${API}?page=${p}&size=7&keyword=${search}&roleCode=${role}`,
        {
          headers: {
            Authorization: `Bearer ${localStorage.getItem("token")}`,
          },
        }
      );
      const json = await res.json();
      setUsers(json.data.content);
      setTotalPages(json.data.totalPages);
      setPage(p);
    } catch (err) {
      console.error("Fetch error:", err);
      setError("Failed to fetch users");
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  // ===== CREATE / UPDATE =====
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    try {
      let res;

      if (form.userId) {
        // UPDATE
        res = await fetch(`${API}/${form.userId}`, {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${localStorage.getItem("token")}`,
          },
          body: JSON.stringify({
            email: form.email,
            fullName: form.fullName,
            roleCode: form.roleCode,
            githubUsername: form.githubUsername,
            jiraAccountId: form.jiraAccountId,
          }),
        });
      } else {
        // CREATE
        res = await fetch(API, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${localStorage.getItem("token")}`,
          },
          body: JSON.stringify({
            username: form.username,
            email: form.email,
            fullName: form.fullName,
            password: form.password,
            roleCode: form.roleCode,
            githubUsername: form.githubUsername,
            jiraAccountId: form.jiraAccountId,
          }),
        });
      }

      const data = await res.json();

      if (!res.ok) {
        const errorMessage = data.message || "An error occurred";
        setError(errorMessage);
        return;
      }

      alert(form.userId ? "User updated successfully!" : "User created successfully!");
      resetForm();
      fetchUsers(page);

    } catch (err) {
      console.error("Submit error:", err);
      setError("Network error. Please try again.");
    }
  };

  // ===== DELETE =====
  const handleDelete = async (id) => {
    if (!window.confirm("Delete this user?")) return;
    setError("");

    try {
      const res = await fetch(`${API}/${id}`, {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
      });

      if (!res.ok) {
        const errorData = await res.json();
        setError(errorData.message || "Failed to delete user");
        return;
      }

      alert("User deleted successfully!");
      fetchUsers(page);

    } catch (err) {
      console.error("Delete error:", err);
      setError("Network error. Please try again.");
    }
  };

  // ===== EDIT =====
  const handleEdit = (u) => {
    setError("");
    setForm({
      userId: u.userId,
      username: u.username,
      email: u.email,
      fullName: u.fullName,
      password: "",
      roleCode: u.roleCode,
      githubUsername: u.githubUsername || "",
      jiraAccountId: u.jiraAccountId || "",
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const resetForm = () => {
    setError("");
    setForm({
      userId: null,
      username: "",
      email: "",
      fullName: "",
      password: "",
      roleCode: "STUDENT",
      githubUsername: "",
      jiraAccountId: "",
    });
  };

  return (
    <div className="admin-container">
      <h2>👤 User Management</h2>

      {error && (
        <div className="error-banner">
          <span className="error-icon">⚠️</span>
          <span>{error}</span>
          <button className="error-close" onClick={() => setError("")}>×</button>
        </div>
      )}

      {/* FORM */}
      <form className="user-form" onSubmit={handleSubmit}>
        <input
          placeholder="Username"
          value={form.username}
          disabled={!!form.userId}
          onChange={(e) => setForm({ ...form, username: e.target.value })}
          required
        />
        <input
          placeholder="Email"
          type="email"
          value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
          required
        />

        <input
          placeholder="Full Name"
          value={form.fullName}
          onChange={(e) => setForm({ ...form, fullName: e.target.value })}
          required
        />

        <select
          value={form.roleCode}
          onChange={(e) => setForm({ ...form, roleCode: e.target.value })}
          required
        >
          <option value="STUDENT">STUDENT</option>
          <option value="ADMIN">ADMIN</option>
          <option value="LECTURER">LECTURER</option>
        </select>

        <input
          placeholder="GitHub Username"
          value={form.githubUsername}
          onChange={(e) => setForm({ ...form, githubUsername: e.target.value })}
        />

        <input
          placeholder="Jira Account ID"
          value={form.jiraAccountId}
          onChange={(e) => setForm({ ...form, jiraAccountId: e.target.value })}
        />

        {!form.userId && (
          <input
            type="password"
            placeholder="Password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            required
          />
        )}

        <div style={{ gridColumn: "1 / -1" }} />

        <div className="form-actions">
          <button type="submit">{form.userId ? "Update User" : "Create User"}</button>
          {form.userId && (
            <button type="button" className="cancel" onClick={resetForm}>
              Cancel
            </button>
          )}
        </div>
      </form>

      {/* SEARCH + FILTER */}
      <div className="search-bar">
        <input
          type="text"
          placeholder="🔍 Search username or email"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />

        <select
          value={filterRole}
          onChange={(e) => setFilterRole(e.target.value)}
        >
          <option value="">All Roles</option>
          <option value="STUDENT">STUDENT</option>
          <option value="ADMIN">ADMIN</option>
          <option value="LECTURER">LECTURER</option>
        </select>

        <button onClick={() => fetchUsers(0)}>
          Search
        </button>
      </div>

      {/* TABLE */}
      <table className="user-table">
        <thead>
          <tr>
            <th>#</th>
            <th>Username</th>
            <th>Email</th>
            <th>Full Name</th>
            <th>GitHub</th>
            <th>Jira ID</th>
            <th>Role</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {users.length > 0 ? (
            users.map((u, index) => (
              <tr key={u.userId}>
                <td>{page * 7 + index + 1}</td>
                <td>{u.username}</td>
                <td>{u.email}</td>
                <td>{u.fullName}</td>
                <td>{u.githubUsername || "-"}</td>
                <td>{u.jiraAccountId || "-"}</td>
                <td>
                  <span style={{
                    padding: "4px 8px",
                    borderRadius: "4px",
                    fontSize: "12px",
                    fontWeight: "600",
                    background: u.roleCode === "ADMIN" ? "#fee2e2" : u.roleCode === "LECTURER" ? "#dbeafe" : "#f0fdf4",
                    color: u.roleCode === "ADMIN" ? "#991b1b" : u.roleCode === "LECTURER" ? "#1e40af" : "#166534"
                  }}>
                    {u.roleCode}
                  </span>
                </td>
                <td>
                  <button onClick={() => handleEdit(u)}>Edit</button>
                  <button onClick={() => handleDelete(u.userId)}>Delete</button>
                </td>
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan="8" style={{ textAlign: "center", padding: "32px", color: "var(--text-tertiary)" }}>
                No users found
              </td>
            </tr>
          )}
        </tbody>
      </table>

      {/* PAGINATION */}
      {totalPages > 1 && (
        <div className="pagination">
          {Array.from({ length: totalPages }, (_, i) => (
            <button
              key={i}
              className={i === page ? "active" : ""}
              onClick={() => fetchUsers(i)}
            >
              {i + 1}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export default AdminUserManagement;
