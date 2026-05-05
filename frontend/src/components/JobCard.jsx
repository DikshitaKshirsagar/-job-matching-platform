import React from "react";

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

const JobCard = ({ job, isApplying, onApply }) => (
  <div className="job-card">
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

      <button type="button" onClick={() => onApply(job.id)} disabled={isApplying}>
        {isApplying ? "Applying..." : "Apply"}
      </button>
    </div>
  </div>
);

export default JobCard;
