import { useAuth } from "../context/AuthContext";

function Dashboard() {
  const { user, logout } = useAuth();

  const handleLogout = () => {
    logout();
  };

  if (!user) {
    return <div>Loading...</div>;
  }

  return (
    <div style={{ padding: "20px" }}>
      <h2>Welcome to Job Matching Platform, {user.name}!</h2>
      <p>Role: {user.role}</p>
      <p>User ID: {user.userId}</p>
      <p>You are logged in!</p>
      <button onClick={handleLogout} style={{ padding: "10px 20px", backgroundColor: "#dc3545", color: "white", border: "none", cursor: "pointer" }}>
        Logout
      </button>
    </div>
  );
}

export default Dashboard;
