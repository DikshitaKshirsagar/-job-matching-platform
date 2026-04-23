import API from './api';

export const authService = {

  async login(credentials) {
    const response = await API.post('/auth/login', {
      email:    credentials.email,
      password: credentials.password,
    });
    const { token, role, name, userId, email } = response.data;
    localStorage.setItem('token', token);
    localStorage.setItem('role', role);
    localStorage.setItem('name', name);
    localStorage.setItem('userId', userId);
    localStorage.setItem('email', email || credentials.email);
    return response.data;
  },

  async register(userData) {
    const response = await API.post('/auth/register', {
      name:     userData.name,
      email:    userData.email,
      password: userData.password,
      role:     userData.role,
    });
    return response.data;
  },

  logout() {
    localStorage.clear();
    window.location.href = '/login';
  },

  getCurrentUser() {
    return {
      token:  localStorage.getItem('token'),
      role:   localStorage.getItem('role'),
      name:   localStorage.getItem('name'),
      email:  localStorage.getItem('email'),
      userId: localStorage.getItem('userId'),
    };
  },

  isAuthenticated() {
    return !!localStorage.getItem('token');
  }
};

export default authService;
