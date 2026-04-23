// Auth storage utilities
const AUTH_KEYS = {
  TOKEN: 'token',
  ROLE: 'role',
  NAME: 'name',
  EMAIL: 'email',
  USER_ID: 'userId'
};

export const storageService = {
  // Set auth data
  setAuthData({ token, role, name, email, userId }) {
    localStorage.setItem(AUTH_KEYS.TOKEN, token);
    localStorage.setItem(AUTH_KEYS.ROLE, role);
    localStorage.setItem(AUTH_KEYS.NAME, name);
    localStorage.setItem(AUTH_KEYS.EMAIL, email || '');
    localStorage.setItem(AUTH_KEYS.USER_ID, userId);
  },

  // Get token
  getToken() {
    return localStorage.getItem(AUTH_KEYS.TOKEN);
  },

  // Get full auth data
  getAuthData() {
    return {
      token: this.getToken(),
      role: localStorage.getItem(AUTH_KEYS.ROLE),
      name: localStorage.getItem(AUTH_KEYS.NAME),
      email: localStorage.getItem(AUTH_KEYS.EMAIL),
      userId: localStorage.getItem(AUTH_KEYS.USER_ID),
    };
  },

  // Clear auth data
  clearAuth() {
    localStorage.removeItem(AUTH_KEYS.TOKEN);
    localStorage.removeItem(AUTH_KEYS.ROLE);
    localStorage.removeItem(AUTH_KEYS.NAME);
    localStorage.removeItem(AUTH_KEYS.EMAIL);
    localStorage.removeItem(AUTH_KEYS.USER_ID);
  },

  // Check authentication
  isAuthenticated() {
    return !!this.getToken();
  }
};

export default storageService;

