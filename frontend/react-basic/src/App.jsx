// src/App.jsx
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Login from './pages/Login';
import AdminUserManagement from './pages/AdminUserManagement';
import AdminGroupManagement from './pages/AdminGroupManagement';
import AdminLayout from './pages/AdminLayout';


function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/admin" element={<AdminLayout />} />
        <Route path="/admin/groups" element={<AdminGroupManagement />} />
        <Route path="/admin/users" element={<AdminUserManagement />} />
        {/* Sau này thêm các route khác như /dashboard, /admin, /lecturer... */}
      </Routes>
    </Router>
  );
}

export default App;