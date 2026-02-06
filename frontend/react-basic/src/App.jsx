// src/App.jsx
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Login from './pages/Login';
import AdminHome from './pages/AdminHome';


function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/admin" element={<AdminHome/>} />
        {/* Sau này thêm các route khác như /dashboard, /admin, /lecturer... */}
      </Routes>
    </Router>
  );
}

export default App;