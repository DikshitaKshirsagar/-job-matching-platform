import "./LoginPage.css";
import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { Link, useNavigate } from "react-router-dom";

function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();

  const handleLogin = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const response = await login({ email, password });
      navigate(response.role === "RECRUITER" ? "/recruiter" : "/dashboard");
    } catch (err) {
      const message =
        err.response?.data?.message ||
        err.response?.data ||
        "Invalid email or password";
      setError(typeof message === "string" ? message : "Invalid email or password");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      
      {/* Floating orbs */}
      <div className="orb"></div>
      <div className="orb"></div>
      <div className="orb"></div>

      <div className="login-card">
        <h1 className="login-title">Welcome Back</h1>
        <p className="login-subtitle">Login to continue</p>

        {error && <span className="error-text">{error}</span>}

        <form onSubmit={handleLogin}>
          
          <div className="form-group">
            <label>Email</label>
            <input
              type="email"
              placeholder="Enter your email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label>Password</label>
            <input
              type="password"
              placeholder="Enter your password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={loading}
            />
          </div>

          <button
            type="submit"
            className={`btn-login ${loading ? "loading" : ""}`}
            disabled={loading}
          >
            {loading ? "Logging in..." : "Login"}
          </button>
        </form>

        <p className="login-footer">
          Don't have an account?{" "}
          <Link to="/register" className="login-link">
            Register
          </Link>
        </p>
      </div>
    </div>
  );
}

export default Login;
