// src/App.jsx
import Login from './pages/Login';
import AdminUserManagement from './pages/AdminUserManagement';
import AdminClassManagement from './pages/AdminClassManagement';
import AdminLayout from './pages/AdminLayout';
import LecturerClassList from "./pages/LecturerClassList";
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import GitHubConfig from './pages/GitHubConfig';
import JiraConfig from './pages/JiraConfig';
import JiraIssuesPreview from "./pages/JiraIssuesPreview";
import LeaderDashboard from "./pages/LeaderDashboard";
import StudentDashboard from "./pages/StudentDashboard";
import AdminSemesterManagement from "./pages/AdminSemesterManagement";
import AdminLecturerManagement from "./pages/AdminLecturerManagement";
import AdminTopicManagement from "./pages/AdminTopicManagement";
import LecturerGroupManagement from "./pages/LecturerGroupManagement";
import LecturerGroupDetail from "./pages/LecturerGroupDetail";



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
          <Route path="classes" element={<AdminClassManagement />} />
          <Route path="classes/:classId/github-config" element={<GitHubConfig />} />
          <Route path="classes/:classId/jira-config" element={<JiraConfig />} />
          <Route path="semesters" element={<AdminSemesterManagement />} />
          <Route path="lecturers" element={<AdminLecturerManagement />} />
          <Route path="topics" element={<AdminTopicManagement />} />
        </Route>

        {/* Lecturer */}
        <Route path="/lecturer">
          <Route path="groups" element={<LecturerClassList />} />
          <Route path="groups/:groupId/jira-issues" element={<JiraIssuesPreview />} />
          <Route path="classes/:classId" element={<LecturerGroupManagement />} />
          <Route path="groups/:groupId/detail" element={<LecturerGroupDetail />} />

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