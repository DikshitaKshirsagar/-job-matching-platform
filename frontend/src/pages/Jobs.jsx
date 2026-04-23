import React, { useEffect, useState } from "react";
import "./Jobs.css";
import { getJobs, applyJob } from "../services/api";

const Jobs = () => {
  const [jobs, setJobs] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchJobs();
  }, []);

  const fetchJobs = async () => {
    try {
      const res = await getJobs();
      setJobs(res.data?.data || res.data || []);
    } catch (err) {
      console.error("Error fetching jobs:", err);
    } finally {
      setLoading(false);
    }
  };

  const handleApply = async (jobId) => {
    try {
      const userId = localStorage.getItem("userId");

      if (!userId) {
        alert("Please login again");
        return;
      }

      await applyJob({ userId, jobId });
      alert("Applied successfully.");
    } catch (err) {
      console.error("Apply error:", err);
      alert("Failed to apply.");
    }
  };

  const filteredJobs = jobs.filter((job) =>
    job.title?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="jobs-page">
      <div className="jobs-header">
        <h2>Find Your Dream Job</h2>

        <input
          type="text"
          placeholder="Search jobs..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {loading ? (
        <p className="loading">Loading jobs...</p>
      ) : (
        <div className="jobs-container">
          {filteredJobs.length === 0 ? (
            <p className="no-jobs">No jobs found</p>
          ) : (
            filteredJobs.map((job) => (
              <div className="job-card" key={job.id}>
                <div className="job-left">
                  <img
                    src={`https://logo.clearbit.com/${job.company?.toLowerCase()}.com`}
                    alt="logo"
                    onError={(e) => {
                      e.target.src = "https://via.placeholder.com/50?text=Logo";
                    }}
                  />

                  <div>
                    <h3>{job.title}</h3>
                    <p>
                      {job.company} | {job.location}
                    </p>
                  </div>
                </div>

                <div className="job-right">
                  <span className="salary">
                    {job.salary ? job.salary : "Not Disclosed"}
                  </span>

                  <button onClick={() => handleApply(job.id)}>Apply</button>
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
};

export default Jobs;
