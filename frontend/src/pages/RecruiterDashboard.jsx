import React, { useEffect, useState } from "react";
import Navbar from "../components/Navbar";
import "./RecruiterDashboard.css";
import { createJob, getJobApplicants, getMyJobs } from "../services/api";

const emptyJob = {
  title: "",
  company: "",
  description: "",
  location: "",
  salary: "",
};

const RecruiterDashboard = () => {
  const [form, setForm] = useState(emptyJob);
  const [jobs, setJobs] = useState([]);
  const [selectedJob, setSelectedJob] = useState(null);
  const [applicants, setApplicants] = useState([]);
  const [message, setMessage] = useState("");

  const loadJobs = async () => {
    try {
      const response = await getMyJobs();
      setJobs(response.data || []);
    } catch (error) {
      console.error("Failed to load recruiter jobs:", error);
    }
  };

  useEffect(() => {
    loadJobs();
  }, []);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setMessage("");

    try {
      await createJob(form);
      setMessage("Job posted successfully.");
      setForm(emptyJob);
      await loadJobs();
    } catch (error) {
      setMessage(error.response?.data?.message || "Failed to post job.");
    }
  };

  const handleSelectJob = async (job) => {
    setSelectedJob(job);

    try {
      const response = await getJobApplicants(job.id);
      setApplicants(response.data || []);
    } catch (error) {
      console.error("Failed to load applicants:", error);
      setApplicants([]);
    }
  };

  return (
    <div className="recruiter-shell">
      <Navbar />

      <div className="recruiter-page">
        <section className="recruiter-card">
          <p className="recruiter-kicker">Recruiter Command Center</p>
          <h2>Post a new job</h2>

          <form className="recruiter-form" onSubmit={handleSubmit}>
            <input
              placeholder="Job title"
              value={form.title}
              onChange={(event) => setForm({ ...form, title: event.target.value })}
            />
            <input
              placeholder="Company"
              value={form.company}
              onChange={(event) => setForm({ ...form, company: event.target.value })}
            />
            <input
              placeholder="Location"
              value={form.location}
              onChange={(event) => setForm({ ...form, location: event.target.value })}
            />
            <input
              placeholder="Salary"
              value={form.salary}
              onChange={(event) => setForm({ ...form, salary: event.target.value })}
            />
            <textarea
              placeholder="Write the job description"
              value={form.description}
              onChange={(event) => setForm({ ...form, description: event.target.value })}
            />
            <button type="submit">Post job</button>
          </form>

          {message && <p className="recruiter-message">{message}</p>}
        </section>

        <section className="recruiter-grid">
          <div className="recruiter-card">
            <h3>My posted jobs</h3>
            <div className="recruiter-job-list">
              {jobs.length === 0 ? (
                <p>No jobs posted yet.</p>
              ) : (
                jobs.map((job) => (
                  <button
                    key={job.id}
                    type="button"
                    className={selectedJob?.id === job.id ? "recruiter-job active" : "recruiter-job"}
                    onClick={() => handleSelectJob(job)}
                  >
                    <strong>{job.title}</strong>
                    <span>{job.location || "Flexible"}</span>
                  </button>
                ))
              )}
            </div>
          </div>

          <div className="recruiter-card">
            <h3>Applicants {selectedJob ? `for ${selectedJob.title}` : ""}</h3>

            {!selectedJob ? (
              <p>Select a job to see ranked applicants.</p>
            ) : applicants.length === 0 ? (
              <p>No applicants yet for this job.</p>
            ) : (
              <div className="recruiter-table">
                <div className="recruiter-table__head">
                  <span>Name</span>
                  <span>Email</span>
                  <span>Score</span>
                  <span>Status</span>
                </div>

                {applicants.map((applicant, index) => (
                  <div
                    key={applicant.id}
                    className={index === 0 ? "recruiter-table__row top-applicant" : "recruiter-table__row"}
                  >
                    <span>{applicant.applicantName || "Applicant"}</span>
                    <span>{applicant.applicantEmail || "N/A"}</span>
                    <span>{applicant.matchScore?.toFixed(1) ?? "0.0"}%</span>
                    <span>{applicant.status}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </section>
      </div>
    </div>
  );
};

export default RecruiterDashboard;
