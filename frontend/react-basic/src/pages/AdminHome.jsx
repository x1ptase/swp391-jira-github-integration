import { useEffect, useState } from "react";
import "./AdminHome.css";

const API = "/api/admin/users";

function AdminHome() {
  const [users, setUsers] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [form, setForm] = useState({
    userId: null,
    username: "",
    email: "",
    githubUsername: "",
    jiraEmail: "",
    fullName: "",
    password: "",
    roleCode: "STUDENT",
  });

  // ===== FETCH USERS =====
  const fetchUsers = async (p = 0) => {
    const res = await fetch(`${API}?page=${p}&size=7`, {
      headers: {
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
    });
    const json = await res.json();
    setUsers(json.data.content);
    setTotalPages(json.data.totalPages);
    setPage(p);
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  // ===== CREATE / UPDATE =====
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (form.userId) {
      // UPDATE (không đổi password)
      await fetch(`${API}/${form.userId}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
        body: JSON.stringify({
          email: form.email,
          fullName: form.fullName,
          roleCode: "STUDENT",
          githubUsername: form.githubUsername,
          jiraEmail: form.jiraEmail,
        }),
      });
    } else {
      // CREATE
      await fetch(API, {
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
          roleCode: "STUDENT",
          githubUsername: form.githubUsername,
          jiraEmail: form.jiraEmail,
        }),
      });
    }

    resetForm();
    fetchUsers(page);
  };

  // ===== DELETE =====
  const handleDelete = async (id) => {
    if (!window.confirm("Delete this user?")) return;
    await fetch(`${API}/${id}`, {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
    });
    fetchUsers(page);
  };

  // ===== EDIT =====
  const handleEdit = (u) => {
    setForm({
      userId: u.userId,
      username: u.username,
      email: u.email,
      fullName: u.fullName,
      password: "",
      roleCode: "STUDENT",
      githubUsername: u.githubUsername,
      jiraEmail: u.jiraEmail,
    });
  };

  const resetForm = () => {
    setForm({
      userId: null,
      username: "",
      email: "",
      fullName: "",
      password: "",
      roleCode: "STUDENT",
      githubUsername: "",
      jiraEmail: "",
    });
  };

  return (
    <div className="admin-container">
      <h2>User Management (Admin)</h2>

      {/* ===== FORM ===== */}
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
          placeholder="Jira Email"
          value={form.jiraEmail}
          onChange={(e) =>
            setForm({ ...form, jiraEmail: e.target.value })
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
            <th>Jira Email</th>
            <th>Full Name</th>
            <th>Role</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {users.map((u,index) => (
            <tr key={u.userId}>
              <td>{page * 7 + index + 1}</td>
              <td>{u.username}</td>
              <td>{u.email}</td>
              <td>{u.githubUsername}</td>
              <td>{u.jiraEmail}</td> 
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

export default AdminHome;
