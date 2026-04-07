import axios from "axios";

// ✅ Use relative URL so proxy in package.json handles it (no CORS issues)
const API = axios.create({
  baseURL: "/api/v1",
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

// ==============================
// 🔐 REQUEST INTERCEPTOR
// Attach JWT token automatically
// ==============================
API.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// ==============================
// 🚨 RESPONSE INTERCEPTOR
// Handle global errors
// ==============================
API.interceptors.response.use(
  (response) => response,
  (error) => {
    if (!error.response) {
      console.error("❌ BACKEND NOT REACHABLE:", error.message);
      alert("Backend server not running. Start Spring Boot.");
      return Promise.reject(error);
    }

    const status = error.response.status;

    if (status === 401) {
      console.warn("⚠️ Session expired. Logging out...");
      localStorage.clear();
      window.location.href = "/login";
    }

    if (status === 403) {
      console.warn("❌ Access denied.");
    }

    if (status === 400) {
      console.warn("⚠️ Bad request:", error.response.data);
    }

    if (status === 500) {
      console.error("🔥 Server error:", error.response.data);
    }

    return Promise.reject(error);
  }
);

export const getJobs = () => API.get('/jobs');
export const applyJob = (data) => API.post('/applications', data);
export const getApplications = () => API.get('/applications');
export const uploadResume = (resumeText) => API.post('/auth/resume', { resumeText });
export default API;
// ==============================
// 📌 AUTH APIs
// ==============================

export const loginUser = (data) =>
  API.post("/auth/login", data);

export const registerUser = (data) =>
  API.post("/auth/register", data);

// ==============================
// 📌 JOB APIs
// ==============================

export const getJobs = () =>
  API.get("/jobs");

export const applyJob = (data) =>
  API.post("/applications", data);

export const getApplications = () =>
  API.get("/applications");

// ==============================
// 📌 PROFILE / RESUME
// ==============================

export const uploadResume = (resumeText) =>
  API.post("/auth/resume", { resumeText });

// ==============================
// 📌 EXPORT INSTANCE
// ==============================
export default API;
