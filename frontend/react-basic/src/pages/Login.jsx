// src/pages/Login.jsx
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

      if (!res.ok) {
        throw new Error("Login failed");
      }

      const data = await res.json();
      console.log("Login response:", data);

      // Lưu token vào localStorage
      localStorage.setItem("token", data.data.token);
      localStorage.setItem("username", username);
      const role = data.data.role;
      localStorage.setItem("role", role);
      console.log("Role from API:", role);
  
      alert("Login thành công!");
      // Chuyển hướng dựa trên vai trò
       if (role === "ROLE_ADMIN" || role === "ADMIN") {
        navigate("/admin/users"); 
      } 
      else if (role === "ROLE_LECTURER" || role === "LECTURER") {
        navigate("/lecturer/groups"); 
      }
      else if (role === "ROLE_STUDENT" || role === "STUDENT") {
        alert("Student dashboard chưa có");
        // navigate("/student/dashboard");
      }
      else {
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