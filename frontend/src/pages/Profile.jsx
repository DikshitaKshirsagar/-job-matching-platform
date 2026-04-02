import React from "react";

const Profile = () => {
  const name = localStorage.getItem("name") || "User";
  const email = localStorage.getItem("email") || "No email available";
  const role = localStorage.getItem("role") || "SEEKER";

  return (
    <div style={{ padding: "30px", color: "white" }}>
      <h2 style={{ marginBottom: "20px" }}>Profile</h2>
      <div
        style={{
          maxWidth: "520px",
          padding: "24px",
          borderRadius: "16px",
          background: "rgba(255,255,255,0.06)",
          backdropFilter: "blur(10px)",
        }}
      >
        <p><strong>Name:</strong> {name}</p>
        <p><strong>Email:</strong> {email}</p>
        <p><strong>Role:</strong> {role}</p>
      </div>
    </div>
  );
};

export default Profile;
