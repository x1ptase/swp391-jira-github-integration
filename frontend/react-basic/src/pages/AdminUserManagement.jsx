import { useEffect, useState } from "react";
import "./AdminUserManagement.css";

const API = "/api/admin/users";

function AdminUserManagement() {
  const [users, setUsers] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [error, setError] = useState("");

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
  const fetchUsers = async (p = 0) => {
    try {
      const res = await fetch(`${API}?page=${p}&size=7`, {
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
      });
      const json = await res.json();
      setUsers(json.data.content);
      setTotalPages(json.data.totalPages);
      setPage(p);
    } catch (err) {
      console.error("Fetch error:", err);
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

      //  Parse response
      const data = await res.json();

      // Kiểm tra lỗi và hiển thị message từ BE
      if (!res.ok) {
        // Lấy message từ backend
        const errorMessage = data.message || "An error occurred";
        setError(errorMessage);
        return;
      }

      //  Thành công
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

      // Kiểm tra response
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
      <h2>User Management</h2>
      
      {/*Error Banner */}
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
          placeholder="GitHub Username"
          value={form.githubUsername}
          onChange={(e) =>
            setForm({ ...form, githubUsername: e.target.value })
          }
        />

        <input
          placeholder="Jira Account ID"
          value={form.jiraAccountId}
          onChange={(e) =>
            setForm({ ...form,jiraAccountId: e.target.value })
          }
        />

        <input
          placeholder="Email"
          value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
          required
        />

        <input
          placeholder="Full name"
          value={form.fullName}
          onChange={(e) => setForm({ ...form, fullName: e.target.value })}
          required
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

        <select
          value={form.roleCode}
          onChange={(e) =>
            setForm({ ...form, roleCode: e.target.value })
          }
          required
        >
          <option value="STUDENT">STUDENT</option>
          <option value="ADMIN">ADMIN</option>
          <option value="LECTURER">LECTURER</option>
        </select>

        <div className="form-actions">
          <button type="submit">{form.userId ? "Update" : "Create"}</button>
          {form.userId && (
            <button type="button" className="cancel" onClick={resetForm}>
              Cancel
            </button>
          )}
        </div>
      </form>

      {/*  TABLE  */}
      <table className="user-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Username</th>
            <th>Email</th>
            <th>GitHub Username</th>
            <th>Jira Account ID</th>
            <th>Full Name</th>
            <th>Role</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {users.map((u, index) => (
            <tr key={u.userId}>
              <td>{page * 7 + index + 1}</td>
              <td>{u.username}</td>
              <td>{u.email}</td>
              <td>{u.githubUsername || "-"}</td>
              <td>{u.jiraAccountId|| "-"}</td>
              <td>{u.fullName}</td>
              <td>{u.roleCode}</td>
              <td>
                <button onClick={() => handleEdit(u)}>Edit</button>
                <button onClick={() => handleDelete(u.userId)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {/*  PAGINATION  */}
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
    </div>
  );
}

export default AdminUserManagement;