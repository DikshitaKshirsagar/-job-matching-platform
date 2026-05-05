import React, { useEffect, useState } from "react";
import "./Profile.css";
import Navbar from "../components/Navbar";
import { getUserProfile, uploadResumeFile } from "../services/api";

const Profile = () => {
  const [profile, setProfile] = useState(null);
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadProfile = async () => {
    try {
      setError("");
      const response = await getUserProfile();
      setProfile(response.data);
    } catch (error) {
      console.error("Failed to load profile:", error);
      setError(error.response?.data?.message || "Unable to load your profile. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProfile();
  }, []);

  const handleUpload = async () => {
    if (!selectedFile) {
      setMessage("Choose a PDF resume first.");
      return;
    }

    setUploading(true);
    setUploadProgress(0);
    setMessage("");

    try {
      const response = await uploadResumeFile(selectedFile, {
        onUploadProgress: (event) => {
          if (event.total) {
            setUploadProgress(Math.round((event.loaded * 100) / event.total));
          }
        },
      });

      setMessage(response.data?.message || "Resume uploaded! AI matching now active.");
      setSelectedFile(null);
      await loadProfile();
    } catch (error) {
      setMessage(error.response?.data?.message || "Resume upload failed.");
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="profile-shell">
      <Navbar />

      <div className="profile-page">
        {loading ? (
          <p className="profile-loading">Loading profile...</p>
        ) : error ? (
          <section className="profile-card profile-error">
            <h2>Could not load profile</h2>
            <p>{error}</p>
          </section>
        ) : (
          <>
            <section className="profile-card">
              <div>
                <p className="profile-kicker">Profile Snapshot</p>
                <h2>{profile?.name}</h2>
              </div>

              <div className="profile-grid">
                <div>
                  <span>Email</span>
                  <strong>{profile?.email}</strong>
                </div>
                <div>
                  <span>Role</span>
                  <strong>{profile?.role}</strong>
                </div>
                <div>
                  <span>Member since</span>
                  <strong>
                    {profile?.createdAt
                      ? new Date(profile.createdAt).toLocaleDateString()
                      : "Recently"}
                  </strong>
                </div>
                <div>
                  <span>Resume</span>
                  <strong>{profile?.resumeFileName || "Not uploaded yet"}</strong>
                </div>
              </div>
            </section>

            <section className="resume-card">
              <div className="resume-card__copy">
                <p className="profile-kicker">Resume Upload</p>
                <h3>Keep your AI matching fresh</h3>
                <p>Upload a PDF resume so the job list and application ranking stay accurate.</p>
              </div>

              <div className="resume-card__actions">
                <label className="resume-picker">
                  <input
                    type="file"
                    accept="application/pdf"
                    onChange={(event) => setSelectedFile(event.target.files?.[0] || null)}
                  />
                  <span>{selectedFile?.name || "Choose PDF resume"}</span>
                </label>

                {uploading && (
                  <div className="progress-track">
                    <div className="progress-bar" style={{ width: `${uploadProgress}%` }}></div>
                  </div>
                )}

                {message && <p className="profile-message">{message}</p>}

                <button type="button" onClick={handleUpload} disabled={uploading}>
                  {uploading ? "Uploading..." : "Upload Resume"}
                </button>
              </div>
            </section>
          </>
        )}
      </div>
    </div>
  );
};

export default Profile;
