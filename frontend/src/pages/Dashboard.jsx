import { useAuth } from "../context/AuthContext";
import { useState, useEffect } from "react";
import { getJobs } from "../services/api.js";

function Dashboard() {
  const { user, logout } = useAuth();
  const [jobs, setJobs] = useState([]);

  const handleLogout = () => {
    logout();
  };

  useEffect(() => {
    getJobs().then(response => {
      setJobs(response.data);
    }).catch(error => {
      console.error("Error fetching jobs:", error);
    });
  }, []);

  if (!user) {
    return <div>Loading...</div>;
  }

  return (
    <div style={{ padding: "20px" }}>
      <h2>Welcome to Job Matching Platform, {user.name}!</h2>
      <p>Role: {user.role}</p>
      <p>User ID: {user.userId}</p>
      <p>You are logged in!</p>
      <div style={{marginTop: '20px'}}>
        <h3>Available Jobs:</h3>
        {jobs.length === 0 ? (
          <p>No jobs available.</p>
        ) : (
          <ul>
            {jobs.map(job => (
              <li key={job.id} style={{marginBottom: '10px', padding: '10px', border: '1px solid #ddd', borderRadius: '5px'}}>
                <strong>{job.title}</strong> at <em>{job.company}</em> - {job.location} | {job.salary}
              </li>
            ))}
          </ul>
        )}
      </div>
      <button onClick={handleLogout} style={{ padding: "10px 20px", backgroundColor: "#dc3545", color: "white", border: "none", cursor: "pointer" }}>
        Logout
      </button>
    </div>
  );
}

export default Dashboard;
