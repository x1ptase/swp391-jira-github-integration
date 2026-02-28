// src/App.jsx
import Login from './pages/Login';
import AdminUserManagement from './pages/AdminUserManagement';
import AdminGroupManagement from './pages/AdminGroupManagement';
import AdminLayout from './pages/AdminLayout';
import LecturerLayout from './pages/LecturerLayout';
import LecturerGroupList from './pages/LecturerGroupList';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import GitHubConfig from './pages/GitHubConfig';
import JiraConfig from './pages/JiraConfig';


function App() {
  return (
     <BrowserRouter>
      <Routes>
        {/* Public Routes */}
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<Login />} />

        {/* Admin Routes */}
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<Navigate to="users" replace />} />
          <Route path="users" element={<AdminUserManagement />} />
          <Route path="groups" element={<AdminGroupManagement />} />
          <Route path="groups/:groupId/github-config" element={<GitHubConfig />} />
          <Route path="groups/:groupId/jira-config" element={<JiraConfig />} />
        </Route>

        {/* Lecturer Routes */}
        <Route path="/lecturer" element={<LecturerLayout />}>
          <Route index element={<Navigate to="groups" replace />} />
          <Route path="groups" element={<LecturerGroupList />} />
          <Route path="groups/:groupId/github-config" element={<GitHubConfig />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
export default App;