import axios from 'axios';

const API = axios.create({ baseURL: 'http://localhost:8080/api/v1' });

API.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor for error handling and token refresh
API.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        // Future: implement refresh token logic here
        // For now, just logout on 401
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(error);
      } catch (refreshError) {
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    
    return Promise.reject(error);
  }
);

export const getJobs = () => API.get('/jobs');
export default API;
