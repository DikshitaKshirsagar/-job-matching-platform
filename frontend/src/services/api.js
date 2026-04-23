import axios from "axios";

const API = axios.create({
  baseURL: "/api/v1",
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

// Attach JWT token automatically.
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

// Handle global API errors in one place.
API.interceptors.response.use(
  (response) => response,
  (error) => {
    if (!error.response) {
      console.error("Backend not reachable:", error.message);
      alert("Backend server not running. Start Spring Boot.");
      return Promise.reject(error);
    }

    const status = error.response.status;

    if (status === 401) {
      console.warn("Session expired. Logging out...");
      localStorage.clear();
      window.location.href = "/login";
    }

    if (status === 403) {
      console.warn("Access denied.");
    }

    if (status === 400) {
      console.warn("Bad request:", error.response.data);
    }

    if (status === 500) {
      console.error("Server error:", error.response.data);
    }

    return Promise.reject(error);
  }
);

export const loginUser = (data) => API.post("/auth/login", data);
export const registerUser = (data) => API.post("/auth/register", data);
export const getJobs = () => API.get("/jobs");
export const applyJob = (data) => API.post("/applications/apply", data);
export const getApplications = () => API.get("/applications/my");
export const uploadResume = (resumeText) =>
  API.post("/auth/resume", { resumeText });

export default API;
