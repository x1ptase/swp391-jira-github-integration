import { Link, Outlet } from "react-router-dom";

function AdminLayout() {
  return (
    <div className="admin-container">
      <h2>Welcome, Admin</h2>

      <nav className="admin-nav">
        <div><Link to="users">Users</Link></div>
        <div><Link to="groups">Groups</Link></div>
      </nav>

      <Outlet />
    </div>
  );
}

export default AdminLayout;
