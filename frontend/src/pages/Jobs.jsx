import React, { useEffect, useState } from "react";
import "./Jobs.css";
import { getJobs, applyJob } from "../services/api";

const Jobs = () => {
  const [jobs, setJobs] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);

  // FETCH JOBS
  useEffect(() => {
    fetchJobs();
  }, []);

  const fetchJobs = async () => {
    try {
      const res = await getJobs();
      setJobs(res.data);
    } catch (err) {
      console.error("Error fetching jobs:", err);
    } finally {
      setLoading(false);
    }
  };

  // APPLY JOB
  const handleApply = async (jobId) => {
    try {
      const userId = localStorage.getItem("userId");

      await applyJob({ userId, jobId });

      alert("✅ Applied Successfully!");
    } catch (err) {
      console.error("Apply error:", err);
      alert("❌ Failed to apply");
    }
  };

  // SEARCH FILTER
  const filteredJobs = jobs.filter((job) =>
    job.title?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="jobs-page">

      {/* HEADER */}
      <div className="jobs-header">
        <h2>Find Your Dream Job 🚀</h2>

        <input
          type="text"
          placeholder="Search jobs..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {/* LOADING */}
      {loading ? (
        <p className="loading">Loading jobs...</p>
      ) : (
        <div className="jobs-container">

          {/* NO JOBS */}
          {filteredJobs.length === 0 ? (
            <p className="no-jobs">No jobs found</p>
          ) : (
            filteredJobs.map((job) => (
              <div className="job-card" key={job.id}>

                {/* LEFT */}
                <div className="job-left">
                  <img
                    src={`https://logo.clearbit.com/${job.company?.toLowerCase()}.com`}
                    alt="logo"
                    onError={(e) =>
                      (e.target.src =
                        "https://via.placeholder.com/50?text=Logo")
                    }
                  />

                  <div>
                    <h3>{job.title}</h3>
                    <p>
                      {job.company} • {job.location}
                    </p>
                  </div>
                </div>

                {/* RIGHT */}
                <div className="job-right">
                  <span className="salary">
                    {job.salary ? job.salary : "Not Disclosed"}
                  </span>

                  <button onClick={() => handleApply(job.id)}>
                    Apply
                  </button>
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