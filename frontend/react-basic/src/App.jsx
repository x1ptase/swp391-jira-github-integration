// src/App.jsx
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Login from './pages/Login';
import AdminUserManagement from './pages/AdminUserManagement';
import AdminGroupManagement from './pages/AdminGroupManagement';
import AdminLayout from './pages/AdminLayout';
import LecturerLayout from './pages/LecturerLayout';
import LecturerGroupList from './pages/LecturerGroupList';
import { Navigate } from 'react-router-dom';


function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/admin" element={<AdminLayout />} />
        <Route path="/admin/groups" element={<AdminGroupManagement />} />
        <Route path="/admin/users" element={<AdminUserManagement />} />

        {/* Lecturer Routes */}
        <Route path="/lecturer" element={<LecturerLayout />}>
          <Route index element={<Navigate to="groups" replace />} />
          <Route path="groups" element={<LecturerGroupList />} />
        </Route>
        {/* Sau này thêm các route khác như /dashboard, /admin, /lecturer... */}
      </Routes>
    </Router>
  );
}

export default App;