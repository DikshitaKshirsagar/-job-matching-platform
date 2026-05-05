import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import "./Application.css";
import Navbar from "../components/Navbar";
import { getApplications } from "../services/api";

const statusClass = (status) => {
  if (status === "SHORTLISTED") {
    return "shortlisted";
  }

  if (status === "REJECTED") {
    return "rejected";
  }

  return "applied";
};

const Applications = () => {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchApplications = async () => {
      try {
        setError("");
        const response = await getApplications();
        setApplications(response.data || []);
      } catch (error) {
        console.error("Error fetching applications:", error);
        setError(error.response?.data?.message || "Unable to load applications. Please try again.");
      } finally {
        setLoading(false);
      }
    };

    fetchApplications();
  }, []);

  return (
    <div className="applications-shell">
      <Navbar />

      <div className="applications-page">
        <div className="applications-header">
          <div>
            <p className="applications-kicker">Tracking Center</p>
            <h2>My Applications</h2>
          </div>
          <Link className="applications-link" to="/jobs">
            Browse jobs
          </Link>
        </div>

        {loading ? (
          <p className="applications-loading">Loading applications...</p>
        ) : error ? (
          <div className="applications-empty applications-error">
            <h3>Could not load applications</h3>
            <p>{error}</p>
          </div>
        ) : applications.length === 0 ? (
          <div className="applications-empty">
            <h3>No applications yet</h3>
            <p>Browse jobs and apply to start building your pipeline.</p>
          </div>
        ) : (
          <div className="applications-container">
            {applications.map((application) => (
              <div className="application-card" key={application.id}>
                <div className="application-card__head">
                  <div>
                    <h3>{application.jobTitle}</h3>
                    <p>
                      {application.company} | {application.location || "Remote / Flexible"}
                    </p>
                  </div>

                  <div className="application-card__badges">
                    <span className={`status ${statusClass(application.status)}`}>
                      {application.status}
                    </span>
                    <span className="score-badge">
                      {application.matchScore?.toFixed(1) ?? "0.0"}% match
                    </span>
                  </div>
                </div>

                <div className="application-card__meta">
                  <span>
                    Applied on{" "}
                    {application.appliedAt
                      ? new Date(application.appliedAt).toLocaleDateString()
                      : "recently"}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default Applications;
