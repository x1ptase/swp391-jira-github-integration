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
import JiraIssuesPreview from "./pages/JiraIssuesPreview";
import LecturerGroupStats from "./pages/LecturerGroupStats";
import LeaderDashboard from "./pages/LeaderDashboard";
import StudentDashboard from "./pages/StudentDashboard";


function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public */}
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<Login />} />

        {/* Admin */}
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<Navigate to="users" replace />} />
          <Route path="users" element={<AdminUserManagement />} />
          <Route path="groups" element={<AdminGroupManagement />} />
          <Route path="groups/:groupId/github-config" element={<GitHubConfig />} />
          <Route path="groups/:groupId/jira-config" element={<JiraConfig />} />
        </Route>

        {/* Lecturer */}
        <Route path="/lecturer" element={<LecturerLayout />}>
          <Route index element={<Navigate to="groups" replace />} />
          <Route path="groups" element={<LecturerGroupList />} />
          <Route path="groups/:groupId/jira-issues" element={<JiraIssuesPreview />} />
          <Route path="groups/:groupId/stats" element={<LecturerGroupStats />} />
        </Route>

        {/* Student — danh sách group */}
        <Route path="/student/groups" element={<StudentDashboard />} />

        {/* Leader — quản lý group (GitHub + Jira config) */}
        <Route path="/leader/groups/:groupId" element={<LeaderDashboard />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;