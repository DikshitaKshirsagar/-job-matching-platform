import React, { useEffect, useState } from "react";
import "./Jobs.css";
import Navbar from "../components/Navbar";
import SkeletonCard from "../components/SkeletonCard";
import { applyJob, getJobs } from "../services/api";

const getMatchTone = (score) => {
  if (score == null) {
    return "neutral";
  }

  if (score >= 80) {
    return "high";
  }

  if (score >= 50) {
    return "medium";
  }

  return "low";
};

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
                <div className="job-card" key={job.id}>
                  <div className="job-left">
                    <img
                      src={`https://logo.clearbit.com/${job.company?.toLowerCase()}.com`}
                      alt={job.company || "Company"}
                      onError={(event) => {
                        event.target.src = "https://via.placeholder.com/56?text=Job";
                      }}
                    />

                    <div>
                      <div className="job-card__top">
                        <h3>{job.title}</h3>
                        {job.matchScore != null && (
                          <span className={`match-badge ${getMatchTone(job.matchScore)}`}>
                            {job.matchScore.toFixed(1)}% match
                          </span>
                        )}
                      </div>

                      <p>
                        {job.company} | {job.location || "Location flexible"}
                      </p>
                      <p className="job-description">
                        {job.description?.slice(0, 140)}
                        {job.description?.length > 140 ? "..." : ""}
                      </p>
                    </div>
                  </div>

                  <div className="job-right">
                    <span className="salary">{job.salary || "Salary not disclosed"}</span>

                    <button
                      type="button"
                      onClick={() => handleApply(job.id)}
                      disabled={applyingJobId === job.id}
                    >
                      {applyingJobId === job.id ? "Applying..." : "Apply"}
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default Jobs;
