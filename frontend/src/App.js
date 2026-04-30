import React from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";

import Login from "./pages/LoginPage";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import Jobs from "./pages/Jobs";
import Applications from "./pages/Application";
import Profile from "./pages/Profile";
import RecruiterDashboard from "./pages/RecruiterDashboard";

const getHomeRoute = () => {
  const token = localStorage.getItem("token");
  const role = localStorage.getItem("role");

  if (!token) {
    return "/login";
  }

  return role === "RECRUITER" ? "/recruiter" : "/dashboard";
};

const PrivateRoute = ({ children }) => {
  const token = localStorage.getItem("token");
  return token ? children : <Navigate to="/login" />;
};

const PublicRoute = ({ children }) => {
  const token = localStorage.getItem("token");
  return token ? <Navigate to={getHomeRoute()} /> : children;
};

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Navigate to={getHomeRoute()} replace />} />

        <Route
          path="/login"
          element={
            <PublicRoute>
              <Login />
            </PublicRoute>
          }
        />

        <Route
          path="/register"
          element={
            <PublicRoute>
              <Register />
            </PublicRoute>
          }
        />

        <Route
          path="/dashboard"
          element={
            <PrivateRoute>
              <Dashboard />
            </PrivateRoute>
          }
        />

        <Route
          path="/recruiter"
          element={
            <PrivateRoute>
              <RecruiterDashboard />
            </PrivateRoute>
          }
        />

        <Route
          path="/jobs"
          element={
            <PrivateRoute>
              <Jobs />
            </PrivateRoute>
          }
        />

        <Route
          path="/applications"
          element={
            <PrivateRoute>
              <Applications />
            </PrivateRoute>
          }
        />

        <Route
          path="/profile"
          element={
            <PrivateRoute>
              <Profile />
            </PrivateRoute>
          }
        />

        <Route path="*" element={<Navigate to={getHomeRoute()} replace />} />
      </Routes>
    </Router>
  );
}

export default App;
