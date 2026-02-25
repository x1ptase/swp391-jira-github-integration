import { Link, Outlet, useLocation } from "react-router-dom";
import "./AdminLayout.css";

function AdminLayout() {
  const username = localStorage.getItem("username");
  const location = useLocation();
  
  const handleLogout = () => {
    localStorage.clear();
    window.location.href = "/login";
  };

  return (
    <div className="layout">
      <aside className="sidebar">
        <div>
          <h2 className="logo">Admin Panel</h2>

          <nav className="nav-menu">
            <Link 
              to="users" 
              className={`nav-item ${location.pathname.includes('users') ? 'active' : ''}`}
            >
              <span className="nav-icon">👤</span>
              <span>Users</span>
            </Link>
            <Link 
              to="groups" 
              className={`nav-item ${location.pathname.includes('groups') ? 'active' : ''}`}
            >
              <span className="nav-icon">👥</span>
              <span>Groups</span>
            </Link>
          </nav>
        </div>

        <div className="sidebar-footer">
          <div className="user-info">
            <span className="user-icon">👨‍💼</span>
            <span className="username">{username || "Admin"}</span>
          </div>
          <button className="logout-btn" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </aside>

      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}

export default AdminLayout;
