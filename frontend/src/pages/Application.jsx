import React, { useEffect, useState } from "react";
import "./Application.css";
import { getApplications } from "../services/api";

const Applications = () => {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchApplications();
  }, []);

  const fetchApplications = async () => {
    try {
      const res = await getApplications();
      setApplications(res.data?.data || res.data || []);
    } catch (err) {
      console.error("Error fetching applications:", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="applications-page">
      <h2>Your Applications</h2>

      {loading ? (
        <p>Loading...</p>
      ) : applications.length === 0 ? (
        <p>No applications yet</p>
      ) : (
        <div className="applications-container">
          {applications.map((app) => (
            <div className="application-card" key={app.id}>
              <div className="app-left">
                <img
                  src={`https://logo.clearbit.com/${app.company?.toLowerCase()}.com`}
                  alt="logo"
                  onError={(e) => {
                    e.target.src = "https://via.placeholder.com/50";
                  }}
                />

                <div>
                  <h3>{app.jobTitle}</h3>
                  <p>{app.company}</p>
                </div>
              </div>

              <div className="app-right">
                <span className={`status ${app.status?.toLowerCase()}`}>
                  {app.status || "Pending"}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default Applications;
