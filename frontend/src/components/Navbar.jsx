import React from "react";
import { NavLink, useNavigate } from "react-router-dom";
import "./Navbar.css";

const Navbar = () => {
  const navigate = useNavigate();
  const role = localStorage.getItem("role") || "SEEKER";
  const name = localStorage.getItem("name") || "User";

  const links = role === "RECRUITER"
    ? [{ to: "/recruiter", label: "Dashboard" }]
    : [
        { to: "/dashboard", label: "Dashboard" },
        { to: "/jobs", label: "Jobs" },
        { to: "/applications", label: "My Applications" },
        { to: "/profile", label: "Profile" },
      ];

  const handleLogout = () => {
    localStorage.clear();
    navigate("/login");
  };

  return (
    <header className="app-navbar">
      <div className="app-navbar__brand">
        <div className="app-navbar__logo">JM</div>
        <div>
          <h1>JobMatch AI</h1>
          <p>Career matching platform</p>
        </div>
      </div>

      <nav className="app-navbar__links">
        {links.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            className={({ isActive }) =>
              isActive ? "app-navbar__link active" : "app-navbar__link"
            }
          >
            {link.label}
          </NavLink>
        ))}
      </nav>

      <div className="app-navbar__actions">
        <div className="app-navbar__identity">
          <span>{name}</span>
          <strong>{role}</strong>
        </div>
        <button type="button" onClick={handleLogout}>
          Logout
        </button>
      </div>
    </header>
  );
};

export default Navbar;
