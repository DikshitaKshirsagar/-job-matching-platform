import React from "react";

const SkeletonCard = () => (
  <div className="job-card skeleton-card" aria-hidden="true">
    <div className="job-left">
      <div className="skeleton skeleton-logo"></div>
      <div>
        <div className="skeleton skeleton-line skeleton-line--title"></div>
        <div className="skeleton skeleton-line"></div>
      </div>
    </div>

    <div className="job-right">
      <div className="skeleton skeleton-pill"></div>
      <div className="skeleton skeleton-button"></div>
    </div>
  </div>
);

export default SkeletonCard;
