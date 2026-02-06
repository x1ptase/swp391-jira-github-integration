import { useEffect, useState } from "react";
import "./AdminHome.css";

const API = "/api/admin/users";

function AdminHome() {
  const [users, setUsers] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [form, setForm] = useState({
    id: null,
    username: "",
    email: "",
    fullName: "",
    role: "USER",
  });

  //  FETCH USERS 
  const fetchUsers = async (p = 0) => {
    try {
      const res = await fetch(`${API}?page=${p}&size=5`, {
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
      });
      const data = await res.json();
      setUsers(data.data.content);
      setTotalPages(data.data.totalPages);
      setPage(p);
    } catch (err) {
      console.error("Error fetching users:", err);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  // CREATE / UPDATE 
  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (form.id) {
        await fetch(`${API}/${form.id}`, {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${localStorage.getItem("token")}`,
          },
          body: JSON.stringify(form),
        });
      } else {
        await fetch(API, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${localStorage.getItem("token")}`,
          },
          body: JSON.stringify(form),
        });
      }
      resetForm();
      fetchUsers(page);
    } catch (err) {
      console.error("Error saving user:", err);
    }
  };

  //  DELETE 
  const handleDelete = async (id) => {
    if (!window.confirm("Delete this user?")) return;
    try {
      await fetch(`${API}/${id}`, {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
      });
      fetchUsers(page);
    } catch (err) {
      console.error("Error deleting user:", err);
    }
  };

  //  EDIT 
  const handleEdit = (u) => {
    setForm({
      id: u.id,
      username: u.username,
      email: u.email,
      fullName: u.fullName,
      role: u.role,
    });
  };

  const resetForm = () => {
    setForm({
      id: null,
      username: "",
      email: "",
      fullName: "",
      role: "USER",
    });
  };

  return (
    <div className="admin-container">
      <h2>User Management (Admin)</h2>

      {/* FORM */}
      <form className="user-form" onSubmit={handleSubmit}>
        <input
          placeholder="Username"
          value={form.username}
          onChange={(e) => setForm({ ...form, username: e.target.value })}
          required
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
        />
        <select
          value={form.role}
          onChange={(e) => setForm({ ...form, role: e.target.value })}
        >
          <option value="ADMIN">ADMIN</option>
          <option value="USER">USER</option>
        </select>

        <div className="form-actions">
          <button type="submit">{form.id ? "Update" : "Create"}</button>
          {form.id && (
            <button type="button" className="cancel" onClick={resetForm}>
              Cancel
            </button>
          )}
        </div>
      </form>

      {/* TABLE */}
      <table className="user-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Username</th>
            <th>Email</th>
            <th>Full Name</th>
            <th>Role</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {users.map((u) => (
            <tr key={u.id}>
              <td>{u.id}</td>
              <td>{u.username}</td>
              <td>{u.email}</td>
              <td>{u.fullName}</td>
              <td>{u.role}</td>
              <td>
                <button onClick={() => handleEdit(u)}>Edit</button>
                <button onClick={() => handleDelete(u.id)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {/*PAGINATION*/}
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