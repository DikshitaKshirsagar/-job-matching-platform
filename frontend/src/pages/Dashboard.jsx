import React, { useState } from "react";
import "./Dashboard.css";
import { useNavigate } from "react-router-dom";
import { uploadResumeFile } from "../services/api";

const Dashboard = () => {
  const navigate = useNavigate();
  const userName = localStorage.getItem("name") || "User";
  const [fileName, setFileName] = useState("");
  const [resumeFile, setResumeFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [uploadMessage, setUploadMessage] = useState("");

  const handleLogout = () => {
    localStorage.clear();
    navigate("/login");
  };

  const handleFileChange = (e) => {
    if (e.target.files[0]) {
      setUploadMessage("");
      setResumeFile(e.target.files[0]);
      setFileName(e.target.files[0].name);
    }
  };

  const handleResumeUpload = async () => {
    if (!resumeFile) {
      setUploadMessage("Please choose a resume file first.");
      return;
    }

    setUploading(true);
    setUploadMessage("");

    try {
      const response = await uploadResumeFile(resumeFile);
      setUploadMessage(response.data?.message || "Resume uploaded successfully.");
    } catch (error) {
      const message =
        error.response?.data?.message ||
        error.response?.data ||
        "Resume upload failed.";
      setUploadMessage(typeof message === "string" ? message : "Resume upload failed.");
    } finally {
      setUploading(false);
    }
  };

  const navItems = [
    { label: "Dashboard", path: "/dashboard" },
    { label: "Jobs", path: "/jobs" },
    { label: "Applications", path: "/applications" },
    { label: "Profile", path: "/profile" },
  ];

  return (
    <div className="dashboard-container">
      <aside className="sidebar">
        <h1 className="logo">SmartHire AI</h1>

        <nav>
          {navItems.map((item) => (
            <button
              key={item.path}
              type="button"
              className={item.path === "/dashboard" ? "active" : ""}
              onClick={() => navigate(item.path)}
            >
              {item.label}
            </button>
          ))}
        </nav>

        <div>
          <div className="sidebar-footer">
            <img src="https://randomuser.me/api/portraits/men/32.jpg" alt="user" />
            <div>
              <p>{userName}</p>
              <span>Premium</span>
            </div>
          </div>

          <button className="logout-btn" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </aside>

      <main className="main">
        <header className="navbar">
          <h2>Dashboard</h2>

          <div className="nav-actions">
            <input placeholder="Search jobs..." />
            <span className="icon" aria-label="notifications">Bell</span>
            <img src="https://randomuser.me/api/portraits/men/32.jpg" alt="user" />
          </div>
        </header>

        <section className="hero">
          <img
            src="https://images.unsplash.com/photo-1600880292203-757bb62b4baf"
            alt="career"
          />

          <div className="hero-overlay">
            <h2>AI-Powered Career Growth</h2>
            <p>Discover jobs perfectly matched to your profile</p>
            <button onClick={() => navigate("/jobs")}>Explore Jobs</button>
          </div>
        </section>

        <section className="stats">
          <div className="card">
            <h3>1,240</h3>
            <p>Jobs Matched</p>
          </div>
          <div className="card">
            <h3>42</h3>
            <p>Applications</p>
          </div>
          <div className="card">
            <h3>5</h3>
            <p>Interviews</p>
          </div>
          <div className="card">
            <h3>98%</h3>
            <p>Profile Score</p>
          </div>
        </section>

        <div className="grid">
          <div className="left">
            <div className="jobs">
              <div className="section-header">
                <h3>Recommended Jobs</h3>
                <button onClick={() => navigate("/jobs")}>View All -&gt;</button>
              </div>

              <div className="job-card">
                <img src="https://logo.clearbit.com/google.com" alt="Google" />
                <div>
                  <h4>Frontend Developer</h4>
                  <p>Google | Remote</p>
                </div>
                <span>98%</span>
              </div>

              <div className="job-card">
                <img src="https://logo.clearbit.com/amazon.com" alt="Amazon" />
                <div>
                  <h4>Backend Engineer</h4>
                  <p>Amazon | Hybrid</p>
                </div>
                <span>95%</span>
              </div>

              <div className="job-card">
                <img src="https://logo.clearbit.com/microsoft.com" alt="Microsoft" />
                <div>
                  <h4>Full Stack Developer</h4>
                  <p>Microsoft | Remote</p>
                </div>
                <span>92%</span>
              </div>

              <div className="job-card">
                <img src="https://logo.clearbit.com/netflix.com" alt="Netflix" />
                <div>
                  <h4>UI/UX Designer</h4>
                  <p>Netflix | Remote</p>
                </div>
                <span>90%</span>
              </div>

              <div className="job-card">
                <img src="https://logo.clearbit.com/apple.com" alt="Apple" />
                <div>
                  <h4>iOS Developer</h4>
                  <p>Apple | Onsite</p>
                </div>
                <span>89%</span>
              </div>
            </div>

            <div className="graph">
              <h3>Applications Analytics</h3>
              <div className="bars">
                <div style={{ height: "40%" }}></div>
                <div style={{ height: "70%" }}></div>
                <div style={{ height: "50%" }}></div>
                <div style={{ height: "90%" }}></div>
                <div style={{ height: "60%" }}></div>
              </div>
            </div>
          </div>

          <div className="right">
            <div className="resume">
              <h3>Upload Resume</h3>
              <p>AI will analyze your resume</p>

              <label className="custom-upload">
                Choose Resume
                <input type="file" accept=".pdf,.doc,.docx,.txt" onChange={handleFileChange} />
              </label>

              {fileName && <p className="file-name">{fileName}</p>}
              {uploadMessage && <p className="upload-message">{uploadMessage}</p>}

              <button type="button" onClick={handleResumeUpload} disabled={uploading}>
                {uploading ? "Uploading..." : "Upload"}
              </button>
            </div>

            <div className="activity">
              <h3>Recent Activity</h3>
              <p>12 new jobs matched</p>
              <p>Interview request received</p>
              <p>Profile improved</p>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default Dashboard;
