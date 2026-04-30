import axios from "axios";

const API = axios.create({
  baseURL: "http://localhost:8081/api/v1",
  timeout: 15000,
  headers: {
    "Content-Type": "application/json",
  },
});

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

API.interceptors.response.use(
  (response) => response,
  (error) => {
    if (!error.response) {
      console.error("Backend not reachable:", error.message);
      alert("Backend server not running. Start Spring Boot on port 8081.");
      return Promise.reject(error);
    }

    if (error.response.status === 401) {
      localStorage.clear();
      window.location.href = "/login";
    }

    return Promise.reject(error);
  }
);

export const loginUser = (data) => API.post("/auth/login", data);
export const registerUser = (data) => API.post("/auth/register", data);

export const getDashboard = () => API.get("/users/dashboard");
export const getUserProfile = () => API.get("/users/profile");
export const uploadResumeFile = (file, config = {}) => {
  const formData = new FormData();
  formData.append("file", file);

  return API.post("/users/upload-resume", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
    ...config,
  });
};

export const getJobs = () => API.get("/jobs");
export const createJob = (data) => API.post("/jobs", data);
export const getMyJobs = () => API.get("/jobs/my-jobs");
export const getJobApplicants = (jobId) => API.get(`/jobs/${jobId}/applicants`);

export const applyJob = (data) => API.post("/applications/apply", data);
export const getApplications = () => API.get("/applications/my");

export default API;
