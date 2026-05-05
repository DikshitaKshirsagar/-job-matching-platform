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
  const [applyingJobId, setApplyingJobId] = useState(null);

  useEffect(() => {
    const fetchJobs = async () => {
      try {
        const response = await getJobs();
        setJobs(response.data || []);
      } catch (error) {
        console.error("Error fetching jobs:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchJobs();
  }, []);

  const handleApply = async (jobId) => {
    try {
      setApplyingJobId(jobId);
      await applyJob({ jobId });
      alert("Applied successfully.");
    } catch (error) {
      const message = error.response?.data?.message || "Failed to apply.";
      alert(message);
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
        ) : (
          <div className="jobs-container">
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
