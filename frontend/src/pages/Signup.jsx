import { useState } from "react";
import API from "../services/api";
import { useNavigate } from "react-router-dom";

function Signup() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");        // ← added
  const [loading, setLoading] = useState(false); // ← added
  const navigate = useNavigate();

  const handleSignup = async (e) => {
    e.preventDefault();          // ← IMPORTANT: stops page refresh
    setError("");
    setLoading(true);
    try {
      const response = await API.post("/auth/register", {
        name,
        email,
        password,
        role: "SEEKER"
      });

      console.log(response.data);
      navigate("/login");

    } catch (err) {
      console.error(err);
      // This shows the REAL error from backend, not just "Signup failed"
      const msg = err.response?.data?.message 
               || err.response?.data?.error
               || "Signup failed. Please try again.";
      setError(msg);             // ← shows real error on screen
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: "20px", maxWidth: "400px", margin: "auto" }}>
      <h2>Sign Up</h2>
      <p>Create an account to start your job search.</p>

      {/* Shows real backend error message */}
      {error && <p style={{ color: "red", marginBottom: "10px" }}>{error}</p>}

      <div style={{ marginBottom: "10px" }}>
        <label>Name:</label>
        <input
          type="text"
          placeholder="Enter your name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          style={{ width: "100%", padding: "8px", marginTop: "5px" }}
          disabled={loading}
        />
      </div>
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
        onClick={handleSignup}
        disabled={loading}
        style={{ 
          padding: "10px 20px", 
          backgroundColor: loading ? "#6c757d" : "#007bff", 
          color: "white", 
          border: "none", 
          cursor: loading ? "not-allowed" : "pointer",
          width: "100%"        // ← made full width like your login button
        }}
      >
        {loading ? "Signing up..." : "Sign Up"}
      </button>
      <p style={{ marginTop: "10px" }}>
        Already have an account? <a href="/login">Login here</a>
      </p>
    </div>
  );
}

export default Signup;