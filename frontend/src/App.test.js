import { render, screen } from '@testing-library/react';
import App from './App';
import { AuthProvider } from './context/AuthContext';

test('renders login page for signed-out users', () => {
  window.localStorage.clear();
  render(
    <AuthProvider>
      <App />
    </AuthProvider>
  );
  expect(screen.getByText(/welcome back/i)).toBeInTheDocument();
});
