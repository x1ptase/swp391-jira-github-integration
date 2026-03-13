import { useState } from 'react';
import { useNavigate } from "react-router-dom";
import './Login.css';

function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setError("");
    try {
      const res = await fetch("/api/auth/login", {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });

      if (!res.ok) throw new Error("Login failed");

      const data = await res.json();

      // Lưu token + thông tin cơ bản
      // role ở đây là system role: STUDENT  LECTURER  ADMIN
      // memberRole (LEADER/MEMBER) KHÔNG có trong login response vì nó là role trong từng group cụ thể, check riêng khi vào group
      // nó là role trong từng group cụ thể, check riêng khi vào group
      const token = data.data.token;
      const role = data.data.role;          // STUDENT LECTURER  ADMIN

      localStorage.setItem("token", token);
      localStorage.setItem("username", username);
      localStorage.setItem("role", role);

      // Navigate theo system role
      const normalized = role?.replace("ROLE_", "").toUpperCase();

      if (normalized === "ADMIN") {
        navigate("/admin/users");
      } else if (normalized === "LECTURER") {
        navigate("/lecturer/groups");
      } else if (normalized === "STUDENT") {
        // Student có thể là LEADER hoặc MEMBER trong các group khác nhau
        // navigate về trang group list của student, không phân biệt ở đây
        navigate("/student/groups");
      } else {
        setError("Unknown role: " + role);
      }

    } catch (err) {
      console.error("Login error:", err);
      setError("Sai tài khoản hoặc mật khẩu");
    }
  };

  return (
    <div className="login-wrapper">
      <div className="login-box">
        <h2>Đăng nhập</h2>
        <form onSubmit={handleLogin}>
          <input
            type="text"
            placeholder="Tên đăng nhập"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
          <input
            type="password"
            placeholder="Mật khẩu"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <button type="submit">Đăng nhập</button>
        </form>
        {error && <p className="error">{error}</p>}
      </div>
    </div>
  );
}

export default Login;