import { Link, Outlet, useLocation } from "react-router-dom";
import "./LecturerLayout.css";

function LecturerLayout() {
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
          <h2 className="logo">Jira & Github Support</h2>

          <nav className="nav-menu">
            <Link 
              to="groups" 
              className={`nav-item ${location.pathname.includes('groups') ? 'active' : ''}`}
            >
              <span className="nav-icon">👥</span>
              My Groups
            </Link>
          </nav>
        </div>

        <div className="sidebar-footer">
          <div className="user-info">
            <span className="user-icon">🧑🏻‍🏫</span>
            <span className="username">{username}</span>
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

export default LecturerLayout;