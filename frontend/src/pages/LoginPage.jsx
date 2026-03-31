import { useState } from "react";
import API from "../services/api";
import { useNavigate } from "react-router-dom";

function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  // const handleLogin = async () => {
  //   try {
  //     const response = await API.post("/auth/login", {
  //       email,
  //       password
  //     });

  //     console.log(response.data);

  //     // Save data
  //     localStorage.setItem("token", response.data.token);
  //     localStorage.setItem("name", response.data.name);
  //     localStorage.setItem("role", response.data.role);
  //     localStorage.setItem("userId", response.data.userId);

  //     // Go to next page
  //     navigate("/dashboard");

  //   } catch (error) {
  //     console.error(error);
  //     alert("Login failed");
  //   }
  // };
  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
        const response = await API.post('/auth/login', {
            email: email,
            password: password
        });
        localStorage.setItem('token', response.data.token);
        localStorage.setItem('role', response.data.role);
        localStorage.setItem('name', response.data.name);
        localStorage.setItem('userId', response.data.userId);
        navigate('/jobs');
    }catch (err) {
        setError('Invalid email or password');
    } finally {
        setLoading(false);
    }
};

  return (
    <div style={{ padding: "20px", maxWidth: "400px", margin: "auto" }}>
      <h2>Login</h2>
      <p>Enter your credentials to access your account.</p>
      {error && <p style={{ color: "red", marginBottom: "10px" }}>{error}</p>}
      <div style={{ marginBottom: "10px" }}>
        <label>Email:</label>
        <input
          type="email"
          placeholder="Enter your email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          style={{ width: "100%", padding: "8px", marginTop: "5px" }}
          disabled={loading}
        />
      </div>
      <div style={{ marginBottom: "10px" }}>
        <label>Password:</label>
        <input
          type="password"
          placeholder="Enter your password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          style={{ width: "100%", padding: "8px", marginTop: "5px" }}
          disabled={loading}
        />
      </div>
      <button
        onClick={handleLogin}
        disabled={loading}
        style={{ 
          padding: "10px 20px", 
          backgroundColor: loading ? "#6c757d" : "#28a745", 
          color: "white", 
          border: "none", 
          cursor: loading ? "not-allowed" : "pointer",
          width: "100%"
        }}
      >
        {loading ? "Logging in..." : "Login"}
      </button>
      <p style={{ marginTop: "10px" }}>
        Don't have an account? <a href="/signup">Sign up here</a>
      </p>
    </div>
  );
}

export default Login;