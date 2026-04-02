import API from './api';

// Auth service with all backend endpoints
export const authService = {
  // Login existing user
  async login(credentials) {
    const response = await API.post('/auth/login', credentials);
    const { token, role, name, userId } = response.data;
    
    // Store auth data consistently
    localStorage.setItem('token', token);
    localStorage.setItem('role', role);
    localStorage.setItem('name', name);
    localStorage.setItem('userId', userId);
    
    return response.data;
  },

  // Register new user (default role: SEEKER)
  async register(userData) {
    const response = await API.post('/auth/register', {
      ...userData,
      role: 'SEEKER' // default
    });
    return response.data;
  },

  // Verify email with token
  async verifyEmail(token) {
    const response = await API.post('/auth/verify-email', { token });
    return response.data;
  },

  // Send forgot password email
  async forgotPassword(email) {
    const response = await API.post('/auth/forgot-password', { email });
    return response.data;
  },

  // Reset password with token
  async resetPassword(token, password) {
    const response = await API.post('/auth/reset-password', { 
      token, 
      password 
    });
    return response.data;
  },

  // Logout
  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('name');
    localStorage.removeItem('userId');
    // Redirect to login
    window.location.href = '/login';
  },

  // Get current user info
  getCurrentUser() {
    return {
      token: localStorage.getItem('token'),
      role: localStorage.getItem('role'),
      name: localStorage.getItem('name'),
      userId: localStorage.getItem('userId'),
    };
  },

  // Check if authenticated
  isAuthenticated() {
    return !!localStorage.getItem('token');
  }
};

export default authService;

