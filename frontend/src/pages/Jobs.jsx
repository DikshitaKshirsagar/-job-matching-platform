import React, { useEffect, useState } from "react";
import "./Jobs.css";
import JobCard from "../components/JobCard";
import Navbar from "../components/Navbar";
import SkeletonCard from "../components/SkeletonCard";
import { applyJob, getJobs } from "../services/api";

const Jobs = () => {
  const [jobs, setJobs] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [statusMessage, setStatusMessage] = useState("");
  const [applyingJobId, setApplyingJobId] = useState(null);

  useEffect(() => {
    const fetchJobs = async () => {
      try {
        setError("");
        const response = await getJobs();
        setJobs(response.data || []);
      } catch (error) {
        console.error("Error fetching jobs:", error);
        setError(error.response?.data?.message || "Unable to load jobs. Please try again.");
      } finally {
        setLoading(false);
      }
    };

    fetchJobs();
  }, []);

  const handleApply = async (jobId) => {
    try {
      setApplyingJobId(jobId);
      setStatusMessage("");
      setError("");
      await applyJob({ jobId });
      setStatusMessage("Applied successfully.");
    } catch (error) {
      const message = error.response?.data?.message || "Failed to apply.";
      setError(message);
    } finally {
      setApplyingJobId(null);
    }
  };

  const filteredJobs = jobs.filter((job) => {
    const haystack = `${job.title || ""} ${job.company || ""} ${job.location || ""}`.toLowerCase();
    return haystack.includes(search.toLowerCase());
  });

  return (
    <div className="jobs-shell">
      <Navbar />

      <div className="jobs-page">
        <div className="jobs-header">
          <div>
            <p className="jobs-kicker">Seeker Workspace</p>
            <h2>Find Your Dream Job</h2>
          </div>

          <input
            type="text"
            placeholder="Search jobs, companies, locations..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>

        {loading ? (
          <div className="jobs-container">
            {Array.from({ length: 6 }).map((_, index) => (
              <SkeletonCard key={index} />
            ))}
          </div>
        ) : error ? (
          <div className="jobs-empty jobs-error">{error}</div>
        ) : (
          <div className="jobs-container">
            {statusMessage && <div className="jobs-empty jobs-success">{statusMessage}</div>}
            {filteredJobs.length === 0 ? (
              <div className="jobs-empty">No jobs found for your search.</div>
            ) : (
              filteredJobs.map((job) => (
                <JobCard
                  key={job.id}
                  job={job}
                  isApplying={applyingJobId === job.id}
                  onApply={handleApply}
                />
              ))
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default Jobs;
